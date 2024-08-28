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

import com.ericsson.oss.orchestration.so.common.error.message.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ApiGatewayExceptionHandler {

  @ExceptionHandler(ApiGatewayHttpException.class)
  @ResponseBody
  public ResponseEntity<ErrorMessage> handleApiGatewayException(
      ApiGatewayHttpException apiGatewayHttpException) {
    return new ResponseEntity<>(
        apiGatewayHttpException.getErrorMessage(), apiGatewayHttpException.getStatusCode());
  }

  @ExceptionHandler(ApiGatewayGenericException.class)
  @ResponseBody
  public ResponseEntity<ErrorMessage> handleException(
      ApiGatewayGenericException apiGatewayException) {
    return new ResponseEntity<>(
        apiGatewayException.getErrorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(OAuth2AuthorizationException.class)
  @ResponseBody
  public ResponseEntity<OAuth2Error> handleOAuth2AuthorizationExceptionException(
      OAuth2AuthorizationException oAuth2AuthorizationException) {
    return new ResponseEntity<>(oAuth2AuthorizationException.getError(), HttpStatus.UNAUTHORIZED);
  }
}
