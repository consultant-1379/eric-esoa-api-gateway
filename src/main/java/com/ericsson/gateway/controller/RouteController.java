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

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.gateway.exception.ApiGatewayHttpException;
import com.ericsson.gateway.model.GatewayRouteDefinition;
import com.ericsson.gateway.repository.RouteRepository;
import com.ericsson.gateway.service.DynamicRouteServiceImpl;
import com.ericsson.gateway.util.AssembleRoute;
import com.ericsson.oss.orchestration.so.common.error.factory.ErrorMessageFactory;
import com.ericsson.oss.orchestration.so.common.error.message.ErrorMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * The v1/route endpoint allows for interactions with the Spring Cloud Gateway application to allow
 * adding, updating and deleting of routes at runtime. To be remotely accessible, the endpoint has
 * to be exposed over HTTP in the application properties.
 */
@RestController
@RequestMapping("v1/routes")
public class RouteController {

  @Autowired DynamicRouteServiceImpl dynamicRouteService;
  @Autowired RouteRepository routeRepository;

  private final Logger logger = LoggerFactory.getLogger(RouteController.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create a route definition
   *
   * @param routeDefinition the route definition to add
   * @return
   */
  @PostMapping()
  public ResponseEntity<Object> createRouteDefinition(
      @RequestBody GatewayRouteDefinition routeDefinition) {
    try {
      RouteDefinition definition = AssembleRoute.assembleRouteDefinition(routeDefinition);

      if (!routeRepository.findById(definition.getId()).isPresent()
          && !dynamicRouteService.isLegacyRoute(definition.getId())) {
        dynamicRouteService.createRoute(definition);
        return ResponseEntity.status(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(definition));
      } else {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Route with ID already exists");
      }
    } catch (Exception e) {
      ErrorMessage errorMessage =
          ErrorMessageFactory.buildFrom(
              ApiGatewayErrorCode.ISSUE_CREATING_ROUTE_DEFINITION.getCode(),
              routeDefinition.getId(),
              e.getMessage());
      logger.error(errorMessage.getUserMessage());
      throw ApiGatewayHttpException.internalServerError(
          ApiGatewayErrorCode.ISSUE_CREATING_ROUTE_DEFINITION,
          routeDefinition.getId(),
          e.getMessage());
    }
  }

  /**
   * Update a route definition
   *
   * @param routeDefinition the route definition to update
   * @return
   */
  @PutMapping()
  public Mono<ResponseEntity<Object>> updateRouteDefinition(
      @RequestBody GatewayRouteDefinition routeDefinition) {
    try {
      RouteDefinition definition = AssembleRoute.assembleRouteDefinition(routeDefinition);
      return this.dynamicRouteService
          .updateRoute(definition)
          .flatMap(
              isSuccessful -> {
                if (Boolean.TRUE.equals(isSuccessful)) {
                  try {
                    return Mono.just(
                        ResponseEntity.status(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(objectMapper.writeValueAsString(definition)));
                  } catch (JsonProcessingException e) {
                    ErrorMessage errorMessage =
                        ErrorMessageFactory.buildFrom(
                            ApiGatewayErrorCode.ISSUE_PROCESSING_JSON_ROUTE_DEFINITION.getCode(),
                            routeDefinition.getId());
                    logger.error(errorMessage.getUserMessage());
                    e.printStackTrace();
                  }
                } else {
                  return Mono.just(
                      ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                          .body(
                              String.format(
                                  "Could not update route with ID %s", routeDefinition.getId())));
                }
                return null;
              });
    } catch (Exception e) {
      throw ApiGatewayHttpException.internalServerError(
          ApiGatewayErrorCode.ISSUE_UPDATING_ROUTE_DEFINITION,
          routeDefinition.getId(),
          e.getMessage());
    }
  }

  /**
   * Delete a route definition
   *
   * @param id the id of route to delete
   * @return
   */
  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Object>> deleteRouteDefinitionById(@PathVariable String id) {
    return this.dynamicRouteService.deleteRoute(id);
  }

  /**
   * Get all route definitions
   *
   * @return
   */
  @GetMapping()
  public ResponseEntity<Object> getAllRouteDefinitions() {
    try {
      String json = objectMapper.writeValueAsString(this.dynamicRouteService.getCurrentRoutes());
      return ResponseEntity.status(HttpStatus.OK)
          .contentType(MediaType.APPLICATION_JSON)
          .body(json);
    } catch (JsonProcessingException ex) {
      throw ApiGatewayHttpException.internalServerError(
          ApiGatewayErrorCode.ISSUE_RETRIEVING_ROUTE_DEFINITION, "All Route Definition");
    }
  }

  /**
   * Get a route definition
   *
   * @param id the id of route to get
   * @return
   */
  @GetMapping("/{id}")
  public ResponseEntity<Object> getRouteDefinitionById(@PathVariable String id) {
    try {
      RouteDefinition definition = this.dynamicRouteService.getRoute(id);
      return ResponseEntity.status(HttpStatus.OK)
          .contentType(MediaType.APPLICATION_JSON)
          .body(objectMapper.writeValueAsString(definition));

    } catch (NotFoundException ex) {
      throw ApiGatewayHttpException.notFound(ApiGatewayErrorCode.ROUTE_DEFINITION_NOT_FOUND, id);
    } catch (IOException ex) {
      throw ApiGatewayHttpException.internalServerError(
          ApiGatewayErrorCode.ISSUE_RETRIEVING_ROUTE_DEFINITION, id);
    }
  }
}
