/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.rate.limit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ericsson.gateway.config.HazelcastConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HazelcastConfig.class, RateLimit.class})
class RateLimitTest {

  @Autowired private RateLimit rateLimit;

  @Test
  void rateLimitShouldReturn429WhenRemainingTokensIsZero() {
    // given
    String mapName = "rate-limiter";
    int resetTime = 120;
    int maxAccess = 1;
    List<HttpStatus> responses = new ArrayList<>();
    // when
    for (int i = 0; i < 2; i++) {
      ServerWebExchange exchange = prepareRequest(mapName, resetTime, maxAccess);
      responses.add(exchange.getResponse().getStatusCode());
    }
    // then
    assertThat(responses.get(0)).isNull();
    assertThat(responses.get(1)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void rateLimitShouldNotReturn429AfterResetTime() throws InterruptedException {
    // given
    String mapName = "rate-limiter";
    int resetTime = 1;
    int maxAccess = 1;
    List<HttpStatus> responses = new ArrayList<>();
    // when
    for (int i = 0; i < 2; i++) {
      ServerWebExchange exchange = prepareRequest(mapName, resetTime, maxAccess);
      responses.add(exchange.getResponse().getStatusCode());
      TimeUnit.SECONDS.sleep(resetTime);
    }
    // then
    assertThat(responses.get(0)).isNull();
    assertThat(responses.get(1)).isNull();
  }

  private ServerWebExchange prepareRequest(String mapName, int resetTime, int maxAccess) {

    MockServerHttpRequest request =
        MockServerHttpRequest.method(HttpMethod.POST, "/dummyurl").build();

    request
        .mutate()
        .header("X-RateLimit-Map-Name", mapName)
        .header("X-RateLimit-Reset-Time", Integer.toString(resetTime))
        .header("X-RateLimit-Max-Access", Integer.toString(maxAccess));

    ServerWebExchange exchange = MockServerWebExchange.from(request);

    rateLimit.applyRateLimit(exchange);

    return exchange;
  }
}
