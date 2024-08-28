/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.api.contract;

import static io.restassured.RestAssured.DEFAULT_URI;

import com.ericsson.gateway.ApiGatewayApplication;
import com.ericsson.gateway.config.WireMockIntegrationTestConfig;
import com.ericsson.gateway.security.authorization.AuthorizationTest;
import com.ericsson.gateway.service.DynamicRouteServiceImpl;
import io.restassured.RestAssured;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

@SpringBootTest(
    classes = {
      ApiGatewayApplication.class,
      AuthorizationTest.AuthorizationTestConfiguration.class,
      WebClientAutoConfiguration.class,
      WireMockIntegrationTestConfig.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.port=0", "ericsson.gateway.security.allowed-paths[0]=/v1/routes/**"})
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
public abstract class ContractVerifierBase {

  @LocalServerPort int port;

  @MockBean private DynamicRouteServiceImpl dynamicRouteService;

  @BeforeEach
  public void setup() throws IOException, URISyntaxException, NotFoundException {

    RouteDefinition definition = prepareRouteDefinition("example-route");
    RouteDefinition definition1 = prepareRouteDefinition("example-route-2");

    Mockito.when(dynamicRouteService.getRoute("example-route")).thenReturn(definition);
    Mockito.when(dynamicRouteService.getRoute("does-not-exist")).thenThrow(NotFoundException.class);
    Mockito.when(dynamicRouteService.deleteRoute("example-route"))
        .thenReturn(Mono.just(ResponseEntity.noContent().build()));
    Mockito.when(dynamicRouteService.deleteRoute("does-not-exist"))
        .thenReturn(Mono.just(ResponseEntity.notFound().build()));
    Mockito.when(dynamicRouteService.createRoute(definition)).thenReturn(true);
    Mockito.when(dynamicRouteService.updateRoute(definition)).thenReturn(Mono.just(true));
    Mockito.when(dynamicRouteService.getCurrentRoutes())
        .thenReturn(Arrays.asList(definition, definition1));

    RestAssured.baseURI = String.format("%s:%s", DEFAULT_URI, port);
  }

  private RouteDefinition prepareRouteDefinition(String id) throws URISyntaxException {
    RouteDefinition definition = new RouteDefinition();
    definition.setId(id);
    definition.setUri(new URI("http://test-gw-client-mychart"));

    List<FilterDefinition> filters =
        Arrays.asList(
            new FilterDefinition("AddRequestHeader=X-RateLimit-Map-Name, so-rate-limiter"),
            new FilterDefinition("AddRequestHeader=X-RateLimit-Max-Access, 180"),
            new FilterDefinition("AddRequestHeader=X-RateLimit-Reset-Time, 4"));

    List<PredicateDefinition> predicates =
        Arrays.asList(
            new PredicateDefinition("Host=pf.ceo2.hahn149.rnd.gicc.ericsson.se"),
            new PredicateDefinition("Path=/nginx/**"));
    definition.setFilters(filters);
    definition.setPredicates(predicates);

    return definition;
  }
}
