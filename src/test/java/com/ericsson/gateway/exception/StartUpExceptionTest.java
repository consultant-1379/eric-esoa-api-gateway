/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StartUpExceptionTest {
  private ApiGatewayException exception;

  @Test
  void testException() {
    Assertions.assertThrows(
        StartUpException.class,
        () -> {
          exception = new StartUpException("test exception");
          throw exception;
        });
    Assertions.assertEquals(exception.getInternalErrorCode(), "STARTUP_EXCEPTION");
    Assertions.assertEquals(exception.getUserMessage(), "Start Up exception");
    Assertions.assertEquals(exception.getDeveloperMessage(), "test exception");
  }
}
