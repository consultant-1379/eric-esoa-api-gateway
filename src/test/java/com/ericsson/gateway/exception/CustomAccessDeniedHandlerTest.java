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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
public class CustomAccessDeniedHandlerTest {
  private AccessDeniedException accessDeniedException;
  private MockServerWebExchange exchange;
  @InjectMocks private CustomAccessDeniedHandler customAccessDeniedHandler;

  @BeforeEach
  void setup() {
    exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer accessToken"));
  }

  @Test
  void testCustomAccessDeniedException() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> {
          accessDeniedException = new AccessDeniedException("test exception");
          throw accessDeniedException;
        });
    Assertions.assertEquals(accessDeniedException.getMessage(), "test exception");
    Assertions.assertNotNull(accessDeniedException);

    accessDeniedException = new AccessDeniedException("test exception");
    Mono<Void> res =
        customAccessDeniedHandler
            .handle(exchange, accessDeniedException)
            .doOnNext(result -> assertNotNull(result));
  }
}
