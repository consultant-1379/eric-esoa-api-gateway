/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import com.ericsson.gateway.model.GatewayFilterDefinition;
import com.ericsson.gateway.model.GatewayPredicateDefinition;
import com.ericsson.gateway.model.GatewayRouteDefinition;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

public class AssembleRoute {

  private AssembleRoute() {}

  public static RouteDefinition assembleRouteDefinition(GatewayRouteDefinition routeDefinition) {
    RouteDefinition definition = new RouteDefinition();
    List<PredicateDefinition> pdList = new ArrayList<>();

    definition.setId(routeDefinition.getId());
    definition.setOrder(routeDefinition.getOrder());

    List<GatewayPredicateDefinition> gatewayPredicateDefinitionList =
        routeDefinition.getPredicates();
    for (GatewayPredicateDefinition gpDefinition : gatewayPredicateDefinitionList) {
      PredicateDefinition predicate = new PredicateDefinition();
      predicate.setArgs(gpDefinition.getArgs());
      predicate.setName(gpDefinition.getName());
      pdList.add(predicate);
    }

    List<GatewayFilterDefinition> gatewayFilterDefinitions = routeDefinition.getFilters();
    List<FilterDefinition> filterList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(gatewayFilterDefinitions)) {
      for (GatewayFilterDefinition gatewayFilterDefinition : gatewayFilterDefinitions) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(gatewayFilterDefinition.getName());
        filterDefinition.setArgs(gatewayFilterDefinition.getArgs());
        filterList.add(filterDefinition);
      }
    }
    definition.setPredicates(pdList);
    definition.setFilters(filterList);

    URI uri = UriComponentsBuilder.fromHttpUrl(routeDefinition.getUri()).build().toUri();
    definition.setUri(uri);
    return definition;
  }
}
