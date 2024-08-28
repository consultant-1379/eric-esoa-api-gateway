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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ericsson.gateway.BasicSpringBootTest;
import com.ericsson.gateway.security.authentication.tenant.InMemoryModifiableReactiveClientRegistrationRepository;
import com.ericsson.gateway.util.StubbedAuthorizationServerRequests;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class LoginControllerTest extends BasicSpringBootTest {

  @Autowired private WebTestClient client;

  @Autowired private InMemoryModifiableReactiveClientRegistrationRepository clientRegistrationRepo;

  @Autowired private StubbedAuthorizationServerRequests stubbedRequests;

  @Autowired private DefaultReactiveOAuth2UserService userService;

  private final OAuth2User user =
      new DefaultOAuth2User(
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
          Collections.singletonMap("sub", "sub1"),
          "sub");

  @Test
  void shouldReturn200ResponseWhenLoginSuccessful() {

    when(userService.loadUser(any())).thenReturn(Mono.just(user));

    stubbedRequests.stubGetProtectionPasswordApiToken();

    client
        .post()
        .uri("/auth/v1")
        .header("X-Login", "user")
        .header("X-password", "password")
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .value(not(isEmptyOrNullString()));
  }

  @Test
  void shouldReturn400ResponseWhenLoginFaild() {

    when(userService.loadUser(any())).thenReturn(Mono.just(user));

    stubbedRequests.stubGetProtectionPasswordApiTokenError(401);

    client
        .post()
        .uri("/auth/v1")
        .header("X-Login", "user")
        .header("X-password", "password")
        .exchange()
        .expectStatus()
        .is4xxClientError()
        .expectBody(String.class)
        .value(not(isEmptyOrNullString()));
  }

  @Test
  void shouldReturn200ResponseWhenTenantLoginSuccessful() {

    when(userService.loadUser(any())).thenReturn(Mono.just(user));

    stubbedRequests.stubGetProtectionPasswordApiToken();

    client
        .post()
        .uri("/auth/v1")
        .header("X-Login", "user")
        .header("X-password", "password")
        .header("X-Tenant", "Tenant1")
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .value(not(isEmptyOrNullString()));

    // verify client registration is stored for tenant realm
    Assertions.assertNotNull(
        clientRegistrationRepo.findByRegistrationId("keycloak-Tenant1").block());
  }

  @Test
  void shouldReturnNotFoundResponseWhenTenantDoesNotExist() {

    when(userService.loadUser(any())).thenReturn(Mono.just(user));

    client
        .post()
        .uri("/auth/v1")
        .header("X-Login", "user")
        .header("X-password", "password")
        .header("X-Tenant", "unknownTenant")
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldReturn500ResponseWhenErrorObtainingAccessToken() {

    client
        .post()
        .uri("/auth/v1")
        .header("X-Login", "user")
        .header("X-password", "password")
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }
}
