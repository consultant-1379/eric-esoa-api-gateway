/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authorization.client;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.*;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Client providing access to the Authorization Servers OAuth2 Token API. */
@Component
public class TokenAPI {

  private final ServerOAuth2AuthorizedClientExchangeFilterFunction refreshTokenFilter;
  private final WebClient webClient;

  private static final Logger LOGGER = LoggerFactory.getLogger(TokenAPI.class);

  public TokenAPI(
      final OAuth2ClientProperties props,
      final ReactiveClientRegistrationRepository clientRegistrationRepository,
      final ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
      final WebClient.Builder webClientBuilder) {

    OAuth2ClientProperties.Provider keycloakProvider = props.getProvider().get("keycloak");
    this.refreshTokenFilter =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrationRepository, authorizedClientRepository);
    this.refreshTokenFilter.setDefaultClientRegistrationId("keycloak");
    this.refreshTokenFilter.setAccessTokenExpiresSkew(Duration.ofSeconds(5));
    this.refreshTokenFilter.setAuthorizationFailureHandler(
        new ReactiveOAuth2AuthorizationFailureHandler() {
          @Override
          public Mono<Void> onAuthorizationFailure(
              OAuth2AuthorizationException e,
              Authentication authentication,
              Map<String, Object> map) {
            return null;
          }
        });
    this.webClient = webClientBuilder.baseUrl(keycloakProvider.getTokenUri()).build();
  }

  /**
   * Check access to the requested resource and scope is authorized. The authorization server will
   * evaluate all policies associated with the resource and scope being requested and issue a
   * Requesting Party Token (RPT) with all permissions granted by the server.
   *
   * <p>If the user has no permissions for the requested resource and scope then
   * OAuth2AuthorizationException will be thrown.
   *
   * @param resource the protected resource
   * @param scope the resource scope which is optional
   * @param oauth2Client oAuth2AuthorizedClient
   * @return OAuth2AccessTokenResponse Mono containing Requesting Party Token (RPT)
   */
  public Mono<OAuth2AccessTokenResponse> authorize(
      final ProtectedResource resource,
      final String scope,
      final OAuth2AuthorizedClient oauth2Client) {

    return Mono.defer(
        () -> {
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(GRANT_TYPE, "urn:ietf:params:oauth:grant-type:uma-ticket")
                  .with("audience", oauth2Client.getClientRegistration().getClientId())
                  .with("permission", resource.getName() + (scope != null ? "#" + scope : ""));
          return webClient
              .mutate()
              .baseUrl(oauth2Client.getClientRegistration().getProviderDetails().getTokenUri())
              .filter(refreshTokenFilter)
              .build()
              .post()
              .attributes(oauth2AuthorizedClient(oauth2Client))
              .accept(MediaType.APPLICATION_JSON)
              .headers(
                  headers -> headers.setBearerAuth(oauth2Client.getAccessToken().getTokenValue()))
              .body(body)
              .exchange()
              .flatMap(response -> response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse()));
        });
  }

  /**
   * Verify the access token provided and also check access to the requested resource and scope is
   * authorized. The authorization server will evaluate all policies associated with the resource
   * and scope being requested and issue a Requesting Party Token (RPT) with all permissions granted
   * by the server.
   *
   * <p>If the user has no permissions for the requested resource and scope then
   * OAuth2AuthorizationException will be thrown.
   *
   * @param accessToken accessToken
   * @param clientRegistration clientRegistration
   * @param resource the protected resource
   * @param scope the resource scope which is optional
   * @return {@link Mono}
   * @see Mono
   * @see OAuth2AccessTokenResponse Mono containing Requesting Party Token (RPT)
   */
  public Mono<OAuth2AccessTokenResponse> verifyAccessToken(
      final String accessToken,
      final ClientRegistration clientRegistration,
      final ProtectedResource resource,
      final String scope) {

    return Mono.defer(
        () -> {
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(GRANT_TYPE, "urn:ietf:params:oauth:grant-type:uma-ticket")
                  .with("audience", clientRegistration.getClientId())
                  .with("permission", resource.getName() + (scope != null ? "#" + scope : ""));
          return webClient
              .mutate()
              .baseUrl(clientRegistration.getProviderDetails().getTokenUri())
              .build()
              .post()
              .accept(MediaType.APPLICATION_JSON)
              .headers(
                  httpHeaders -> {
                    httpHeaders.setBearerAuth(accessToken);
                  })
              .body(body)
              .exchange()
              .flatMap(response -> response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse()));
        });
  }

  /**
   * Request a Protection API Token (PAT).
   *
   * @param clientRegistration the registered oauth2 client
   * @return OAuth2AccessTokenResponse Mono
   */
  public Mono<OAuth2AccessTokenResponse> getProtectionApiToken(
      final ClientRegistration clientRegistration) {

    return Mono.defer(
        () -> {
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(GRANT_TYPE, "client_credentials")
                  .with(CLIENT_ID, clientRegistration.getClientId())
                  .with(CLIENT_SECRET, clientRegistration.getClientSecret());

          return webClient
              .post()
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .exchange()
              .flatMap(response -> response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse()));
        });
  }

  /**
   * Request user access token.
   *
   * @param userName the user name
   * @param password the user password
   * @param clientRegistration the registered oauth2 client
   * @return OAuth2AccessTokenResponse Mono
   */
  public Mono<OAuth2AccessTokenResponse> getUserAccessToken(
      final String userName, final String password, final ClientRegistration clientRegistration) {
    return Mono.defer(
        () -> {
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(GRANT_TYPE, "password")
                  .with(CLIENT_ID, clientRegistration.getClientId())
                  .with(CLIENT_SECRET, clientRegistration.getClientSecret())
                  .with("username", userName)
                  .with("password", password);

          return webClient
              .mutate()
              .baseUrl(clientRegistration.getProviderDetails().getTokenUri())
              .build()
              .post()
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .exchange()
              .flatMap(
                  response -> {
                    if (response.statusCode().isError()) {
                      return response.createException().flatMap(Mono::error);
                    }
                    return response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse());
                  });
        });
  }

  /**
   * Refresh an access token.
   *
   * @param oAuth2AuthorizedClient the oauth2 authorized client
   * @return OAuth2AccessTokenResponse Mono
   */
  public Mono<OAuth2AccessTokenResponse> refreshToken(
      final OAuth2AuthorizedClient oAuth2AuthorizedClient) {

    return Mono.defer(
        () -> {
          final ClientRegistration clientRegistration =
              oAuth2AuthorizedClient.getClientRegistration();
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(GRANT_TYPE, "refresh_token")
                  .with(REFRESH_TOKEN, oAuth2AuthorizedClient.getRefreshToken().getTokenValue())
                  .with(CLIENT_ID, clientRegistration.getClientId())
                  .with(CLIENT_SECRET, clientRegistration.getClientSecret());

          return webClient
              .mutate()
              .baseUrl(clientRegistration.getProviderDetails().getTokenUri())
              .build()
              .post()
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .exchange()
              .flatMap(response -> response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse()));
        });
  }

  /**
   * Logout a client session.
   *
   * @param clientRegistration the registered oauth2 client
   * @param refreshToken the refresh token value
   * @return empty mono
   */
  public Mono<Void> logoutToken(
      final ClientRegistration clientRegistration, final String refreshToken) {
    return Mono.defer(
        () -> {
          final BodyInserters.FormInserter<String> body =
              BodyInserters.fromFormData(REFRESH_TOKEN, refreshToken)
                  .with(CLIENT_ID, clientRegistration.getClientId())
                  .with(CLIENT_SECRET, clientRegistration.getClientSecret());

          return webClient
              .mutate()
              .baseUrl(
                  clientRegistration.getProviderDetails().getTokenUri().replace("token", "logout"))
              .build()
              .post()
              .body(body)
              .retrieve()
              .bodyToMono(Object.class)
              .then(Mono.empty());
        });
  }
}
