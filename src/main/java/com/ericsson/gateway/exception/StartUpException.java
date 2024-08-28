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

public class StartUpException extends ApiGatewayException {

  private static final long serialVersionUID = 1L;
  protected static final String USER_MESSAGE = "Start Up exception";
  protected static final String INTERNAL_ERROR_CODE = "STARTUP_EXCEPTION";

  public StartUpException(String developerMessage) {
    super(USER_MESSAGE, INTERNAL_ERROR_CODE, developerMessage);
  }
}
