/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authentication.tenant;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TimeoutMarkerFilter implements WebFilter {

  private static final String LAST_REQUEST_TIME = "lastRequestTime";
  private static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

  @Value("${session.timeout}")
  int sessionTimeout;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return exchange
        .getSession()
        .flatMap(
            webSession -> {
              if (null != webSession
                  && null != webSession.getAttributes()
                  && webSession.getAttributes().containsKey(LAST_REQUEST_TIME)) {
                if (isExpired(
                    (Instant) webSession.getAttributes().get(LAST_REQUEST_TIME), sessionTimeout)) {
                  webSession.getAttributes().remove(SPRING_SECURITY_CONTEXT);
                }
              }
              if (null != webSession.getAttributes()) {
                webSession.getAttributes().put(LAST_REQUEST_TIME, Instant.now());
              }
              return chain.filter(exchange).then(Mono.empty());
            });
  }

  private boolean isExpired(Instant time, int timeOutSeconds) {
    return Instant.now().minusSeconds(time.getEpochSecond()).getEpochSecond() - timeOutSeconds > 0;
  }
}
