/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import com.ericsson.gateway.BasicSpringBootTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.ForwardPathFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class AccessControlTest extends BasicSpringBootTest {

  public static final String ECM_SERVICE_PATH_VALUE = "http://test:0000";

  @Autowired private WebTestClient client;

  @MockBean private RouteToRequestUrlFilter routeToRequestUrlFilter;

  @MockBean private ForwardPathFilter forwardPathFilter;

  @BeforeAll
  public static void setRouteVariables() {
    System.setProperty("ECM_SERVICE_PATH", ECM_SERVICE_PATH_VALUE);
  }

  @Test
  void authorizationNotRequired() {
    ArgumentCaptor<ServerWebExchange> arg = ArgumentCaptor.forClass(ServerWebExchange.class);
    when(routeToRequestUrlFilter.filter(arg.capture(), any())).thenReturn(Mono.empty());
    when(forwardPathFilter.filter(any(), any())).thenReturn(Mono.empty());

    client.get().uri("/ecm_service/vimzones").exchange().expectStatus().isOk();

    Route route = arg.getValue().getAttribute(GATEWAY_ROUTE_ATTR);
    assertNotNull(route);
    assertEquals(ECM_SERVICE_PATH_VALUE, route.getUri().toString());
  }

  @Test
  void authorizationRequiredRequestShouldBeRedirected() {
    client
        .get()
        .uri("/dmaap-mr")
        .exchange()
        .expectStatus()
        .is3xxRedirection()
        .expectHeader()
        .valueMatches(HttpHeaders.LOCATION, "/login");

    verify(forwardPathFilter, never()).filter(any(), any());
    verify(routeToRequestUrlFilter, never()).filter(any(), any());
  }
}
