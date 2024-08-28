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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(classes = TenantClientRegistrationsCleanupJob.class)
class TenantClientRegistrationsCleanupJobTest {

  @Autowired private TenantClientRegistrationsCleanupJob tenantClientRegistrationsCleanupJob;

  @MockBean
  private InMemoryModifiableReactiveClientRegistrationRepository clientRegistrationRepository;

  @MockBean private RestTemplate restTemplate;

  @Test
  void shouldRemoveClientRegistrationsForDeletedRealms() {
    ClientRegistration client1 = createClientRegistration("client1");
    ClientRegistration client2 = createClientRegistration("client2");
    ClientRegistration client3 = createClientRegistration("client3");
    ClientRegistration client4 = createClientRegistration("client4");

    final List<ClientRegistration> clientRegistrations =
        Stream.of(client1, client2, client3, client4).collect(Collectors.toList());

    when(clientRegistrationRepository.iterator()).thenReturn(clientRegistrations.iterator());
    when(clientRegistrationRepository.removeClientRegistration(any())).thenReturn(Mono.empty());
    when(restTemplate.getForEntity("client1", String.class))
        .thenReturn(ResponseEntity.ok().build());
    when(restTemplate.getForEntity("client2", String.class))
        .thenReturn(ResponseEntity.notFound().build());
    when(restTemplate.getForEntity("client3", String.class))
        .thenReturn(ResponseEntity.ok().build());
    when(restTemplate.getForEntity("client4", String.class))
        .thenReturn(ResponseEntity.notFound().build());

    tenantClientRegistrationsCleanupJob.removeClientRegistrationsForDeletedRealms();

    Mockito.verify(clientRegistrationRepository, never()).removeClientRegistration(client1);
    Mockito.verify(clientRegistrationRepository).removeClientRegistration(client2);
    Mockito.verify(clientRegistrationRepository, never()).removeClientRegistration(client3);
    Mockito.verify(clientRegistrationRepository).removeClientRegistration(client4);
  }

  private ClientRegistration createClientRegistration(String id) {
    return ClientRegistration.withRegistrationId(id)
        .clientId("myclient")
        .redirectUriTemplate("redirectUriTemplate")
        .authorizationUri("authorizationUri")
        .tokenUri("tokenUri")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .providerConfigurationMetadata(Collections.singletonMap("issuer", id))
        .build();
  }
}
