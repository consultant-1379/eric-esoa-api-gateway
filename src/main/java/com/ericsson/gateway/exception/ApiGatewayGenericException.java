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

public class ApiGatewayGenericException extends Exception {

  @Getter private final ErrorMessage errorMessage;

  public ApiGatewayGenericException(ErrorMessage errorMessage) {
    this.errorMessage = errorMessage;
  }

  public ApiGatewayGenericException(ApiGatewayErrorCode errorCode, String... errorData) {
    this.errorMessage = ErrorMessageFactory.buildFrom(errorCode.getCode(), errorData);
  }
}
