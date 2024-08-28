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

import com.ericsson.gateway.security.authorization.client.ProtectedResource;
import com.ericsson.gateway.security.authorization.client.TokenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Enforce the authorization policies and permissions configured in the Authorization server for the
 * resource identified by the incoming http request.
 *
 * <p>A request is mapped to a resource by matching the uri of the request to a resource configured
 * in authorization server. Ant-style path patterns are supported for the configured resource uris.
 *
 * <p>If scope is defined for the resource which matches the name of the requested http method then
 * the scope will be included in the authorization check. Otherwise authorization is evaluated on
 * the resource only.
 *
 * <p>If no resource is configured in the authorization server which matches the http request then
 * access is granted without calling the Autorization server.
 */
@Component
class PolicyEnforcer {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyEnforcer.class);

  private final AntPathMatcher antPathMatcher = new AntPathMatcher();

  @Autowired private ReactiveOAuth2AuthorizedClientService oauth2ClientService;

  @Autowired private ProtectedResourcesTtlCache protectedResourcesTtlCache;

  @Autowired private TokenAPI tokenAPI;

  /**
   * Enforce authorization policies and permissions configured in the Authorization server for the
   * incoming http request.
   *
   * @param authentication the authentication
   * @param exchange the server web exchange
   * @return AuthorizationDecision Mono
   */
  public Mono<AuthorizationDecision> enforce(
      final Authentication authentication, final ServerWebExchange exchange) {
    LOGGER.debug("PE: Principal= " + authentication.getPrincipal().toString());
    LOGGER.debug("PE: Authenticated= " + authentication.isAuthenticated());
    LOGGER.debug("PE: Cookie based flow ");
    return Mono.defer(
        () -> {
          final String requestUri = exchange.getRequest().getPath().value();
          final String authToken =
              exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

          return protectedResourcesTtlCache
              .readAllProtectedResources()
              .filter(res -> resourceMatchesRequestUri(res, requestUri))
              .singleOrEmpty()
              .doOnSuccess(
                  res ->
                      LOGGER.debug(
                          "Found matching resource?: request={}, resource={}", requestUri, res))
              .flatMap(
                  res -> {
                    return checkResourceAccessPermitted(res, authentication, exchange);
                  })
              .defaultIfEmpty(new AuthorizationDecision(true))
              .doOnSuccessOrError(
                  (decision, throwable) -> logAuthorizationResult(decision, throwable, exchange))
              .onErrorReturn(new AuthorizationDecision(false));
        });
  }

  /**
   * Enforce authorization policies and permissions configured in the Authorization server for the
   * client credential request
   *
   * @param exchange exchange
   * @param bearerToken bearerToken
   * @param clientRegistration clientRegistration
   * @return {@link Mono}
   * @see Mono
   * @see OAuth2AccessTokenResponse
   */
  public Mono<OAuth2AccessTokenResponse> enforce(
      final ServerWebExchange exchange,
      final String bearerToken,
      final ClientRegistration clientRegistration) {

    return Mono.defer(
        () -> {
          final String requestUri = exchange.getRequest().getPath().value();
          LOGGER.debug("PE:Client credential flow ");
          return protectedResourcesTtlCache
              .readAllProtectedResources()
              .filter(res -> resourceMatchesRequestUri(res, requestUri))
              .singleOrEmpty()
              .doOnSuccess(
                  resource ->
                      LOGGER.debug(
                          "Found matching resource?: request={}, resource={}",
                          requestUri,
                          resource))
              .flatMap(
                  resource -> {
                    return tokenAPI.verifyAccessToken(
                        bearerToken,
                        clientRegistration,
                        resource,
                        resolveAuthorizationScope(
                            exchange.getRequest().getMethodValue(), resource));
                  })
              .doOnSuccessOrError(
                  (decision, throwable) ->
                      logAuthorizationResult(
                          new AuthorizationDecision(false), throwable, exchange));
        });
  }

  private boolean resourceMatchesRequestUri(
      final ProtectedResource resource, final String requestURI) {
    return !"Default Resource".equals(resource.getName())
        && resource.getUris().stream().anyMatch(uri -> antPathMatcher.match(uri, requestURI));
  }

  private Mono<AuthorizationDecision> checkResourceAccessPermitted(
      final ProtectedResource resource,
      final Authentication authentication,
      final ServerWebExchange exchange) {
    return oauth2ClientService
        .loadAuthorizedClient(
            ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId(),
            authentication.getName())
        .flatMap(
            oauth2AuthClient ->
                tokenAPI.authorize(
                    resource,
                    resolveAuthorizationScope(exchange.getRequest().getMethodValue(), resource),
                    oauth2AuthClient))
        .thenReturn(new AuthorizationDecision(true));
  }

  private String resolveAuthorizationScope(final String scope, final ProtectedResource resource) {
    return resource.getScopes() != null
            && resource.getScopes().stream().anyMatch(s -> s.equalsIgnoreCase(scope))
        ? scope
        : null;
  }

  private void logAuthorizationResult(
      final AuthorizationDecision authDecision,
      final Throwable t,
      final ServerWebExchange exchange) {
    if (t != null) {
      final String details =
          t instanceof WebClientResponseException
              ? ((WebClientResponseException) t).getResponseBodyAsString()
              : null;
      LOGGER.error("Authorization error: msg={}, details={}", t.getMessage(), details);
    }
    LOGGER.debug(
        "Authorization decision: request={}:{}, granted={}",
        exchange.getRequest().getPath(),
        exchange.getRequest().getMethodValue(),
        (authDecision != null && authDecision.isGranted()));
  }
}
