/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authentication.tenant;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CustomRedirectStrategy implements ServerRedirectStrategy {
  private HttpStatus httpStatus = HttpStatus.FOUND;

  private boolean contextRelative = true;

  public Mono<Void> sendRedirect(ServerWebExchange exchange, URI location) {
    Assert.notNull(exchange, "exchange cannot be null");
    Assert.notNull(location, "location cannot be null");

    return exchange
        .getSession()
        .flatMap(
            webSession -> {
              ServerHttpResponse response = exchange.getResponse();
              if (null != webSession && null != webSession.getAttribute("MACHINE_CLIENT")) {
                if ((boolean) webSession.getAttribute("MACHINE_CLIENT")) {
                  response.setStatusCode(HttpStatus.UNAUTHORIZED);
                  return Mono.empty();
                }
              }
              response.setStatusCode(this.httpStatus);
              response.getHeaders().setLocation(createLocation(exchange, location));
              return Mono.empty();
            });
  }

  private URI createLocation(ServerWebExchange exchange, URI location) {
    if (!this.contextRelative) {
      return location;
    }
    String url = location.toASCIIString();
    if (url.startsWith("/")) {
      String context = exchange.getRequest().getPath().contextPath().value();
      return URI.create(context + url);
    }
    return location;
  }

  /**
   * The {@link HttpStatus} to use for the redirect.
   *
   * @param httpStatus the status to use. Cannot be null
   */
  public void setHttpStatus(HttpStatus httpStatus) {
    Assert.notNull(httpStatus, "httpStatus cannot be null");
    this.httpStatus = httpStatus;
  }

  /**
   * Sets if the location is relative to the context.
   *
   * @param contextRelative if redirects should be relative to the context. Default is true.
   */
  public void setContextRelative(boolean contextRelative) {
    this.contextRelative = contextRelative;
  }
}
