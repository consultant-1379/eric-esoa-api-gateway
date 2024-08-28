/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.route.filter;

import com.ericsson.gateway.rate.limit.RateLimit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RateLimitFilter implements GatewayFilterFactory<Config> {

  @Autowired private RateLimit rateLimit;

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      AtomicReference<ServerWebExchange> updatedExchange = new AtomicReference<>(exchange);
      exchange
          .getSession()
          .subscribe(session -> updatedExchange.set(rateLimit.applyRateLimit(exchange)));

      HttpStatus responseStatusCode = updatedExchange.get().getResponse().getStatusCode();
      if (responseStatusCode.equals(HttpStatus.TOO_MANY_REQUESTS)) {
        return updatedExchange.get().getResponse().setComplete();
      }

      return chain.filter(updatedExchange.get());
    };
  }

  @Override
  public Config newConfig() {
    return new Config();
  }

  @Override
  public Class<Config> getConfigClass() {
    return Config.class;
  }
}
