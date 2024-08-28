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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Apply Rate Limit with Throttling approach, according to https://stripe.com/ie/blog/rate-limiters
 */
@Component
public class RateLimit {

  @Autowired private HazelcastInstance hazelcastInstance;

  private static final String RATE_LIMIT_MAP_NAME_HEADER = "X-RateLimit-Map-Name";
  private static final String RATE_LIMIT_MAX_ACCESS_HEADER = "X-RateLimit-Max-Access";
  private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
  private static final String RATE_LIMIT_RESET_TIME_HEADER = "X-RateLimit-Reset-Time";

  /**
   * Apply Rate Limit rule, according to the URL configurations for rate limiting. Set HTTP Status
   * 429 in case of the rate limit is reached
   *
   * @param exchange including session details
   */
  public synchronized ServerWebExchange applyRateLimit(ServerWebExchange exchange) {
    IMap<String, Integer> rateLimitMap = getRateMap(exchange);
    Integer remainingTokens = rateLimitMap.get(RATE_LIMIT_REMAINING_HEADER);

    if (remainingTokens == null) {
      initializeRateMap(exchange, rateLimitMap);
    } else if (remainingTokens.equals(0)) {
      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    } else {
      rateLimitMap.computeIfPresent(
          RATE_LIMIT_REMAINING_HEADER, (key, value) -> remainingTokens - 1);
    }
    return exchange;
  }

  private IMap getRateMap(ServerWebExchange exchange) {
    String mapName = exchange.getRequest().getHeaders().get(RATE_LIMIT_MAP_NAME_HEADER).toString();
    return hazelcastInstance.getMap(mapName);
  }

  private void initializeRateMap(ServerWebExchange exchange, Map<String, Integer> rateLimitMap) {
    int rateLimit =
        Integer.parseInt(
            exchange.getRequest().getHeaders().get(RATE_LIMIT_MAX_ACCESS_HEADER).get(0));
    int rateLimitResetTime =
        Integer.parseInt(
            exchange.getRequest().getHeaders().get(RATE_LIMIT_RESET_TIME_HEADER).get(0));
    int initialRemainingTokens = rateLimit;

    rateLimitMap.put(RATE_LIMIT_MAX_ACCESS_HEADER, rateLimit);
    ((IMap) rateLimitMap)
        .put(
            RATE_LIMIT_REMAINING_HEADER,
            initialRemainingTokens - 1,
            rateLimitResetTime,
            TimeUnit.SECONDS);
  }
}
