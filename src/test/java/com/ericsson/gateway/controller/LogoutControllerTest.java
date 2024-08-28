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

import com.ericsson.gateway.BasicSpringBootTest;
import com.ericsson.gateway.util.StubbedAuthorizationServerRequests;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

class LogoutControllerTest extends BasicSpringBootTest {

  private static final String PRINCIPAL_NAME = "user1";

  @Autowired private WebTestClient client;

  @Autowired private StubbedAuthorizationServerRequests stubbedRequests;

  @Autowired private ApplicationContext context;

  @Autowired private ServerOAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepository;

  private final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("abc456", Instant.now());

  private final OAuth2AccessToken token =
      new OAuth2AccessToken(
          OAuth2AccessToken.TokenType.BEARER,
          "abc123",
          Instant.now(),
          Instant.now().plusSeconds(60));

  private final WebFilter securityCtxFilter =
      (serverWebExchange, webFilterChain) ->
          serverWebExchange
              .getSession()
              .doOnNext(
                  webSession1 ->
                      webSession1
                          .getAttributes()
                          .put(
                              "SPRING_SECURITY_CONTEXT",
                              new SecurityContextImpl(createAuthenticatedUserToken())))
              .then(webFilterChain.filter(serverWebExchange));

  private final WebFilter authorizedClientFilter =
      (serverWebExchange, webFilterChain) ->
          serverWebExchange
              .getSession()
              .flatMap(
                  webSession ->
                      clientRegistrationRepository
                          .findByRegistrationId("keycloak")
                          .map(
                              clientRegistration ->
                                  new OAuth2AuthorizedClient(
                                      clientRegistration, PRINCIPAL_NAME, token, refreshToken))
                          .flatMap(
                              oAuth2AuthorizedClient1 ->
                                  oAuth2AuthorizedClientRepository.saveAuthorizedClient(
                                      oAuth2AuthorizedClient1,
                                      createAuthenticatedUserToken(),
                                      serverWebExchange)))
              .then(webFilterChain.filter(serverWebExchange));

  @BeforeEach
  void initClient() {
    client =
        WebTestClient.bindToApplicationContext(context)
            .webFilter(securityCtxFilter, authorizedClientFilter)
            .build();
  }

  @Test
  void shouldRedirectToKeycloakAuthEndpointWhenLogoutSuccessfulAndReferrerNotSet() {
    stubbedRequests.stubTokenLogout();
    client
        .post()
        .uri("/auth/v1/logout")
        .cookie("JSESSIONID", "jsession")
        .exchange()
        .expectStatus()
        .isFound()
        .expectHeader()
        .valueMatches("Location", "/oauth2/authorization/keycloak");
  }

  @Test
  void shouldRedirectToReferrerWhenLogoutSuccessfulAndReferrerSet() {
    stubbedRequests.stubTokenLogout();
    client
        .post()
        .uri("/auth/v1/logout")
        .header("Referer", "/referrer")
        .cookie("JSESSIONID", "jsession")
        .exchange()
        .expectStatus()
        .isFound()
        .expectHeader()
        .valueMatches("Location", "/referrer");
  }

  @Test
  void shouldRedirectToKeycloakAuthEndpointWhenNoSession() {
    stubbedRequests.stubTokenLogout();
    client
        .post()
        .uri("/auth/v1/logout")
        .exchange()
        .expectStatus()
        .isFound()
        .expectHeader()
        .valueMatches("Location", "/oauth2/authorization/keycloak");
  }

  @Test
  void shouldReturnErrorResponseWhenLogoutError() {
    stubbedRequests.stubTokenLogoutWithError();
    client.post().uri("/auth/v1/logout").exchange().expectStatus().is4xxClientError();
  }

  private OAuth2AuthenticationToken createAuthenticatedUserToken() {
    final OAuth2User user =
        new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            Collections.singletonMap("sub", PRINCIPAL_NAME),
            "sub");
    return new OAuth2AuthenticationToken(
        user, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")), "keycloak");
  }

  private void createAuthorizedClient(
      final OAuth2AuthenticationToken authenticatedUserToken, ServerWebExchange webExchange) {
    final OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "abc123",
            Instant.now(),
            Instant.now().plusSeconds(60));
    final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("abc456", Instant.now());
    final ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId("keycloak").block();
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(clientRegistration, PRINCIPAL_NAME, token, refreshToken);
    oAuth2AuthorizedClientRepository
        .saveAuthorizedClient(oAuth2AuthorizedClient, authenticatedUserToken, webExchange)
        .block();
  }
}
