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

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.gateway.exception.ApiGatewayHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Create <code>OAuth2AuthorizationRequest</code> towards requested realm/tenant. Identifies the
 * requested tenant via 'tenant' query parameter in the original request.
 *
 * <p>If 'tenant' query parameter is not supplied then no change to the standard flow and <code>
 * OAuth2AuthorizationRequest</code> is handled by <code>
 * DefaultServerOAuth2AuthorizationRequestResolver</code>.
 *
 * <p>If 'tenant' query parameter is supplied then create and stores a <code>ClientRegistration
 * </code> for the new tenant containing details of the tenant realm. Assumes the same client id and
 * secret as configured client registration. The created <code>OAuth2AuthorizationRequest</code>
 * contains the id of the dynamically created client. The redirectUri in the request redirects to
 * the realm identified by the tenant.
 */
@Component
public class TenantAwareServerOAuth2AuthorizationRequestResolver
    extends DefaultServerOAuth2AuthorizationRequestResolver {

  private final ReactiveClientRegistrationRepository clientRegistrationRepository;

  private final WebSessionServerRequestCache webSessionServerRequestCache =
      new WebSessionServerRequestCache();

  private static final String TENANT_QUERY_PARAM = "tenant";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TenantAwareServerOAuth2AuthorizationRequestResolver.class);

  public TenantAwareServerOAuth2AuthorizationRequestResolver(
      @Autowired ReactiveClientRegistrationRepository clientRegistrationRepository) {
    super(clientRegistrationRepository);
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  @Override
  public Mono<OAuth2AuthorizationRequest> resolve(
      ServerWebExchange exchange, String clientRegistrationId) {
    return resolveTenantNameFromOriginalRequest(exchange)
        .flatMap(tenantName -> createAuthorizedClientForTenant(tenantName, clientRegistrationId))
        .flatMap(
            tenantClientRegistration ->
                super.resolve(exchange, tenantClientRegistration.getRegistrationId()))
        .switchIfEmpty(super.resolve(exchange, clientRegistrationId));
  }

  private Mono<ClientRegistration> createAuthorizedClientForTenant(
      final String tenantName, final String clientRegistrationId) {
    return this.clientRegistrationRepository
        .findByRegistrationId(clientRegistrationId + "-" + tenantName)
        .switchIfEmpty(
            this.clientRegistrationRepository
                .findByRegistrationId(clientRegistrationId)
                .map(
                    clientRegistration -> {
                      final String tenantIssuerUri =
                          clientRegistration
                              .getProviderDetails()
                              .getTokenUri()
                              .replaceFirst("realms/.*", "realms/" + tenantName);
                      return ClientRegistrations.fromOidcIssuerLocation(tenantIssuerUri)
                          .registrationId(clientRegistration.getRegistrationId() + "-" + tenantName)
                          .clientId(clientRegistration.getClientId())
                          .clientSecret(clientRegistration.getClientSecret())
                          .build();
                    })
                .doOnError(
                    throwable -> {
                      throw ApiGatewayHttpException.notFound(
                          ApiGatewayErrorCode.TENANT_NOT_FOUND, tenantName);
                    })
                .flatMap(
                    tenantClientRegistration ->
                        ((InMemoryModifiableReactiveClientRegistrationRepository)
                                clientRegistrationRepository)
                            .addClientRegistration(tenantClientRegistration)
                            .thenReturn(tenantClientRegistration)));
  }

  private Mono<String> resolveTenantNameFromOriginalRequest(final ServerWebExchange exchange) {
    return webSessionServerRequestCache
        .getRedirectUri(exchange)
        .filter(uri -> uri.getQuery() != null && uri.getQuery().contains(TENANT_QUERY_PARAM))
        .map(
            uri ->
                UriComponentsBuilder.fromUriString(uri.toString())
                    .build()
                    .getQueryParams()
                    .getFirst(TENANT_QUERY_PARAM));
  }
}
