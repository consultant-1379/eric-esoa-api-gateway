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

public abstract class ApiGatewayException extends Exception {

  private static final long serialVersionUID = 1L;
  private final String internalErrorCode;
  private final String userMessage;
  private final String developerMessage;

  public ApiGatewayException(
      final String userMessage, final String internalErrorCode, String developerMessage) {
    super(userMessage);
    this.userMessage = userMessage;
    this.internalErrorCode = internalErrorCode;
    this.developerMessage = developerMessage;
  }

  public String getInternalErrorCode() {
    return internalErrorCode;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public String getDeveloperMessage() {
    return developerMessage;
  }
}
