/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.ericsson.gateway.repository.EORouteDefinitionRepository;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javassist.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class DynamicRouteServiceImplTest {

  private static List<RouteDefinition> routeList;

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Mock private EORouteDefinitionRepository eoRouteDefinitionRepository;

  @InjectMocks private DynamicRouteServiceImpl dynamicRouteService;

  @BeforeEach
  void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    dynamicRouteService.setApplicationEventPublisher(applicationEventPublisher);
    routeList = buildSampleRoutes();
  }

  @Test
  void getExistingRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.getRouteById(anyString())).willReturn(routeDef1);
    RouteDefinition definition = dynamicRouteService.getRoute("1");
    then(routeDef1).isEqualTo(definition);
    BDDMockito.then(eoRouteDefinitionRepository).should().getRouteById("1");
  }

  @Test
  void getNonExistingRouteThrowsExceptionTest() throws Exception {
    given(eoRouteDefinitionRepository.getRouteById(anyString())).willThrow(NotFoundException.class);
    thenThrownBy(() -> dynamicRouteService.getRoute("1")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void getCurrentRoutesTest() throws Exception {
    given(eoRouteDefinitionRepository.getRouteDefinitions())
        .willReturn(Flux.fromIterable(routeList));
    List<RouteDefinition> currentRoutes = dynamicRouteService.getCurrentRoutes();
    then(routeList).isEqualTo(currentRoutes);
    BDDMockito.then(eoRouteDefinitionRepository).should().getRouteDefinitions();
  }

  @Test
  void createRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.save(any()))
        .willReturn(createRouteResponse(Mono.just(routeDef1)));

    boolean shouldBeTrue = dynamicRouteService.createRoute(routeDef1);
    then(shouldBeTrue).isEqualTo(true);
  }

  @Test
  void createExistingRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.save(any())).willThrow(new RuntimeException());

    boolean shouldBeFalse = dynamicRouteService.createRoute(routeDef1);
    then(shouldBeFalse).isEqualTo(false);
  }

  @Test
  void updateRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.delete(any())).willReturn(createRouteResponse(Mono.empty()));
    given(eoRouteDefinitionRepository.save(any())).willReturn(createRouteResponse(Mono.empty()));

    Mono<Boolean> updateRoute = dynamicRouteService.updateRoute(routeDef1);
    StepVerifier.create(updateRoute).assertNext(Assertions::assertTrue).expectComplete().verify();
  }

  @Test
  void updateNonExistingRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.delete(any())).willReturn(createRouteResponse(Mono.empty()));
    given(eoRouteDefinitionRepository.save(any())).willThrow(new RuntimeException());

    Mono<Boolean> updateRoute = dynamicRouteService.updateRoute(routeDef1);
    StepVerifier.create(updateRoute).assertNext(Assertions::assertFalse).expectComplete().verify();
  }

  @Test
  void deleteRouteTest() throws Exception {
    RouteDefinition routeDef1 = routeDef(1);
    given(eoRouteDefinitionRepository.delete(any())).willReturn(createRouteResponse(Mono.empty()));

    Mono<ResponseEntity<Object>> deleteResponseEntity = dynamicRouteService.deleteRoute("1");
    Assertions.assertEquals(
        HttpStatus.NO_CONTENT,
        Objects.requireNonNull(deleteResponseEntity.block()).getStatusCode());
  }

  @Test
  void deleteNonExistingRouteTest() throws Exception {
    given(eoRouteDefinitionRepository.delete(any())).willReturn(Mono.error(new Exception()));

    Mono<ResponseEntity<Object>> deleteResponseEntity = dynamicRouteService.deleteRoute("1");
    Assertions.assertEquals(
        HttpStatus.NOT_FOUND, Objects.requireNonNull(deleteResponseEntity.block()).getStatusCode());
  }

  static RouteDefinition routeDef(int id) {
    RouteDefinition def = new RouteDefinition();
    def.setId(String.valueOf(id));
    def.setUri(URI.create("http://localhost/" + id));
    def.setOrder(id);
    return def;
  }

  private static List<RouteDefinition> buildSampleRoutes() throws IOException {
    RouteDefinition routeDef1 = routeDef(1);
    RouteDefinition routeDef2 = routeDef(2);
    RouteDefinition routeDef3 = routeDef(3);

    List<RouteDefinition> routeList = new ArrayList<>();
    routeList.add(routeDef1);
    routeList.add(routeDef2);
    routeList.add(routeDef3);

    return routeList;
  }

  private Mono<Void> createRouteResponse(Mono<RouteDefinition> route) {
    return route.flatMap(
        r -> {
          return Mono.empty();
        });
  }
}
