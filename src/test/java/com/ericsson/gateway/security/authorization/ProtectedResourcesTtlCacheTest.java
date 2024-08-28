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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.ericsson.gateway.security.authorization.client.ProtectedResource;
import com.ericsson.gateway.security.authorization.client.ProtectionAPI;
import com.ericsson.gateway.security.authorization.client.TokenAPI;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProtectedResourcesTtlCacheTest {

  @Mock private TokenAPI tokenAPI;

  @Mock private ProtectionAPI protectionAPI;

  @Mock private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Test
  void shouldReturnCachedResourcesWhenRefreshIntervalNotExpired() {

    final OAuth2AccessTokenResponse tokenResponse =
        OAuth2AccessTokenResponse.withToken("accessToken")
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .refreshToken("refreshToken")
            .build();

    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("keycloak")
            .clientId("myclient")
            .redirectUriTemplate("redirectUriTemplate")
            .authorizationUri("authorizationUri")
            .tokenUri("tokenUri")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .providerConfigurationMetadata(Collections.singletonMap("issuer", "keycloak"))
            .build();

    when(clientRegistrationRepository.findByRegistrationId("keycloak"))
        .thenReturn(Mono.just(clientRegistration));
    doReturn(Mono.just(tokenResponse)).when(tokenAPI).getProtectionApiToken(clientRegistration);
    when(protectionAPI.getAllResourceIds(any()))
        .thenReturn(Mono.just(Collections.singletonList("1")))
        .thenReturn(Mono.just(Arrays.asList("1", "2")))
        .thenReturn(Mono.just(Arrays.asList("1", "2", "3")));
    when(protectionAPI.getResource(any(), any())).thenReturn(Mono.just(new ProtectedResource()));

    final ProtectedResourcesTtlCache resourcesCache =
        new ProtectedResourcesTtlCache(
            clientRegistrationRepository, tokenAPI, protectionAPI, 60000);

    // consecutive calls to cache should return the same initially cached result
    for (int i = 0; i < 10; i++) {
      assertEquals(1, resourcesCache.readAllProtectedResources().collectList().block().size());
    }
  }

  @Test
  void shouldRefreshResourcesWhenRefreshIntervalExpires() {

    final OAuth2AccessTokenResponse tokenResponse =
        OAuth2AccessTokenResponse.withToken("accessToken")
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .refreshToken("refreshToken")
            .build();

    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("keycloak")
            .clientId("myclient")
            .redirectUriTemplate("redirectUriTemplate")
            .authorizationUri("authorizationUri")
            .tokenUri("tokenUri")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .providerConfigurationMetadata(Collections.singletonMap("issuer", "keycloak"))
            .build();

    when(clientRegistrationRepository.findByRegistrationId("keycloak"))
        .thenReturn(Mono.just(clientRegistration));
    doReturn(Mono.just(tokenResponse)).when(tokenAPI).getProtectionApiToken(clientRegistration);
    when(protectionAPI.getAllResourceIds(any()))
        .thenReturn(Mono.just(Collections.singletonList("1")))
        .thenReturn(Mono.just(Arrays.asList("1", "2")))
        .thenReturn(Mono.just(Arrays.asList("1", "2", "3")))
        .thenReturn(Mono.just(Arrays.asList("1", "2", "3", "4")));
    when(protectionAPI.getResource(any(), any())).thenReturn(Mono.just(new ProtectedResource()));

    final ProtectedResourcesTtlCache resourcesCache =
        new ProtectedResourcesTtlCache(clientRegistrationRepository, tokenAPI, protectionAPI, 100);

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> resourcesCache.readAllProtectedResources().collectList().block().size() == 3);
  }

  @Test
  void shouldRetryResourcesWhenErrorThrown() {

    final OAuth2AccessTokenResponse tokenResponse =
        OAuth2AccessTokenResponse.withToken("accessToken")
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .refreshToken("refreshToken")
            .build();

    ClientRegistration clientRegistration =
        ClientRegistration.withRegistrationId("keycloak")
            .clientId("myclient")
            .redirectUriTemplate("redirectUriTemplate")
            .authorizationUri("authorizationUri")
            .tokenUri("tokenUri")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .providerConfigurationMetadata(Collections.singletonMap("issuer", "keycloak"))
            .build();

    when(clientRegistrationRepository.findByRegistrationId("keycloak"))
        .thenReturn(Mono.just(clientRegistration));
    doReturn(Mono.just(tokenResponse)).when(tokenAPI).getProtectionApiToken(clientRegistration);
    when(protectionAPI.getAllResourceIds(any()))
        .thenReturn(Mono.just(Collections.singletonList("1")))
        .thenReturn(Mono.just(Arrays.asList("1", "2")))
        .thenReturn(Mono.just(Arrays.asList("1", "2", "3")));
    when(protectionAPI.getResource(any(), any())).thenReturn(Mono.error(new RuntimeException()));

    final ProtectedResourcesTtlCache resourcesCache =
        new ProtectedResourcesTtlCache(
            clientRegistrationRepository, tokenAPI, protectionAPI, 60000);

    Flux<ProtectedResource> resources = resourcesCache.readAllProtectedResources();

    StepVerifier.create(resources).expectErrorMatches(Exceptions::isRetryExhausted).verify();
  }
}
