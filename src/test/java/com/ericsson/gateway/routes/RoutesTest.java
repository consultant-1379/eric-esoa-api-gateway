/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.routes;

import static org.junit.Assert.assertTrue;

import com.ericsson.gateway.ApiGatewayApplication;
import com.ericsson.gateway.config.WireMockIntegrationTestConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

@SpringBootTest(classes = {ApiGatewayApplication.class, WireMockIntegrationTestConfig.class})
@ActiveProfiles("test")
class RoutesTest {

  @Autowired private RouteDefinitionLocator routeDefinitionLocator;

  static void setRouteEnvironments() {
    // Add hostnames
    System.setProperty("SO_HOST", "so.host");
    System.setProperty("SDD_HOST", "sdd.host");
    System.setProperty("VNFM_HOST", "vnfm.host");
    System.setProperty("POLICY_HOST", "pf.host");
    System.setProperty("UDS_HOST", "uds.host");
    System.setProperty("CM_HOST", "cm.host");
    System.setProperty("GR_HOST", "gr.host");
    System.setProperty("GUI_AGGREGATOR_HOST", "guiaggr.host");
    // Add the new routes below
    addRoutePathVariables(
        "SO_DASHBOARD_CONTEXT_PATH",
        "SO_DASHBOARD_SERVICE_PATH",
        "/dashboard/**",
        "http://dashboard",
        "so.host");
    addRoutePathVariables(
        "SO_ONBOARDING_CONTEXT_PATH",
        "SO_ONBOARDING_SERVICE_PATH",
        "/onboarding/**",
        "http://onboarding",
        "so.host");
    addRoutePathVariables(
        "SO_ORCHESTRATIONCOCKPIT_CONTEXT_PATH",
        "SO_ORCHESTRATIONCOCKPIT_SERVICE_PATH",
        "/orchestrationcockpit/**",
        "http://orchestrationcockpit",
        "so.host");
    addRoutePathVariables(
        "SO_ORCHESTRATION_CONTEXT_PATH",
        "SO_ORCHESTRATION_SERVICE_PATH",
        "/orchestration/**",
        "http://engine",
        "so.host");
    addRoutePathVariables(
        "SO_SUBSYSTEMMANAGER_CONTEXT_PATH",
        "SO_SUBSYSTEMMANAGER_SERVICE_PATH",
        "/subsystems-manager/**",
        "http://subsystems-manager",
        "so.host");
    addRoutePathVariables(
        "SO_SUBSYSTEMMANAGER_CONTEXT_PATH",
        "SO_SUBSYSTEMMANAGER_SERVICE_PATH",
        "/subsystems-manager/**",
        "http://subsystems-manager",
        "uds.host");
    addRoutePathVariables(
        "SO_SUBSYSTEMMANAGEMENT_CONTEXT_PATH",
        "SO_SUBSYSTEMMANAGEMENT_SERVICE_PATH",
        "/subsystems-manager/**",
        "http://subsystems-management",
        "so.host");
    addRoutePathVariables(
        "SO_SUBSYSTEMMANAGEMENT_CONTEXT_PATH",
        "SO_SUBSYSTEMMANAGEMENT_SERVICE_PATH",
        "/subsystems-manager/**",
        "http://subsystems-management",
        "uds.host");
    addRoutePathVariables(
        "SUBSYSTEMSMGMT_UI_CONTEXT_PATH",
        "SUBSYSTEMSMGMT_UI_SERVICE_PATH",
        "/subsystemsmgmt*ui/**",
        "http://subsystemsmgmt-ui",
        "so.host");
    addRoutePathVariables(
        "SO_IPMANAGER_CONTEXT_PATH",
        "SO_IPMANAGER_SERVICE_PATH",
        "/ip-manager/**",
        "http://ip-manager",
        "so.host");
    addRoutePathVariables(
        "SO_IPAM_UI_CONTEXT_PATH",
        "SO_IPAM_UI_SERVICE_PATH",
        "/ipam*ui/**",
        "http://ipam-ui",
        "so.host");
    addRoutePathVariables(
        "SO_TOPOLOGY_CONTEXT_PATH",
        "SO_TOPOLOGY_SERVICE_PATH",
        "/topology/**",
        "http://topology",
        "so.host");
    addRoutePathVariables(
        "SO_NSSMF_AGENT_CONTEXT_PATH",
        "SO_NSSMF_AGENT_SERVICE_PATH",
        "/nssmf-agent/**",
        "http://eric-eo-so-nssmf-agent",
        "so.host");
    addRoutePathVariables(
        "SO_GUI_CONTEXT_PATH", "SO_GUI_SERVICE_PATH", "/**", "http://orchestration-gui", "so.host");
    addRoutePathVariables(
        "TENANTMGMT_SERVICE_CONTEXT_PATH",
        "TENANTMGMT_SERVICE_PATH",
        "/idm/tenantmgmt/**",
        "http://eric-eo-tenantmgmt",
        "so.host");
    addRoutePathVariables(
        "SDD_GUI_CONTEXT_PATH", "SDD_GUI_SERVICE_PATH", "/**", "http://sdd-sim", "sdd.host");
    addRoutePathVariables(
        "UDS_GUI_CONTEXT_PATH",
        "UDS_GUI_SERVICE_PATH",
        "/**",
        "http://eric-oss-uds-authenticator",
        "uds.host");
    addRoutePathVariables(
        "GUI_AGGREGATOR_CONTEXT_PATH",
        "GUI_AGGREGATOR_SERVICE_PATH",
        "/ui-meta/**",
        "http://gui-aggregator",
        "guiaggr.host");
    addRoutePathVariables(
        "GUI_AGGREGATOR_CONTEXT_PATH",
        "GUI_AGGREGATOR_SERVICE_PATH",
        "/ui-logging/**",
        "http://gui-aggregator",
        "guiaggr.host");
    addRoutePathVariables(
        "GUI_AGGREGATOR_CONTEXT_PATH",
        "GUI_AGGREGATOR_SERVICE_PATH",
        "/gas-internal/**",
        "http://gui-aggregator",
        "guiaggr.host");
    addRoutePathVariables(
        "GUI_AGGREGATOR_CONTEXT_PATH",
        "GUI_AGGREGATOR_SERVICE_PATH",
        "/**",
        "http://gui-aggregator",
        "guiaggr.host");
    addRoutePathVariables(
        "VNFM_ONBOARDING_CONTEXT_PATH",
        "VNFM_ONBOARDING_SERVICE_PATH",
        "/vnfm/onboarding/**",
        "http://vnfm-onboarding",
        "vnfm.host");
    addRoutePathVariables(
        "VNFM_CONTAINER_CONTEXT_PATH",
        "VNFM_CONTAINER_SERVICE_PATH",
        "/vnfm/container/**",
        "http://eric-eo-evnfm-nbi",
        "vnfm.host");
    addRoutePathVariables(
        "VNFM_NBI_CONTEXT_PATH",
        "VNFM_NBI_SERVICE_PATH",
        "/vnflcm/**",
        "http://eric-eo-evnfm-nbi",
        "vnfm.host");
    addRoutePathVariables(
        "VNFM_WFS_CONTEXT_PATH",
        "VNFM_NBI_SERVICE_PATH",
        "/vnfm/wfs/api/lcm/v2/cluster",
        "http://eric-eo-evnfm-nbi",
        "vnfm.host");
    addRoutePathVariables(
        "VNFM_GUI_CONTEXT_PATH",
        "VNFM_GUI_SERVICE_PATH",
        "/vnfm/**",
        "http://vnfm-gui",
        "vnfm.host");
    addRoutePathVariables(
        "VNFLCM_GUI_CONTEXT_PATH", // environment variable
        "VNFLCM_GUI_SERVICE_PATH",
        "/**",
        "http://eric-vnflcm-ui",
        "vnfm.host");
    addRoutePathVariables(
        "VNFLCM_VEVNFMEM_CONTEXT_PATH", // environment variable
        "VNFLCM_VEVNFMEM_SERVICE_PATH",
        "/vevnfmem/vnflcm/**",
        "http://eric-vnflcm-service",
        "vnfm.host");
    addRoutePathVariables(
        "BRO_CONTEXT_PATH", // environment variable);
        "BRO_SERVICE_PATH",
        "/backup-restore/**",
        "http://eric-ctrl-bro",
        "so.host");
    addRoutePathVariables(
        "BRO_CONTEXT_PATH", // environment variable);
        "BRO_SERVICE_PATH",
        "/backup-restore/**",
        "http://eric-ctrl-bro",
        "vnfm.host");
    addRoutePathVariables(
        "BRO_CONTEXT_PATH", // environment variable);
        "BRO_SERVICE_PATH",
        "/backup-restore/**",
        "http://eric-ctrl-bro",
        "uds.host");
    addRoutePathVariables(
        "BRO_CONTEXT_PATH", // environment variable);
        "BRO_SERVICE_PATH",
        "/backup-restore/**",
        "http://eric-ctrl-bro",
        "cm.host");
    addRoutePathVariables(
        "POLICY_PF_DIST_SERVICE_CONTEXT_PATH",
        "POLICY_PF_DIST_SERVICE_PATH",
        "/policy/dist/**",
        "https://eric-oss-pf-policy-dist:6969",
        "pf.host");
    addRoutePathVariables(
        "APEX_SERVICE_CONTEXT_PATH",
        "APEX_SERVICE_PATH",
        "/policy/apex-pdp/**",
        "https://eric-oss-pf-apex:6969",
        "pf.host");
    addRoutePathVariables(
        "POLICY_API_SERVICE_CONTEXT_PATH",
        "POLICY_API_SERVICE_PATH",
        "/policy/api/**",
        "https://eric-oss-pf-policyapi:6969",
        "pf.host");
    addRoutePathVariables(
        "POLICY_PF_PAP_SERVICE_CONTEXT_PATH",
        "POLICY_PF_PAP_SERVICE_PATH",
        "/policy/pap/**",
        "https://eric-oss-pf-pap:6969",
        "pf.host");
    addRoutePathVariables(
        "POLICY_PF_XACML_SERVICE_CONTEXT_PATH",
        "POLICY_PF_XACML_SERVICE_PATH",
        "/policy/pdpx/**",
        "https://eric-oss-pf-xacml:6969",
        "pf.host");
    addRoutePathVariables(
        "POLICY_PF_DROOLS_SERVICE_CONTEXT_PATH",
        "POLICY_PF_DROOLS_SERVICE_PATH",
        "/policy/drools/**",
        "https://eric-oss-pf-drools:6969",
        "pf.host");
    addRoutePathVariables(
        "DMAAP_SERVICE_CONTEXT_PATH",
        "DMAAP_SERVICE_PATH",
        "/dmaap-mr/**",
        "https://eric-oss-dmaap:3905",
        "pf.host");
    addRoutePathVariables(
        "GR_ORCHESTRATOR_CONTEXT_PATH",
        "GR_ORCHESTRATOR_SERVICE_PATH",
        "/**",
        "http://eric-gr-bur-orchestrator",
        "gr.host");
    addRoutePathVariables(
        "ECM_SERVICE_CONTEXT_PATH",
        "ECM_SERVICE_PATH",
        "/ecm_service/**",
        "https://ericsson-ecm-services",
        null);
  }

  static void addRoutePathVariables(
      final String contextVariableName,
      final String serviceVariableName,
      final String context,
      final String path,
      final String host) {
    System.setProperty(contextVariableName, context);
    System.setProperty(serviceVariableName, path);
    CustomArgumentProvider.list.add(Arguments.of(context, path, host));
  }

  @ParameterizedTest(name = "context=''{0}'', service={1}, host={2}")
  @ArgumentsSource(CustomArgumentProvider.class)
  void testRoutes(final String context, final String path, final String host) {
    final AtomicBoolean result = new AtomicBoolean(false);
    final Flux<Boolean> latest =
        routeDefinitionLocator.getRouteDefinitions().map(validatePath(context, path, host, result));
    latest.blockLast();
    assertTrue(result.get());
  }

  // TODO This method has side effect
  private Function<RouteDefinition, Boolean> validatePath(
      final String context, final String path, final String host, final AtomicBoolean result) {
    return routeDefinition -> {
      final boolean isPathValid = pathVerification(routeDefinition, path, context, host);
      if (isPathValid) {
        result.set(true);
      }
      return isPathValid;
    };
  }

  private boolean pathVerification(
      final RouteDefinition routeDefinition,
      final String path,
      final String context,
      final String host) {
    final boolean isPathValid = routeDefinition.getUri().toString().equals(path);
    return routeDefinition.getPredicates().stream()
        .allMatch(verifyPath(context, host, isPathValid));
  }

  private Predicate<PredicateDefinition> verifyPath(
      final String context, final String host, final boolean isPathValid) {
    return predicateDefinition -> {
      boolean result = isPathValid;
      final String predicateDefinitionName = predicateDefinition.getName();
      final Collection<String> values = predicateDefinition.getArgs().values();
      if ("path".equalsIgnoreCase(predicateDefinitionName)) {
        result &= values.contains(context);
      } else if ("host".equalsIgnoreCase(predicateDefinitionName)) {
        result &= values.contains(host);
      }
      return result;
    };
  }

  static class CustomArgumentProvider implements ArgumentsProvider {

    static List<Arguments> list = new ArrayList<>();

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      setRouteEnvironments();
      return list.stream();
    }
  }
}
