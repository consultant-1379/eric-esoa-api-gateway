/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.service;

import com.ericsson.gateway.repository.EORouteDefinitionRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DynamicRouteServiceImpl implements ApplicationEventPublisherAware {
  private final Logger logger = LoggerFactory.getLogger(DynamicRouteServiceImpl.class);

  @Autowired private ApplicationEventPublisher publisher;

  @Autowired EORouteDefinitionRepository eoRouteDefinitionRepository;

  private final RouteDefinitionLocator routeDefinitionLocator;

  @Autowired
  public DynamicRouteServiceImpl(
      RouteDefinitionLocator routeDefinitionLocator,
      EORouteDefinitionRepository eoRouteDefinitionRepository) {
    this.routeDefinitionLocator = routeDefinitionLocator;
    this.eoRouteDefinitionRepository = eoRouteDefinitionRepository;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.publisher = applicationEventPublisher;
  }

  /**
   * Create Routing
   *
   * @param definition the route definition
   * @return true if the addition is successful.
   */
  public boolean createRoute(RouteDefinition definition) {
    try {
      eoRouteDefinitionRepository.save(Mono.just(definition)).subscribe();
      this.publisher.publishEvent(new RefreshRoutesEvent(this));
      return true;
    } catch (Exception e) {
      logger.error("Route definition addition Failed: ", e);
    }
    return false;
  }

  /**
   * Update Routing
   *
   * @param definition the route definition
   * @return true if the update is successful.
   */
  public Mono<Boolean> updateRoute(RouteDefinition definition) {
    return eoRouteDefinitionRepository
        .delete(Mono.just(definition.getId()))
        .then(Mono.defer(() -> Mono.just(createRoute(definition))))
        .onErrorResume(Exception.class::isInstance, t -> Mono.just(false));
  }

  /**
   * Delete Routing
   *
   * @param id the route identifier
   * @return responseEntity
   */
  public Mono<ResponseEntity<Object>> deleteRoute(final String id) {
    return eoRouteDefinitionRepository
        .delete(Mono.just(id))
        .then(Mono.defer(() -> Mono.just(ResponseEntity.noContent().build())))
        .onErrorResume(
            Exception.class::isInstance, t -> Mono.just(ResponseEntity.notFound().build()));
  }

  /**
   * Get Routing
   *
   * @param id the route identifier
   * @return the route definition.
   */
  public RouteDefinition getRoute(final String id) throws NotFoundException, IOException {
    return eoRouteDefinitionRepository.getRouteById(id);
  }

  /**
   * Get Current Routing
   *
   * @return the list of the current route definitions.
   */
  public List<RouteDefinition> getCurrentRoutes() {
    List<RouteDefinition> routeList = new ArrayList<>();
    eoRouteDefinitionRepository.getRouteDefinitions().subscribe(routeList::add);
    return routeList;
  }

  /**
   * Is Routing part of legacy
   *
   * @param id the route identifier
   * @return true if the legacy route exists.
   */
  public boolean isLegacyRoute(final String id) {
    List<RouteDefinition> routeList = new ArrayList<>();
    routeDefinitionLocator
        .getRouteDefinitions()
        .subscribe(
            routeDefinition -> {
              if (routeDefinition.getId().equals(id)) {
                routeList.add(routeDefinition);
              }
            });
    return !routeList.isEmpty();
  }
}
