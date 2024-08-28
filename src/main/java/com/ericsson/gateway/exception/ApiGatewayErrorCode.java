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

import lombok.Getter;

public enum ApiGatewayErrorCode {
  TENANT_NOT_FOUND("APIGW-L-01"),
  LOGOUT_FAILED("APIGW-L-02"),
  ISSUE_CREATING_ROUTE_DEFINITION("APIGW-R-01"),
  ISSUE_PROCESSING_JSON_ROUTE_DEFINITION("APIGW-R-02"),
  ISSUE_UPDATING_ROUTE_DEFINITION("APIGW-R-03"),
  ROUTE_NOT_FOUND("APIGW-R-04"),
  ISSUE_RETRIEVING_ROUTE_DEFINITION("APIGW-R-05"),
  FAILED_TO_PERSIST("APIGW-R-06"),
  ROUTE_DEFINITION_NOT_FOUND("APIGW-R-07"),
  STARTUP_EXCEPTION_LOGIN("APIGW-S-01"),
  STARTUP_EXCEPTION_LOGOUT("APIGW-S-02"),
  CLIENT_REGISTRATION_CLEANUP("APIGW-C-01");

  @Getter private final String code;

  ApiGatewayErrorCode(String code) {
    this.code = code;
  }
}
