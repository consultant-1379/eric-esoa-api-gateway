/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.config;

import com.ericsson.gateway.exception.ApiGatewayGenericException;
import com.ericsson.gateway.exception.CustomAccessDeniedHandler;
import com.ericsson.gateway.security.authentication.tenant.TenantAwareOAuth2AuthorizationRequestRedirectWebFilter;
import com.ericsson.gateway.security.authentication.tenant.TimeoutMarkerFilter;
import com.ericsson.gateway.security.authorization.KeycloakAuthorizationManager;
import com.ericsson.gateway.util.RenameDefaultGenerationEntrypoints;
import com.ericsson.gateway.util.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties.class)
class SecurityConfig {

  @Autowired private KeycloakAuthorizationManager keycloakAuthorizationManager;

  @Autowired
  private TenantAwareOAuth2AuthorizationRequestRedirectWebFilter
      oAuth2AuthorizationRequestRedirectWebFilter;

  @Autowired TimeoutMarkerFilter timeoutMarkerFilter;

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Autowired SecurityProperties properties;

  @Bean
  public SecurityWebFilterChain configure(ServerHttpSecurity http)
      throws ApiGatewayGenericException {
    http.csrf()
        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
        .and()
        .exceptionHandling()
        .accessDeniedHandler(new CustomAccessDeniedHandler())
        .and()
        .authorizeExchange()
        .pathMatchers(properties.getAllowedPaths())
        .permitAll()
        .anyExchange()
        .access(keycloakAuthorizationManager)
        .and()
        .csrf()
        .disable()
        .oauth2Login()
        .clientRegistrationRepository(clientRegistrationRepository);
    http.headers().disable();
    http.addFilterAt(timeoutMarkerFilter, SecurityWebFiltersOrder.REACTOR_CONTEXT);
    http.addFilterAt(
        oAuth2AuthorizationRequestRedirectWebFilter, SecurityWebFiltersOrder.HTTP_BASIC);
    SecurityWebFilterChain filterChain = http.build();
    RenameDefaultGenerationEntrypoints.disableGeneratedPages(http);
    return filterChain;
  }
}
