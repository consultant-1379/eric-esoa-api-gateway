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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import com.ericsson.gateway.config.WireMockIntegrationTestConfig;
import com.ericsson.gateway.util.StubbedAuthorizationServerRequests;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(
    classes = {
      AuthorizationTest.AuthorizationTestConfiguration.class,
      WebClientAutoConfiguration.class,
      WireMockIntegrationTestConfig.class
    })
public class AuthorizationTest {

  @Autowired private KeycloakAuthorizationManager keycloakAuthManager;

  @Autowired private ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService;

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Autowired private StubbedAuthorizationServerRequests stubbedRequests;

  @Autowired private DefaultReactiveOAuth2UserService userService;

  private static final String PRINCIPAL_NAME = "user1";

  private static final String AUTHORIZATION_REFRESH_TOKEN = "Authorization-Refresh";

  @AfterEach
  void afterEach() {
    stubbedRequests.resetMappingsAndRequests();
  }

  @Test
  void shouldGrantAccessWhenRequestedResourceNotManagedByAuthServer() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticatedUserToken();
    createAuthorizedClient(authenticatedUserToken);

    stubbedRequests.stubGetProtectionApiToken().stubGetProtectedResources().stubTokenLogout();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/unmanaged_resource")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager.check(Mono.just(authenticatedUserToken), authContext).block();
    assertNotNull(authDecision);
    assertTrue(authDecision.isGranted());
  }

  @Test
  void shouldGrantAccessWhenAuthServerReturnsOK() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticatedUserToken();
    createAuthorizedClient(authenticatedUserToken);

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        .stubTokenLogout();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/resource1")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager
            .check(Mono.just(authenticatedUserToken), authContext)
            .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
            .block();
    assertNotNull(authDecision);
    assertTrue(authDecision.isGranted());
  }

  @Test
  void shouldDenyAccessWhenAuthServerReturnsError() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticatedUserToken();
    createAuthorizedClient(authenticatedUserToken);

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationDenied()
        .stubTokenLogout();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/resource1")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager.check(Mono.just(authenticatedUserToken), authContext).block();
    assertNotNull(authDecision);
    assertFalse(authDecision.isGranted());
  }

  @Test
  void shouldDenyAccessWhenMultipleResourcesMatchingRequestedURI() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticatedUserToken();
    createAuthorizedClient(authenticatedUserToken);

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetDuplicateProtectedResources()
        .stubTokenLogout();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/resource1")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager.check(Mono.just(authenticatedUserToken), authContext).block();
    assertNotNull(authDecision);
    assertFalse(authDecision.isGranted());
  }

  @Test
  void shouldExcludeScopeFromAuthRequestWhenNoResourceScopeMatchingHttpRequestMethod() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticatedUserToken();
    createAuthorizedClient(authenticatedUserToken);

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithoutScope()
        .stubTokenLogout();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.post("/resource1")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager
            .check(Mono.just(authenticatedUserToken), authContext)
            .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
            .block();
    assertNotNull(authDecision);
    assertTrue(authDecision.isGranted());
  }

  void generate() {
    stubbedRequests.stubGetProtectionApiToken();

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/resource1")));
  }

  @Test
  void shouldGrantAccessWhenAuthServerForClientCredReturnsOK() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticationTest();

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        .stubVerifyAccessToken()
        .stubTokenLogout();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/resource1")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                .header(AUTHORIZATION_REFRESH_TOKEN + createRefreshToken()));

    final AuthorizationContext authContext = new AuthorizationContext(exchange);

    final AuthorizationDecision authDecision =
        keycloakAuthManager
            .check(Mono.just(authenticatedUserToken), authContext)
            .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
            .block();
    assertNotNull(authDecision, "Authorization is successful");
    assertTrue(authDecision.isGranted());
  }

  @Test
  void shouldDenyAccessWhenAuthServerForClientCredReturnsError() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticationTest();

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        // .stubVerifyAccessToken()
        .stubAuthorizationDenied()
        .stubTokenLogout();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/resource1")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                .header(AUTHORIZATION_REFRESH_TOKEN + createRefreshToken()));

    final AuthorizationContext authContext =
        new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/resource1")));

    final AuthorizationDecision authDecision =
        keycloakAuthManager.check(Mono.just(authenticatedUserToken), authContext).block();

    assertNotNull(authDecision, "Authorization failed due to error received");
  }

  @Test
  void shouldDenyAccessWhenAuthServerForClientCredWithInvalidTokenUsed() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticationTest();

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        .stubVerifyAccessToken()
        .stubAuthorizationDenied()
        .stubTokenLogout();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/resource1")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createInvalidToken())
                .header(AUTHORIZATION_REFRESH_TOKEN + createRefreshToken()));

    final AuthorizationContext authContext = new AuthorizationContext(exchange);

    AuthorizationDecision authDecision = null;
    try {
      authDecision =
          keycloakAuthManager
              .check(Mono.just(authenticatedUserToken), authContext)
              .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
              .block();
    } catch (OAuth2AuthorizationException exception) {
      assertNull(
          authDecision,
          "Authorization is failed due to invalid/corrupted access token value provided.");
    }
  }

  @Test
  void shouldDenyAccessWhenAuthServerForClientCredWithEmptyAccessToken() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticationTest();

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        .stubVerifyAccessToken()
        .stubAuthorizationDenied()
        .stubTokenLogout();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/resource1")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer ")
                .header(AUTHORIZATION_REFRESH_TOKEN + createRefreshToken()));
    final AuthorizationContext authContext = new AuthorizationContext(exchange);

    AuthorizationDecision authDecision = null;
    try {
      authDecision =
          keycloakAuthManager
              .check(Mono.just(authenticatedUserToken), authContext)
              .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
              .block();
    } catch (OAuth2AuthorizationException exception) {
      assertNull(authDecision, "Authorization is failed due to empty access token value.");
    }
  }

  @Test
  void shouldDenyAccessWhenAuthServerForClientCredWithAuthBasicToken() {
    final OAuth2AuthenticationToken authenticatedUserToken = createAuthenticationTest();

    stubbedRequests
        .stubGetProtectionApiToken()
        .stubGetProtectedResources()
        .stubAuthorizationSuccessWithGetScope()
        .stubVerifyAccessToken()
        .stubAuthorizationDenied()
        .stubTokenLogout();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/resource1")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                .header(HttpHeaders.AUTHORIZATION, "Basic " + createToken())
                .header(AUTHORIZATION_REFRESH_TOKEN + createRefreshToken()));

    final AuthorizationContext authContext = new AuthorizationContext(exchange);

    AuthorizationDecision authDecision = null;
    try {
      authDecision =
          keycloakAuthManager
              .check(Mono.just(authenticatedUserToken), authContext)
              .contextWrite(Context.of(ServerWebExchange.class, authContext.getExchange()))
              .block();
    } catch (OAuth2AuthorizationException exception) {
      assertNull(authDecision, "Authorization is failed due to invalid schema provided.");
    }
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

  protected String createToken() {
    final OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwZ284cGc1Y0czaDFVZEI1WmZmUTJweWZuVVVqOG9YMVNTLV9EQ0xjWm44In0.eyJleHAiOjE2NjM2NTExMjgsImlhdCI6MTY2MzY0OTMyOCwianRpIjoiOTQyMDBlMzEtMDcwMi00NzYxLWIwMmEtMzI4ZDRjMmRkNWZjIiwiaXNzIjoiaHR0cHM6Ly9pYW0uZWlhcHNlZjMuYzEyNS1oYXJ0MTEwLmV3cy5naWMuZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjBmMTcyM2IxLWQ3OTktNDEzOC05ODZjLTNmYzg1MzBlNWM2MCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImNsaWVudGNyZWRuZXciLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwic2NvcGUiOiJwcm9maWxlIHJvbGVzIGVtYWlsIiwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInRlbmFudF9uYW1lIjoibWFzdGVyIiwiY2xpZW50SWQiOiJjbGllbnRjcmVkbmV3IiwiY2xpZW50SG9zdCI6IjEwLjE1Ni4xMjAuNTMiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1jbGllbnRjcmVkbmV3IiwiY2xpZW50QWRkcmVzcyI6IjEwLjE1Ni4xMjAuNTMifQ.F-gv5xAaSmlhrRArMdgpnlnhxpkCcLRcXjUTzuNBZsfQNzZ_5YwIs_tRtDbbm5dcrW1O7b5oI9yI0gfGR3O0llSpgFwWeYU42H4uTXUZclbfKR5XENL9RuICQO-EoZWjAtcF7qrwA_N6NHkvCadQ0-jzzuDIJgmJvuLNPYJZEo42atEzkkyqgLAe4HosQQsLUMTJiwZVx4eiNs1K4X6biUOtoWe1tkU-Wo_Dj_tFtCEJy3hBfQtDVUqd1mZHZr5By15cj9YoAGPzaSNnkNoJFGpZG06CUnqbHcesb3y0zTVf1fuZUWsvPIu8GYafhYi4L1ixLmzDal6LdnAcrTqSIg",
            Instant.now(),
            Instant.now().plusSeconds(60));
    return token.getTokenValue();
  }

  protected String createRefreshToken() {
    final OAuth2RefreshToken oAuth2RefreshToken =
        new OAuth2RefreshToken("refreshToken", Instant.now());
    return oAuth2RefreshToken.getTokenValue();
  }

  protected String createInvalidToken() {
    final OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2i00NzYxLWIwMmEtMzI4ZDRjMmRkNWZjIiwiaXNzIjoiaHJdfX0sInRlbmFudF9uYW1UsInByZWZlcnJlZF91c2VybmFtZSI6InNlczcyI6IjEwLjE1Ni4xMjAuNTMiK4X6biUOtoWe1tkU-Wo_Dj_tFtCEJy3hBfQtDVUqd1mZafhYi4L1ixLmzDal6LdnAcrTqSIg",
            Instant.now(),
            Instant.now().plusSeconds(60));
    return token.getTokenValue();
  }

  private OAuth2AuthenticationToken createAuthenticationTest() {
    final OAuth2User user =
        new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            Collections.singletonMap("sub", PRINCIPAL_NAME),
            "sub");

    Mockito.when(userService.loadUser(any(OAuth2UserRequest.class))).thenReturn(Mono.just(user));

    final OAuth2AuthenticationToken token =
        new OAuth2AuthenticationToken(
            user, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")), "keycloak");
    return token;
  }

  private void createAuthorizedClient(final OAuth2AuthenticationToken authenticatedUserToken) {
    final OAuth2AccessToken token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "abc123",
            Instant.now(),
            Instant.now().plusSeconds(60));
    final ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId("keycloak").block();
    if (clientRegistration != null) {
      final OAuth2AuthorizedClient oAuth2AuthorizedClient =
          new OAuth2AuthorizedClient(clientRegistration, PRINCIPAL_NAME, token);
      oAuth2AuthorizedClientService
          .saveAuthorizedClient(oAuth2AuthorizedClient, authenticatedUserToken)
          .block();
    }
  }

  @TestConfiguration
  @EnableConfigurationProperties(OAuth2ClientProperties.class)
  @ComponentScan(basePackages = "com.ericsson.gateway.security.authorization")
  public static class AuthorizationTestConfiguration {

    @Bean
    public ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService(
        ReactiveClientRegistrationRepository clientRegistrationRepository) {
      return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository(
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService) {
      return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(
          reactiveOAuth2AuthorizedClientService);
    }
  }
}
