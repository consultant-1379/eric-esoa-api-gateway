/*
 * Copyright Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.*;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());
  private HttpStatus errorStatus = HttpStatus.UNAUTHORIZED;
  private String errorMessage = "Authorization Failed! ";

  @Override
  public Map<String, Object> getErrorAttributes(
      ServerRequest request, ErrorAttributeOptions options) {
    Map<String, Object> map = super.getErrorAttributes(request, options);

    if (getError(request) instanceof OAuth2AuthorizationException) {
      map.put("status", HttpStatus.UNAUTHORIZED.value());
      map.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
    } else if (getError(request) instanceof NullPointerException) {
      map.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      map.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (getError(request) instanceof IllegalArgumentException) {
      map.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      map.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    } else if (getError(request) instanceof WebClientResponseException) {
      map.put("status", HttpStatus.UNAUTHORIZED);
      map.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
    } else {
      map.put("status", getErrorStatus());
      map.put("message", getErrorMessage());
    }
    return map;
  }

  public HttpStatus getErrorStatus() {
    return errorStatus;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
