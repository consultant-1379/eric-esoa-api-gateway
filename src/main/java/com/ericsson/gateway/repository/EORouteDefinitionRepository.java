/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.repository;

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.gateway.exception.ApiGatewayGenericException;
import com.ericsson.gateway.model.RoutesJson;
import com.ericsson.oss.orchestration.so.common.error.factory.ErrorMessageFactory;
import com.ericsson.oss.orchestration.so.common.error.message.ErrorMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class EORouteDefinitionRepository implements RouteDefinitionRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(EORouteDefinitionRepository.class);
  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired RouteRepository routeRepository;

  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {
    return Flux.fromIterable(getAllRoutes());
  }

  @Override
  public Mono<Void> save(Mono<RouteDefinition> route) {
    return route.flatMap(
        r -> {
          try {
            RoutesJson routesJson = new RoutesJson(r.getId(), mapper.writeValueAsString(r));
            routeRepository.saveAndFlush(routesJson);
          } catch (Exception e) {
            ErrorMessage errorMessage =
                ErrorMessageFactory.buildFrom(
                    ApiGatewayErrorCode.FAILED_TO_PERSIST.getCode(), r.getId());
            LOGGER.error(errorMessage.getUserMessage());
            return Mono.defer(() -> Mono.error(new ApiGatewayGenericException(errorMessage)));
          }
          return Mono.empty();
        });
  }

  @Override
  public Mono<Void> delete(Mono<String> routeId) {
    return routeId.flatMap(
        id -> {
          try {
            routeRepository.deleteById(id);
          } catch (Exception e) {
            ErrorMessage errorMessage =
                ErrorMessageFactory.buildFrom(
                    ApiGatewayErrorCode.ROUTE_DEFINITION_NOT_FOUND.getCode(), id);
            LOGGER.error(errorMessage.getUserMessage());
            return Mono.defer(() -> Mono.error(new ApiGatewayGenericException(errorMessage)));
          }
          return Mono.empty();
        });
  }

  public List<RouteDefinition> getAllRoutes() {
    List<RoutesJson> routeList = routeRepository.findAll();
    List<RouteDefinition> routes = new ArrayList<>();
    for (RoutesJson rj : routeList) {
      String s = rj.getRouteDefinition();
      try {
        RouteDefinition definition = mapper.readValue(s, RouteDefinition.class);
        routes.add(definition);
      } catch (IOException e) {
        LOGGER.error(
            ErrorMessageFactory.buildUserMessage(
                ApiGatewayErrorCode.ISSUE_RETRIEVING_ROUTE_DEFINITION.getCode(), "All Routes"));
      }
    }
    return routes;
  }

  public RouteDefinition getRouteById(String routeId) throws NotFoundException, IOException {
    Optional<RoutesJson> routesJson = routeRepository.findById(routeId);
    LOGGER.info("Getting route with id {}", routeId);
    if (routesJson.isPresent()) {
      try {
        return mapper.readValue(routesJson.get().getRouteDefinition(), RouteDefinition.class);

      } catch (IOException e) {
        throw new IOException(
            String.format(
                "Cannot get routeDefinition with id: %s.%nError: %s", routeId, e.getMessage()));
      }
    } else throw new NotFoundException("RouteDefinition not found: " + routeId);
  }
}
