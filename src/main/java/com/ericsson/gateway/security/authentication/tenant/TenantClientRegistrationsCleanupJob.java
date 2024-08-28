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

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.oss.orchestration.so.common.error.factory.ErrorMessageFactory;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.client.RestTemplate;

/**
 * Cron job that runs once per day to remove tenant client registrations for realms which have been
 * deleted.
 */
@Configuration
@EnableScheduling
public class TenantClientRegistrationsCleanupJob {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TenantClientRegistrationsCleanupJob.class);

  @Autowired private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Autowired private RestTemplate restTemplate;

  @Scheduled(cron = "0 0 23 * * ?")
  public void removeClientRegistrationsForDeletedRealms() {
    LOGGER.info("Performing client registration cleanup");
    try {
      final Iterator<ClientRegistration> clientRegistrationsIt =
          ((Iterable) clientRegistrationRepository).iterator();
      while (clientRegistrationsIt.hasNext()) {
        final ClientRegistration clientRegistration = clientRegistrationsIt.next();
        final String clientIssuerUrl =
            (String)
                clientRegistration.getProviderDetails().getConfigurationMetadata().get("issuer");
        final ResponseEntity<String> response =
            restTemplate.getForEntity(clientIssuerUrl, String.class);
        if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
          LOGGER.info(
              "Realm not found, removing client registration '{}'",
              clientRegistration.getRegistrationId());
          ((InMemoryModifiableReactiveClientRegistrationRepository) clientRegistrationRepository)
              .removeClientRegistration(clientRegistration)
              .block();
        }
      }
      LOGGER.info("Finished client registration cleanup");
    } catch (final Exception e) {
      LOGGER.warn(
          "Error during client registration cleanup",
          ErrorMessageFactory.buildUserMessage(
              ApiGatewayErrorCode.CLIENT_REGISTRATION_CLEANUP.getCode()),
          e.getMessage());
    }
  }
}
