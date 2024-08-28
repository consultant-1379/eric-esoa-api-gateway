/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authorization;

import com.ericsson.gateway.security.authorization.client.ProtectedResource;
import com.ericsson.gateway.security.authorization.client.ProtectionAPI;
import com.ericsson.gateway.security.authorization.client.TokenAPI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

/**
 * Reactive time-to-live cache of all protected resources configured in the Authorization server.
 * The cache is refreshed at an interval defined by the property
 * keycloak.authorization.resources.refresh-interval.
 */
@Component
public class ProtectedResourcesTtlCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedResourcesTtlCache.class);

  private final TokenAPI tokenAPI;
  private final ProtectionAPI protectionAPI;
  private final ReactiveClientRegistrationRepository clientRegistrationRepo;
  private final Flux<ProtectedResource> protectedResources;

  public ProtectedResourcesTtlCache(
      final ReactiveClientRegistrationRepository clientRegistrationRepo,
      final TokenAPI tokenAPI,
      final ProtectionAPI protectionAPI,
      @Value("${keycloak.authorization.resources.refresh-interval}") final long refreshInterval) {
    this.tokenAPI = tokenAPI;
    this.protectionAPI = protectionAPI;
    this.clientRegistrationRepo = clientRegistrationRepo;

    this.protectedResources =
        Flux.defer(this::doFindAll)
            .doOnError(e -> LOGGER.error("Protected resources read failed: ", e))
            .retryWhen(
                Retry.max(3)
                    .doBeforeRetry(
                        retrySignal -> LOGGER.info("Retrying: {}", retrySignal.totalRetries())))
            .cache(Duration.ofMillis(refreshInterval));
  }

  private Flux<ProtectedResource> doFindAll() {
    return clientRegistrationRepo
        .findByRegistrationId("keycloak")
        .flatMap(
            clientRegistration ->
                tokenAPI
                    .getProtectionApiToken(clientRegistration)
                    .zipWhen(
                        tokenResponse ->
                            protectionAPI
                                .getAllResourceIds(tokenResponse.getAccessToken())
                                .flatMapMany(Flux::fromIterable)
                                .flatMap(
                                    id ->
                                        protectionAPI.getResource(
                                            tokenResponse.getAccessToken(), id))
                                .collectList())
                    .map(Tuple2::getT2))
        .flatMapMany(Flux::fromIterable);
  }

  /**
   * Get reactive protected resources cache.
   *
   * @return ProtectedResource Flux
   */
  public Flux<ProtectedResource> readAllProtectedResources() {
    return this.protectedResources;
  }
}
