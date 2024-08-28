/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ericsson.gateway.BasicSpringBootTest;
import com.ericsson.gateway.service.DynamicRouteServiceImpl;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(
    properties = {
      "ericsson.gateway.security.allowed-paths[0]=/v1/routes/**",
      "spring.cloud.gateway.routes[0].id=legacy-service",
      "spring.cloud.gateway.routes[0].uri=http://localhost:32167",
      "spring.cloud.gateway.routes[0].predicates[0].name=Path",
      "spring.cloud.gateway.routes[0].predicates[0].args[pattern]=/connect/delay/{timeout}",
      "spring.cloud.gateway.routes[0].metadata[connect-timeout]=5"
    })
class RouteControllerTest extends BasicSpringBootTest {

  @Autowired private WebTestClient client;

  @Autowired private DynamicRouteServiceImpl dynamicRouteService;

  @BeforeEach
  public void before() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("test-service");
    testRouteDefinition.setUri(URI.create("http://test-service.org"));

    FilterDefinition prefixPathFilterDefinition = new FilterDefinition("PrefixPath=/test-path");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    dynamicRouteService.createRoute(testRouteDefinition);
  }

  @Test
  void testPostValidRouteDefinition() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("test-post-valid-route-definition");
    testRouteDefinition.setUri(URI.create("http://example.org"));

    FilterDefinition prefixPathFilterDefinition = new FilterDefinition("PrefixPath=/test-path");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    client
        .post()
        .uri("v1/routes")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject(testRouteDefinition))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBodyList(Map.class)
        .consumeWith(
            result -> {
              List<Map> responseBody = result.getResponseBody();
              assertThat(responseBody).isNotNull();
              assertThat(responseBody.size()).isEqualTo(1);
              assertThat(responseBody).isNotEmpty();
            });
  }

  @Test
  void testPostValidRouteDefinitionThatAlreadyExists() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("test-service");
    testRouteDefinition.setUri(URI.create("http://test-service.org"));

    FilterDefinition prefixPathFilterDefinition = new FilterDefinition("PrefixPath=/test-path");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    client
        .post()
        .uri("v1/routes")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject(testRouteDefinition))
        .exchange()
        .expectStatus()
        .isEqualTo((HttpStatus.CONFLICT));
  }

  @Test
  void testPostValidRouteDefinitionThatIsLegacy() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("legacy-service");
    testRouteDefinition.setUri(URI.create("http://test-service.org"));

    FilterDefinition prefixPathFilterDefinition = new FilterDefinition("PrefixPath=/test-path");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    client
        .post()
        .uri("v1/routes")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject(testRouteDefinition))
        .exchange()
        .expectStatus()
        .isEqualTo((HttpStatus.CONFLICT));
  }

  @Test
  void testPutValidRouteDefinition() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("test-service");
    testRouteDefinition.setUri(URI.create("http://example.org"));

    FilterDefinition prefixPathFilterDefinition =
        new FilterDefinition("PrefixPath=/test-path-updated");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    client
        .put()
        .uri("v1/routes")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject(testRouteDefinition))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Map.class)
        .consumeWith(
            result -> {
              List<Map> responseBody = result.getResponseBody();
              assertThat(responseBody).isNotNull();
              assertThat(responseBody.size()).isEqualTo(1);
              assertThat(responseBody).isNotEmpty();
            });
  }

  @Test
  void testPutValidRouteDefinitionNotExisting() {

    RouteDefinition testRouteDefinition = new RouteDefinition();
    testRouteDefinition.setId("test-put-valid-route-definition-not-existing");
    testRouteDefinition.setUri(URI.create("http://example.org"));

    FilterDefinition prefixPathFilterDefinition =
        new FilterDefinition("PrefixPath=/test-path-updated");
    FilterDefinition redirectToFilterDefinition =
        new FilterDefinition("RemoveResponseHeader=Sensitive-Header");
    testRouteDefinition.setFilters(
        Arrays.asList(prefixPathFilterDefinition, redirectToFilterDefinition));

    PredicateDefinition hostRoutePredicateDefinition = new PredicateDefinition("Host=myhost.org");
    PredicateDefinition methodRoutePredicateDefinition = new PredicateDefinition("Method=GET");
    testRouteDefinition.setPredicates(
        Arrays.asList(hostRoutePredicateDefinition, methodRoutePredicateDefinition));

    client
        .put()
        .uri("v1/routes")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject(testRouteDefinition))
        .exchange()
        .expectStatus()
        .isNotModified();
  }

  @Test
  public void testGetRoutes() {
    client
        .get()
        .uri("v1/routes")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Map.class)
        .consumeWith(
            result -> {
              List<Map> responseBody = result.getResponseBody();
              assertThat(responseBody).isNotEmpty();
            });
  }

  @Test
  void testGetSpecificRoute() {
    client
        .get()
        .uri("v1/routes/test-service")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Map.class)
        .consumeWith(
            result -> {
              List<Map> responseBody = result.getResponseBody();
              assertThat(responseBody).isNotNull();
              assertThat(responseBody.size()).isEqualTo(1);
              assertThat(responseBody).isNotEmpty();
            });
  }

  @Test
  void testGetSpecificRouteNotExisting() {
    client
        .get()
        .uri("v1/routes/test-get-specific-route-not-existing")
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void testDeleteSpecificRoute() {
    client.delete().uri("v1/routes/test-service").exchange().expectStatus().isNoContent();
  }

  @Test
  void testDeleteSpecificRouteNotExisting() {
    client
        .delete()
        .uri("v1/routes/test-delete-specific-route-not-existing")
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
