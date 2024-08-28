/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import com.ericsson.gateway.BasicSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@ActiveProfiles("override-security")
class RenameDefaultGenerationentryPointsTest extends BasicSpringBootTest {

  @Autowired private WebTestClient client;

  @Test
  void checkLogindisabled() {
    client.get().uri("/login").exchange().expectStatus().is4xxClientError();
  }
}
