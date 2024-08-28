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
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  public GlobalErrorWebExceptionHandler(
      GlobalErrorAttributes g,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer) {
    super(g, new WebProperties.Resources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(
      final ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }

  private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
    ErrorAttributeOptions options =
        ErrorAttributeOptions.defaults()
            .including(ErrorAttributeOptions.Include.MESSAGE)
            .including(ErrorAttributeOptions.Include.STACK_TRACE);
    final Map<String, Object> errorPropertiesMap = getErrorAttributes(request, options);

    //  LOGGER.error((String)errorPropertiesMap.get("trace"));
    errorPropertiesMap.remove("trace");

    if (getError(request) instanceof OAuth2AuthorizationException) {
      LOGGER.error("OAuth2AuthorizationException occurred > {}", request.requestPath());
      return ServerResponse.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(errorPropertiesMap));
    } else if (getError(request) instanceof NullPointerException) {
      LOGGER.error("NullPointerException occurred > {}", request.requestPath());
      return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(errorPropertiesMap));
    } else if (getError(request) instanceof IllegalArgumentException) {
      LOGGER.error("IllegalArgumentException occurred > {}", request.requestPath());
      return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(errorPropertiesMap));
    } else if (getError(request) instanceof WebClientResponseException) {
      LOGGER.error("WebClientResponseException occurred > {}", request.requestPath());
      return ServerResponse.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(errorPropertiesMap));
    }
    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(errorPropertiesMap));
  }
}
