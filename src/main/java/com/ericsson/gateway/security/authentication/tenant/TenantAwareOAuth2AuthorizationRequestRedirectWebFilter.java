/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authentication.tenant;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.OAuth2AuthorizationRequestRedirectWebFilter;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Initiates the authorization code grant by redirecting to authorization servers authorization
 * endpoint. Used in place of provided {@link OAuth2AuthorizationRequestRedirectWebFilter} in order
 * to use custom {@link ServerOAuth2AuthorizationRequestResolver}.
 *
 * @implNote Not necessary from spring-security-oauth2-client 5.2.0 release onwards which supports
 *     configuration of custom <code>ServerOAuth2AuthorizationRequestResolver</code>.
 * @see TenantAwareServerOAuth2AuthorizationRequestResolver
 */
@Component
public class TenantAwareOAuth2AuthorizationRequestRedirectWebFilter implements WebFilter {

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Autowired
  private TenantAwareServerOAuth2AuthorizationRequestResolver authorizationRequestResolver;

  private final ServerRedirectStrategy authorizationRedirectStrategy = new CustomRedirectStrategy();

  private final ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository = new WebSessionOAuth2ServerAuthorizationRequestRepository();
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TenantAwareOAuth2AuthorizationRequestRedirectWebFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return this.authorizationRequestResolver
        .resolve(exchange)
        .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
        .onErrorResume(
            ClientAuthorizationRequiredException.class,
            e -> {
              return this.authorizationRequestResolver.resolve(
                  exchange, e.getClientRegistrationId());
            })
        .flatMap(
            clientRegistration -> {
              return this.sendRedirectForAuthorization(exchange, clientRegistration);
            });
  }

  private Mono<Void> sendRedirectForAuthorization(
      ServerWebExchange exchange, OAuth2AuthorizationRequest authorizationRequest) {
    return Mono.defer(
        () -> {
          Mono<Void> saveAuthorizationRequest = Mono.empty();
          if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(
              authorizationRequest.getGrantType())) {
            saveAuthorizationRequest =
                this.authorizationRequestRepository.saveAuthorizationRequest(
                    authorizationRequest, exchange);
          }
          URI redirectUri =
              UriComponentsBuilder.fromUriString(authorizationRequest.getAuthorizationRequestUri())
                  .build(true)
                  .toUri();
          return saveAuthorizationRequest.then(
              this.authorizationRedirectStrategy.sendRedirect(exchange, redirectUri));
        });
  }
}
