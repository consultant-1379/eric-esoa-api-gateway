/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.exception;

import com.ericsson.oss.orchestration.so.common.error.factory.ErrorMessageFactory;
import com.ericsson.oss.orchestration.so.common.error.message.ErrorMessage;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class ApiGatewayHttpException extends HttpStatusCodeException {

  @Getter private final ErrorMessage errorMessage;

  public ApiGatewayHttpException(
      HttpStatus statusCode, ApiGatewayErrorCode errorCode, String... errorData) {
    super(statusCode);
    errorMessage = ErrorMessageFactory.buildFrom(errorCode.getCode(), errorData);
  }

  public ApiGatewayHttpException(HttpStatus statusCode, ErrorMessage errorMessage) {
    super(statusCode);
    this.errorMessage = errorMessage;
  }

  public static ApiGatewayHttpException notFound(
      ApiGatewayErrorCode errorCode, String... errorData) {
    throw new ApiGatewayHttpException(HttpStatus.NOT_FOUND, errorCode, errorData);
  }

  public static ApiGatewayHttpException internalServerError(
      ApiGatewayErrorCode errorCode, String... errorData) {
    throw new ApiGatewayHttpException(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, errorData);
  }
}
