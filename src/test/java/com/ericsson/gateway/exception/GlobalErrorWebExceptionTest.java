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
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class GlobalErrorWebExceptionTest {
  private OAuth2AuthorizationException exception;
  private final OAuth2Error error =
      new OAuth2Error(HttpStatus.UNAUTHORIZED.toString(), "errorMessage", "/test");

  @Test
  void testException() {
    Assertions.assertThrows(
        OAuth2AuthorizationException.class,
        () -> {
          exception = new OAuth2AuthorizationException(error);
          throw exception;
        });
    Assertions.assertThrows(
        OAuth2AuthorizationException.class,
        () -> {
          exception = new OAuth2AuthorizationException(error);
          throw exception;
        });
    Assertions.assertEquals(exception.getMessage(), "[401 UNAUTHORIZED] errorMessage");
  }
}
