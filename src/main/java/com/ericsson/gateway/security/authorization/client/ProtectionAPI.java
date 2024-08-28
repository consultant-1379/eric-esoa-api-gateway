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

import java.util.List;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Client providing access to the Authorization Servers UMA-compliant Protection API. */
@Component
public class ProtectionAPI {

  private final WebClient webClient;

  public ProtectionAPI(
      final WebClient.Builder webClientBuilder, final OAuth2ClientProperties props) {
    final String issuerUri = props.getProvider().values().iterator().next().getIssuerUri();
    this.webClient = webClientBuilder.baseUrl(issuerUri + "/authz/protection/resource_set").build();
  }

  /**
   * Get the ids of all configured resources.
   *
   * @param pat the protection api token
   * @return Mono containing list of resource ids
   */
  public Mono<List<String>> getAllResourceIds(final OAuth2AccessToken pat) {

    return Mono.defer(
        () ->
            webClient
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(pat.getTokenValue()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {}));
  }

  /**
   * Get the resource identified by the supplied id.
   *
   * @param pat the protection api token
   * @param resourceId the resource id
   * @return ProtectedResource mono
   */
  public Mono<ProtectedResource> getResource(final OAuth2AccessToken pat, final String resourceId) {

    return Mono.defer(
        () ->
            webClient
                .get()
                .uri("/" + resourceId)
                .accept(new MediaType[] {MediaType.APPLICATION_JSON})
                .headers(h -> h.setBearerAuth(pat.getTokenValue()))
                .retrieve()
                .bodyToMono(ProtectedResource.class));
  }
}
