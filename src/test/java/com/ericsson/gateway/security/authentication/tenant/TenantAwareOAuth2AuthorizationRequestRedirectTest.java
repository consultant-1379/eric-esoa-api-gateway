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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ericsson.gateway.config.WireMockIntegrationTestConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(
    classes = {
      TenantAwareServerOAuth2AuthorizationRequestResolver.class,
      TenantAwareOAuth2AuthorizationRequestRedirectWebFilter.class,
      InMemoryModifiableReactiveClientRegistrationRepository.class,
      WireMockIntegrationTestConfig.class
    })
@EnableConfigurationProperties(OAuth2ClientProperties.class)
class TenantAwareOAuth2AuthorizationRequestRedirectTest {

  private static final String SPRING_SECURITY_SAVED_REQUEST = "/original-uri";
  private static final String SPRING_SECURITY_SAVED_REQUEST_TENANT = "/original-uri?tenant=Tenant1";
  private static final String SPRING_SECURITY_SAVED_REQUEST_TENANT2 =
      "/original-uri?tenant=Tenant2";

  private static final String SPRING_OAUTH2_URI = "/oauth2/authorization/keycloak";

  @Autowired private WireMockServer mockServer;

  @Autowired
  private TenantAwareOAuth2AuthorizationRequestRedirectWebFilter
      oAuth2AuthorizationRequestRedirectWebFilter;

  @Autowired
  private InMemoryModifiableReactiveClientRegistrationRepository clientRegistrationRepository;

  @Test
  void shouldDoNothingIfNotAuthorizationCodeGrantFlow() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(oAuth2AuthorizationRequestRedirectWebFilter)
            .build();
    client.get().uri("/test").exchange().expectStatus().isOk();
  }

  @Test
  void shouldReturnUnAuthorizedOnSessionTimeOutForM2M() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(
                new InjectTimeoutWebFilter(true), oAuth2AuthorizationRequestRedirectWebFilter)
            .build();

    // verify redirect to master realm
    String expectedLocation =
        "http://localhost:"
            + mockServer.port()
            + "/auth/realms/master/protocol/openid-connect/auth.*";
    client
        .get()
        .uri(SPRING_OAUTH2_URI)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldReturnUnAuthorizedOnSessionTimeOutForNonM2M() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(
                new InjectTimeoutWebFilter(false), oAuth2AuthorizationRequestRedirectWebFilter)
            .build();

    // verify redirect to master realm
    String expectedLocation =
        "http://localhost:"
            + mockServer.port()
            + "/auth/realms/master/protocol/openid-connect/auth.*";
    client
        .get()
        .uri(SPRING_OAUTH2_URI)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.FOUND)
        .expectHeader()
        .valueMatches("location", expectedLocation);
  }

  @Test
  void shouldRedirectToConfiguredKeycloakProviderWhenTenantQueryParamNotSet() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(
                new SavedRequestWebFilter(SPRING_SECURITY_SAVED_REQUEST),
                oAuth2AuthorizationRequestRedirectWebFilter)
            .build();

    // verify redirect to master realm
    String expectedLocation =
        "http://localhost:"
            + mockServer.port()
            + "/auth/realms/master/protocol/openid-connect/auth.*";
    client
        .get()
        .uri(SPRING_OAUTH2_URI)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.FOUND)
        .expectHeader()
        .valueMatches("location", expectedLocation);
  }

  @Test
  void shouldRedirectToTenantRealmWhenTenantQueryParamSet() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(
                new SavedRequestWebFilter(SPRING_SECURITY_SAVED_REQUEST_TENANT),
                oAuth2AuthorizationRequestRedirectWebFilter)
            .build();

    // verify redirect to tenant realm
    String expectedLocation =
        "http://localhost:"
            + mockServer.port()
            + "/auth/realms/Tenant1/protocol/openid-connect/auth.*";
    client
        .get()
        .uri(SPRING_OAUTH2_URI)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.FOUND)
        .expectHeader()
        .valueMatches("location", expectedLocation);

    // verify client registration is stored for tenant realm
    assertNotNull(clientRegistrationRepository.findByRegistrationId("keycloak-Tenant1").block());
  }

  @Test
  void shouldReturnErrorWhenTenantDoesNotExist() {
    WebHandler handler = exchange -> Mono.empty();
    WebTestClient client =
        WebTestClient.bindToWebHandler(handler)
            .webFilter(
                new SavedRequestWebFilter(SPRING_SECURITY_SAVED_REQUEST_TENANT2),
                oAuth2AuthorizationRequestRedirectWebFilter)
            .build();

    client
        .get()
        .uri(SPRING_OAUTH2_URI)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // WebFilter to save to original request that resulted in initiation of oauth2 authorization grant
  // flow
  class SavedRequestWebFilter implements WebFilter {

    private final String originalUri;

    SavedRequestWebFilter(String oringinalUri) {
      this.originalUri = oringinalUri;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
      return serverWebExchange
          .getSession()
          .doOnNext(
              webSession ->
                  webSession.getAttributes().put("SPRING_SECURITY_SAVED_REQUEST", originalUri))
          .then(webFilterChain.filter(serverWebExchange));
    }
  }

  class InjectTimeoutWebFilter implements WebFilter {

    public boolean machineClient;

    InjectTimeoutWebFilter(boolean machineClient) {
      this.machineClient = machineClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
      return serverWebExchange
          .getSession()
          .doOnNext(
              webSession -> {
                if (this.machineClient) {
                  webSession.getAttributes().put("MACHINE_CLIENT", true);
                }
                webSession.getAttributes().put("lastRequestTime", Instant.now().minusSeconds(100));
              })
          .then(webFilterChain.filter(serverWebExchange));
    }
  }
}
