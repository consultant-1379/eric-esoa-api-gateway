/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.controller;

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.gateway.exception.ApiGatewayHttpException;
import com.ericsson.gateway.provider.KeycloakProperties;
import com.ericsson.gateway.security.authentication.tenant.InMemoryModifiableReactiveClientRegistrationRepository;
import com.ericsson.gateway.security.authorization.client.TokenAPI;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/auth")
public class LoginController {

  private static final String LAST_REQUEST_TIME = "lastRequestTime";

  @Autowired private KeycloakProperties keycloakProp;

  @Autowired private ReactiveOAuth2AuthorizedClientService authorizedClientService;

  @Autowired private TokenAPI tokenApi;

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepo;

  @Autowired private DefaultReactiveOAuth2UserService userService;

  /** Validate credentials from the IAM server. */
  @RequestMapping(
      path = {"/v1", "/v1/login"},
      method = {RequestMethod.POST})
  public Mono<String> validate(
      @RequestHeader(name = "X-Login", required = true) String userName,
      @RequestHeader(name = "X-password", required = true) String password,
      @RequestHeader(name = "X-tenant", required = false) Optional<String> tenantName,
      ServerWebExchange exchange,
      WebSession session) {

    return findOrCreateClientRegistration(tenantName)
        .zipWhen(clientRegistation -> getAccessToken(userName, password, clientRegistation))
        .flatMap(
            access ->
                createAuthentication(access.getT2(), access.getT1())
                    .flatMap(
                        authentication ->
                            saveAuthorizedClient(access.getT1(), access.getT2(), authentication)
                                .thenReturn(authentication)))
        .doOnSuccess(authentication -> saveSecurityContext(session, authentication))
        .thenReturn(session.getId());
  }

  private Mono<ClientRegistration> findOrCreateClientRegistration(
      final Optional<String> tenantName) {
    if (tenantName.isPresent()) {
      return clientRegistrationRepo
          .findByRegistrationId(keycloakProp.getProviderName() + "-" + tenantName.get())
          .switchIfEmpty(
              clientRegistrationRepo
                  .findByRegistrationId(keycloakProp.getProviderName())
                  .flatMap(
                      clientRegistration ->
                          createTenantClientRegistration(tenantName.get(), clientRegistration)));
    } else {
      return clientRegistrationRepo.findByRegistrationId(keycloakProp.getProviderName());
    }
  }

  private Mono<ClientRegistration> createTenantClientRegistration(
      final String tenantName, final ClientRegistration providerClientRegistration) {

    return Mono.defer(
            () -> {
              final String tenantIssuerUri =
                  providerClientRegistration
                      .getProviderDetails()
                      .getTokenUri()
                      .replaceFirst("realms/.*", "realms/" + tenantName);
              return Mono.just(
                  ClientRegistrations.fromOidcIssuerLocation(tenantIssuerUri)
                      .registrationId(
                          providerClientRegistration.getRegistrationId() + "-" + tenantName)
                      .clientId(providerClientRegistration.getClientId())
                      .clientSecret(providerClientRegistration.getClientSecret())
                      .build());
            })
        .doOnError(
            throwable -> {
              throw ApiGatewayHttpException.notFound(
                  ApiGatewayErrorCode.TENANT_NOT_FOUND, tenantName);
            })
        .flatMap(
            tenantClientRegistration ->
                ((InMemoryModifiableReactiveClientRegistrationRepository) clientRegistrationRepo)
                    .addClientRegistration(tenantClientRegistration)
                    .thenReturn(tenantClientRegistration));
  }

  private Mono<OAuth2AccessTokenResponse> getAccessToken(
      final String userName, final String password, final ClientRegistration clientRegistration) {
    return tokenApi.getUserAccessToken(userName, password, clientRegistration);
  }

  private Mono<OAuth2AuthenticationToken> createAuthentication(
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

  private Mono<Void> saveAuthorizedClient(
      final ClientRegistration clientRegistration,
      final OAuth2AccessTokenResponse response,
      final Authentication authentication) {
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(
            clientRegistration,
            authentication.getName(),
            response.getAccessToken(),
            response.getRefreshToken());
    return authorizedClientService.saveAuthorizedClient(oAuth2AuthorizedClient, authentication);
  }

  private void saveSecurityContext(final WebSession session, final Authentication authentication) {
    final SecurityContext securityContext = new SecurityContextImpl(authentication);
    session.getAttributes().put("MACHINE_CLIENT", true);
    session.getAttributes().put(LAST_REQUEST_TIME, Instant.now());
    session.getAttributes().put("SPRING_SECURITY_CONTEXT", securityContext);
  }
}
