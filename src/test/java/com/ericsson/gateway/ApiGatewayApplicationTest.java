/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway;

import static org.junit.Assert.assertEquals;

import com.ericsson.gateway.provider.KeycloakProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiGatewayApplicationTest extends BasicSpringBootTest {

  @Autowired KeycloakProperties keycloakProp;

  @Test
  void checkIfKeyCloakBeanIsGenerated() {
    assertEquals("eo", keycloakProp.getClientId());
    assertEquals("secret", keycloakProp.getClientSecret());
    assertEquals("keycloak", keycloakProp.getProviderName());
    assertEquals("sub", keycloakProp.getAttributeKey());
  }
}
