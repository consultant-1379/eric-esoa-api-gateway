/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * A reactive authorization manager which delegates to the Authorization server (Keycloak) to
 * perform evaluation of all permissions and authorization policies associated with the resources
 * being requested.
 *
 * <p>Resources which are not managed by the Authorization server will be ignored and as such access
 * will be granted to those resources.
 */
@Component
public class KeycloakAuthorizationManager
    implements ReactiveAuthorizationManager<AuthorizationContext> {

  @Autowired private PolicyEnforcer policyEnforcer;
  private final Logger logger = LoggerFactory.getLogger(KeycloakAuthorizationManager.class);

  @Autowired private DefaultReactiveOAuth2UserService userService;
  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepo;

  @Autowired private ProtectedResourcesTtlCache protectedResourcesTtlCache;
  private static final String TOKEN_PREFIX = "Bearer ";
  private static final String COOKIE = "cookie";
  private static final String JSESSIONID = "JSESSIONID";

  @Override
  public Mono<AuthorizationDecision> check(
      final Mono<Authentication> authentication, final AuthorizationContext object) {
    logger.debug("KAM:REQUEST_HEADERS -->" + object.getExchange().getRequest().getHeaders());
    String bearerToken;
    boolean isJsessionIdExists = isJsessionIdExists(object);
    if (Objects.nonNull(
            object.getExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
        && !isJsessionIdExists) {
      bearerToken =
          object.getExchange().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
      if (!bearerToken.isEmpty() && bearerToken.startsWith(TOKEN_PREFIX)) {
        bearerToken = tokenExtract(authentication, object, bearerToken);
        String finalBearerToken = bearerToken;
        return findClientRegistration()
            .zipWhen(
                clientRegistration ->
                    verifyAccessToken(finalBearerToken, clientRegistration, object))
            .doOnError(
                throwable -> {
                  saveErrorInSecurityContext(authentication, object, throwable.getMessage());
                })
            .flatMap(
                access ->
                    createAuthentication(access.getT2(), access.getT1())
                        .doOnNext(oAuth2AuthToken -> saveSecurityContext(oAuth2AuthToken, object))
                        .filter(Authentication::isAuthenticated)
                        .flatMap(
                            e -> {
                              logger.info("is Authenticated = " + e.isAuthenticated());
                              if (!e.isAuthenticated()) {
                                return Mono.justOrEmpty(new AuthorizationDecision(false));
                              }
                              return Mono.justOrEmpty(new AuthorizationDecision(true));
                            })
                        .thenReturn(new AuthorizationDecision(true)))
            .thenReturn(new AuthorizationDecision(true));
      } else {
        String errorMessage =
            "Authorization is failed due to Bearer schema/token is missing in access token provided";
        logger.error("KAM:" + errorMessage);
        saveErrorInSecurityContext(authentication, object, errorMessage);
        return Mono.justOrEmpty(new AuthorizationDecision(false));
      }
    } else {
      logger.debug("KAM:Cookie based flow initiated ");
      return authentication
          .filter(Authentication::isAuthenticated)
          .flatMap(auth -> policyEnforcer.enforce(auth, object.getExchange()))
          .defaultIfEmpty(new AuthorizationDecision(false));
    }
  }

  private String tokenExtract(
      Mono<Authentication> authentication, AuthorizationContext object, String bearerToken) {
    bearerToken = bearerToken.replace(TOKEN_PREFIX, "");

    try {
      bearerToken = extractDetailsFromToken(bearerToken);
    } catch (Exception exception) {
      String errorMessage =
          "Authorization failed due to invalid/corrupted access token value provided";
      logger.error("KAM:" + errorMessage + "::" + exception.getMessage());
      saveErrorInSecurityContext(
          authentication, object, errorMessage + "::" + exception.getMessage());
    }
    return bearerToken;
  }

  protected Mono<ClientRegistration> findClientRegistration() {
    return clientRegistrationRepo.findByRegistrationId("keycloak");
  }

  protected Mono<OAuth2AccessTokenResponse> verifyAccessToken(
      final String bearerToken,
      final ClientRegistration clientRegistration,
      final AuthorizationContext object) {
    return policyEnforcer.enforce(object.getExchange(), bearerToken, clientRegistration);
  }

  protected Mono<OAuth2AuthenticationToken> createAuthentication(
      final OAuth2AccessTokenResponse response, final ClientRegistration clientRegistration) {
    final OAuth2UserRequest oAuth2UserRequest =
        new OAuth2UserRequest(clientRegistration, response.getAccessToken());
    return userService
        .loadUser(oAuth2UserRequest)
        .map(
            oAuth2User ->
                new OAuth2AuthenticationToken(
                    oAuth2User,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    clientRegistration.getRegistrationId()));
  }

  protected void saveSecurityContext(
      final Authentication authentication, final AuthorizationContext exchange) {
    final SecurityContext securityContext;
    if (authentication != null) {
      securityContext = new SecurityContextImpl(authentication);
      securityContext.setAuthentication(authentication);
    } else {
      securityContext = new SecurityContextImpl();
    }
    Mono<WebSession> session = exchange.getExchange().getSession();
    session.flatMap(
        e -> {
          e.getAttributes().put("MACHINE_CLIENT", true);
          e.getAttributes().put("lastRequestTime", Instant.now());
          e.getAttributes().put("SPRING_SECURITY_CONTEXT", securityContext);
          return session;
        });
  }

  private void saveErrorInSecurityContext(
      Mono<Authentication> authentication, AuthorizationContext exchange, String errorMessage) {
    saveSecurityContext(authentication.block(), exchange);
    OAuth2Error error =
        new OAuth2Error(
            HttpStatus.UNAUTHORIZED.toString(),
            errorMessage,
            exchange.getExchange().getRequest().getURI().getPath());
    throw new OAuth2AuthorizationException(error);
  }

  protected String extractDetailsFromToken(String bearerToken) {
    final ObjectMapper objectMapper = new ObjectMapper();
    Base64.Decoder decoder = Base64.getUrlDecoder();
    try {
      String[] parts = bearerToken.split("\\.");
      String body = new String(decoder.decode(parts[1]));
      HashMap<String, Object> map = objectMapper.readValue(body, HashMap.class);
      if (Objects.nonNull(map.get("clientId").toString())
          && Objects.nonNull(map.get("tenant_name").toString())
          && Objects.nonNull(map.get("azp").toString())
          && Objects.nonNull(map.get("iss").toString())
          && Objects.nonNull(map.get("preferred_username").toString())) {
        logger.info(
            "KAM:Received access token values are clientId="
                + map.get("clientId").toString()
                + ", tenant_name="
                + map.get("tenant_name").toString()
                + ", audience="
                + map.get("azp").toString()
                + ", Issuer="
                + map.get("iss").toString()
                + ", preferred_username="
                + map.get("preferred_username").toString());
      }
    } catch (JsonProcessingException | ArrayIndexOutOfBoundsException e) {
      throw new RuntimeException(e.getMessage());
    }
    return bearerToken;
  }

  private boolean isJsessionIdExists(AuthorizationContext object) {
    boolean isJsessionIdExists = false;
    if (Objects.nonNull(object.getExchange().getRequest().getHeaders().getFirst(COOKIE))) {
      isJsessionIdExists =
          object
              .getExchange()
              .getRequest()
              .getHeaders()
              .get(COOKIE)
              .get(0)
              .split(";")[0]
              .contains(JSESSIONID);
    }
    return isJsessionIdExists;
  }
}
