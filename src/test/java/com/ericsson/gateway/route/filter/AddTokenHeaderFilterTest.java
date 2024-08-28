/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.route.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ericsson.gateway.security.authorization.client.TokenAPI;
import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AddTokenHeaderFilterTest {

  @InjectMocks private AddTokenHeaderFilter addTokenHeaderFilter;

  @Mock private ReactiveOAuth2AuthorizedClientService clientService;

  @Mock private TokenAPI tokenAPI;

  @Mock private GatewayFilterChain gatewayFilterChain;

  private MockServerWebExchange exchange, exchange1, exchange2;

  private static final String AUTHORIZATION_REFRESH_TOKEN = "Authorization-Refresh";

  private SecurityContext securityContext =
      new SecurityContextImpl(createOAuth2AuthenticationToken());

  @BeforeEach
  void setup() {
    exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
    exchange1 =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer accessToken")
                .header(AUTHORIZATION_REFRESH_TOKEN, "refreshToken"));
    exchange2 =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer accessToken"));
    securityContext = new SecurityContextImpl(createOAuth2AuthenticationToken());
    exchange.getSession().block().getAttributes().put("SPRING_SECURITY_CONTEXT", securityContext);
  }

  @Test
  void shouldAddTokenWithoutRefreshWhenRefeshStrategyNotSet() {
    final OAuth2AccessToken oAuth2AccessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "accessToken",
            Instant.now(),
            Instant.now().plusSeconds(60));
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(createClientRegistration(), "test", oAuth2AccessToken);

    when(clientService.loadAuthorizedClient(anyString(), anyString()))
        .thenReturn(Mono.just(oAuth2AuthorizedClient));
    when(gatewayFilterChain.filter(any())).thenReturn(Mono.empty());

    addTokenHeaderFilter
        .apply(new AddTokenHeaderFilter.Config())
        .filter(exchange, gatewayFilterChain)
        .block();

    assertEquals(
        "Bearer " + oAuth2AuthorizedClient.getAccessToken().getTokenValue(),
        exchange.getRequest().getHeaders().get("Authorization").get(0));

    verifyNoMoreInteractions(tokenAPI);
  }

  @Test
  void shouldAddTokenWithoutRefreshWhenRefeshStrategyExpiringAndTokenExpiryGreaterThan5s() {
    final OAuth2AccessToken oAuth2AccessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "accessToken",
            Instant.now(),
            Instant.now().plusSeconds(10));
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(createClientRegistration(), "test", oAuth2AccessToken);

    when(clientService.loadAuthorizedClient(anyString(), anyString()))
        .thenReturn(Mono.just(oAuth2AuthorizedClient));
    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    final AddTokenHeaderFilter.Config config = new AddTokenHeaderFilter.Config();
    config.setRefreshStrategy("expiring");
    addTokenHeaderFilter.apply(config).filter(exchange, gatewayFilterChain).block();

    assertEquals(
        "Bearer " + oAuth2AuthorizedClient.getAccessToken().getTokenValue(),
        exchange.getRequest().getHeaders().get("Authorization").get(0));

    verifyNoMoreInteractions(tokenAPI);
  }

  @ParameterizedTest
  @MethodSource("provideAccessTokensForRefreshStrategy")
  void shouldRefreshTokenWhenRefeshStrategyIsAlwaysOrExpiring(
      final String refreshStrategy, final OAuth2AccessToken oAuth2AccessToken) {
    final OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh_token", Instant.now());

    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(
            createClientRegistration(), "test", oAuth2AccessToken, refreshToken);

    final OAuth2AccessTokenResponse refreshAuth2AccessTokenResponse =
        createOAuth2AccessTokenResponse("accessToken2");

    when(clientService.loadAuthorizedClient(anyString(), anyString()))
        .thenReturn(Mono.just(oAuth2AuthorizedClient));
    when(tokenAPI.refreshToken(any(OAuth2AuthorizedClient.class)))
        .thenReturn(Mono.just(refreshAuth2AccessTokenResponse));
    when(clientService.saveAuthorizedClient(any(), any())).thenReturn(Mono.empty());
    when(gatewayFilterChain.filter(any())).thenReturn(Mono.empty());

    final AddTokenHeaderFilter.Config config = new AddTokenHeaderFilter.Config();
    config.setRefreshStrategy(refreshStrategy);
    addTokenHeaderFilter.apply(config).filter(exchange, gatewayFilterChain).block();

    assertEquals(
        "Bearer " + refreshAuth2AccessTokenResponse.getAccessToken().getTokenValue(),
        exchange.getRequest().getHeaders().get("Authorization").get(0));
  }

  @Test
  void shouldAddRefreshTokenHeaderWhenConfigured() {
    final OAuth2AccessToken oAuth2AccessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "accessToken",
            Instant.now(),
            Instant.now().plusSeconds(10));
    final OAuth2RefreshToken oAuth2RefreshToken =
        new OAuth2RefreshToken("refreshToken", Instant.now());
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(
            createClientRegistration(), "test", oAuth2AccessToken, oAuth2RefreshToken);

    when(clientService.loadAuthorizedClient(anyString(), anyString()))
        .thenReturn(Mono.just(oAuth2AuthorizedClient));
    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    final AddTokenHeaderFilter.Config config = new AddTokenHeaderFilter.Config();
    config.setIncludeRefreshToken(true);
    addTokenHeaderFilter.apply(config).filter(exchange, gatewayFilterChain).block();

    assertEquals(
        oAuth2AuthorizedClient.getRefreshToken().getTokenValue(),
        exchange.getRequest().getHeaders().get("Authorization-Refresh").get(0));

    verifyNoMoreInteractions(tokenAPI);
  }

  @Test
  void shouldAddAccessTokenWhenConfigured() {
    final OAuth2AccessToken oAuth2AccessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "accessToken",
            Instant.now(),
            Instant.now().plusSeconds(10));
    final OAuth2RefreshToken oAuth2RefreshToken =
        new OAuth2RefreshToken("refreshToken", Instant.now());
    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(
            createClientRegistration(), "test", oAuth2AccessToken, oAuth2RefreshToken);

    final AddTokenHeaderFilter.Config config = new AddTokenHeaderFilter.Config();
    config.setIncludeRefreshToken(true);
    addTokenHeaderFilter.apply(config).filter(exchange1, gatewayFilterChain);

    assertNotNull(exchange1.getRequest().getHeaders().get("Authorization").get(0), "Token exists");
    assertEquals(
        "Bearer accessToken",
        exchange1.getRequest().getHeaders().get("Authorization").get(0),
        "Valid token received..");
    assertEquals(
        "refreshToken",
        exchange1.getRequest().getHeaders().get(AUTHORIZATION_REFRESH_TOKEN).get(0),
        "Valid refresh token received..");
    verifyNoMoreInteractions(tokenAPI);
  }

  @Test
  void shouldAddAccessTokenWhenRefreshTokenNotConfigured() {
    final OAuth2AccessToken oAuth2AccessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "accessToken",
            Instant.now(),
            Instant.now().plusSeconds(10));

    final OAuth2AuthorizedClient oAuth2AuthorizedClient =
        new OAuth2AuthorizedClient(createClientRegistration(), "test", oAuth2AccessToken);

    final AddTokenHeaderFilter.Config config = new AddTokenHeaderFilter.Config();
    config.setIncludeRefreshToken(true);
    addTokenHeaderFilter.apply(config).filter(exchange2, gatewayFilterChain);

    assertNotNull(exchange2.getRequest().getHeaders().get("Authorization").get(0), "Token exists");
    assertEquals(
        "Bearer accessToken",
        exchange2.getRequest().getHeaders().get("Authorization").get(0),
        "Valid token received..");
    verifyNoMoreInteractions(tokenAPI);
  }

  private static Stream<Arguments> provideAccessTokensForRefreshStrategy() {
    final Arguments validToken =
        Arguments.of(
            "always",
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "accessToken",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60)));

    final Arguments expiredToken =
        Arguments.of(
            "expiring",
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "accessToken",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(30)));

    final Arguments expiringToken =
        Arguments.of(
            "expiring",
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "accessToken",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(5)));

    return Stream.of(validToken, expiredToken, expiringToken);
  }

  private ClientRegistration createClientRegistration() {
    return ClientRegistration.withRegistrationId("client")
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .clientId("client")
        .clientSecret("secret")
        .tokenUri("tokenUri")
        .build();
  }

  private OAuth2AuthenticationToken createOAuth2AuthenticationToken() {
    final OAuth2User user =
        new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            Collections.singletonMap("sub", "sub1"),
            "sub");

    return new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");
  }

  private OAuth2AccessTokenResponse createOAuth2AccessTokenResponse(final String accessToken) {
    return OAuth2AccessTokenResponse.withToken(accessToken)
        .tokenType(OAuth2AccessToken.TokenType.BEARER)
        .refreshToken("refreshToken")
        .build();
  }
}
