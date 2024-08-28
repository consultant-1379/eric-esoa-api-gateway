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

import java.util.Iterator;
import java.util.Map;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In memory <code>ReactiveClientRegistrationRepository</code> which supports adding and removing
 * clients during runtime. Used in place of provided <code>
 * InMemoryReactiveClientRegistrationRepository</code> which does not support runtime updates to
 * client registrations. As with <code>InMemoryReactiveClientRegistrationRepository</code> static
 * client registrations configured via a security property <code>spring.security.oauth2.client
 * </code> are added on initialization.
 */
@Component
public class InMemoryModifiableReactiveClientRegistrationRepository
    implements ReactiveClientRegistrationRepository, Iterable<ClientRegistration> {

  private final Map<String, ClientRegistration> registrations;

  public InMemoryModifiableReactiveClientRegistrationRepository(
      OAuth2ClientProperties oauth2ClientProperties) {
    registrations =
        OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(oauth2ClientProperties);
  }

  @Override
  public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
    return Mono.justOrEmpty(registrations.get(registrationId));
  }

  /**
   * Add client registration to the repository. Overwrites registration is already existing with the
   * same id.
   *
   * @param clientRegistration the client registration
   * @return Void
   */
  public Mono<Void> addClientRegistration(final ClientRegistration clientRegistration) {
    return Mono.justOrEmpty(
            registrations.put(clientRegistration.getRegistrationId(), clientRegistration))
        .then();
  }

  /**
   * Remove client registration from the repository.
   *
   * @param clientRegistration the client registration
   * @return Void
   */
  public Mono<Void> removeClientRegistration(final ClientRegistration clientRegistration) {
    return Mono.justOrEmpty(registrations.remove(clientRegistration.getRegistrationId())).then();
  }

  public Iterator<ClientRegistration> iterator() {
    return this.registrations.values().iterator();
  }
}
