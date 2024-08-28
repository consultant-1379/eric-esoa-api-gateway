/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.config;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.ericsson.gateway.security.authentication.tenant.InMemoryModifiableReactiveClientRegistrationRepository;
import com.ericsson.gateway.util.StubbedAuthorizationServerRequests;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Optional;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;

@TestConfiguration
public class WireMockIntegrationTestConfig {

  @Bean(destroyMethod = "stop")
  public WireMockServer wireMock() {
    WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    int port = wireMockServer.port();
    wireMockServer
        .listAllStubMappings()
        .getMappings()
        .forEach(
            stubMapping -> {
              String response = stubMapping.getResponse().getBody().replace("{port}", port + "");
              stubMapping.getResponse().setBody(response);
            });
    return wireMockServer;
  }

  @Bean
  @Primary
  public ReactiveClientRegistrationRepository clientRegistrationRepository(
      OAuth2ClientProperties properties, WireMockServer mockServer) {
    Optional.ofNullable(properties.getProvider())
        .map(item -> item.get("keycloak"))
        .ifPresent(
            provider -> {
              String issueUri = provider.getIssuerUri();
              String tockenUri = provider.getTokenUri();
              issueUri = String.format(issueUri, mockServer.port());
              tockenUri = String.format(tockenUri, mockServer.port());
              provider.setIssuerUri(issueUri);
              provider.setTokenUri(tockenUri);
            });
    return new InMemoryModifiableReactiveClientRegistrationRepository(properties);
  }

  @Bean
  @Lazy
  public StubbedAuthorizationServerRequests stubbedRequests(WireMockServer mockServer) {
    return new StubbedAuthorizationServerRequests(mockServer);
  }

  @MockBean @Lazy private DefaultReactiveOAuth2UserService userService;
}
