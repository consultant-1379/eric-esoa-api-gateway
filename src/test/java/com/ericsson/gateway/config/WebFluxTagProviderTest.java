/*
 * Copyright Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.Tag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {WebFluxTagProvider.class})
public class WebFluxTagProviderTest {

  @Test
  public void shouldMapVnfInstanceRequest() {
    String expectedRequest =
        mapRequest("/vnflcm/v1/vnf_instances/68f20904-b3a2-4615-9187-a9c7a73dd0e");

    assertEquals("/vnflcm/v1/vnf_instances/{vnfInstanceId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfInstanceRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/vnf_instances/null");

    assertEquals("/vnflcm/v1/vnf_instances/null", expectedRequest);
  }

  @Test
  public void shouldMapVnfInstanceBackupRequest() {
    String expectedRequest =
        mapRequest(
            "/vnflcm/v1/vnf_instances/68f20904-b3a2-4615-9187-a9c7a73dd0e/backups/some_backup_name/some_scope");

    assertEquals(
        "/vnflcm/v1/vnf_instances/{vnfInstanceId}/backups/{backupName}/{scope}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfInstanceBackupRequest() {
    String expectedRequest =
        mapRequest("/vnflcm/v1/vnf_instances/null/backups/some_backup_name/some_scope");

    assertEquals(
        "/vnflcm/v1/vnf_instances/null/backups/some_backup_name/some_scope", expectedRequest);
  }

  @Test
  public void shouldMapVnfLsmOpOccSRequest() {
    String expectedRequest =
        mapRequest("/vnflcm/v1/vnf_lcm_op_occs/68f20904-b3a2-4615-9187-a9c7a73dd0e");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/{vnfLcmOpOccId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfLsmOpOccSRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/vnf_lcm_op_occs/undefined");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/undefined", expectedRequest);
  }

  @Test
  public void shouldMapVnfLsmOpOccSRollbackRequest() {
    String expectedRequest =
        mapRequest("/vnflcm/v1/vnf_lcm_op_occs/68f20904-b3a2-4615-9187-a9c7a73dd0e/rollback");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/{vnfLcmOpOccId}/rollback", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfLsmOpOccSRollbackRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/vnf_lcm_op_occs/null/rollback");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/null/rollback", expectedRequest);
  }

  @Test
  public void shouldMapClusterConfigRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/clusterconfigs/some_name");

    assertEquals("/vnflcm/v1/clusterconfigs/{clusterConfigName}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidClusterConfigRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/clusterconfigs/null");

    assertEquals("/vnflcm/v1/clusterconfigs/null", expectedRequest);
  }

  @Test
  public void shouldMapNamespaceRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/validateNamespace/default/zpakole-ns");

    assertEquals("/vnflcm/v1/validateNamespace/{clusterName}/{namespace}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidNamespaceRequest() {
    String expectedRequest = mapRequest("/vnflcm/v1/validateNamespace/null/zpakole-ns");

    assertEquals("/vnflcm/v1/validateNamespace/null/zpakole-ns", expectedRequest);
  }

  @Test
  public void shouldMapResourceRequest() {
    String expectedRequest =
        mapRequest("/vnfm/container/api/v1/resources/f4724dce-ea6c-4659-93f1-728195158624");

    assertEquals("/vnfm/container/api/v1/resources/{resourceId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidResourceRequest() {
    String expectedRequest = mapRequest("/vnfm/container/api/v1/resources/null");

    assertEquals("/vnfm/container/api/v1/resources/null", expectedRequest);
  }

  @Test
  public void shouldMapChartRequest() {
    String expectedRequest = mapRequest("/vnfm/onboarding/api/v1/charts/chart_name");

    assertEquals("/vnfm/onboarding/api/v1/charts/{chartName}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidChartRequest() {
    String expectedRequest = mapRequest("/vnfm/onboarding/api/v1/charts/null");

    assertEquals("/vnfm/onboarding/api/v1/charts/null", expectedRequest);
  }

  @Test
  public void shouldMapPackagesRequest() {
    String expectedRequest =
        mapRequest(
            "/vnfm/onboarding/api/v1/packages/68f20904-b3a2-4615-9187-a9c7a73dd0e/deletion/service_model");

    assertEquals(
        "/vnfm/onboarding/api/v1/packages/{id}/{operation}/service_model", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidPackagesRequest() {
    String expectedRequest =
        mapRequest("/vnfm/onboarding/api/v1/packages/null/supported_operations");

    assertEquals("/vnfm/onboarding/api/v1/packages/null/supported_operations", expectedRequest);
  }

  @Test
  public void shouldMapVnfPackagesRequest() {
    String expectedRequest =
        mapRequest(
            "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/f4724dce-ea6c-4659-93f1-728195158624/package_content");

    assertEquals(
        "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/{id}/package_content", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfPackagesRequest() {
    String expectedRequest =
        mapRequest("/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/null/package_content");

    assertEquals(
        "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/null/package_content", expectedRequest);
  }

  @Test
  public void shouldMapImageRequest() {
    String expectedRequest =
        mapRequest("/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/2.1.1-21");

    assertEquals("/vnfm/onboarding/api/v1/images/{imageName}/{imageTag}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidImageRequest() {
    String expectedRequest =
        mapRequest("/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/null");

    assertEquals("/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/null", expectedRequest);
  }

  @Test
  public void shouldNotMapRequestWithoutVariables() {
    String expectedRequest = mapRequest("/vnflcm/v1/clusterconfigs");

    assertEquals("/vnflcm/v1/clusterconfigs", expectedRequest);
  }

  @Test
  public void shouldNotMapEmptyRequest() {
    String expectedRequest = mapRequest("");

    assertEquals("", expectedRequest);
  }

  @Test
  public void shouldNotMapNullRequest() {

    String expectedRequest = mapRequest("null");

    assertEquals("null", expectedRequest);
  }

  private String getUri(Iterable<Tag> tags) {
    String uri = "";

    for (Tag tag : tags) {
      if ("uri".equals(tag.getKey())) {
        uri = tag.getValue();
        break;
      }
    }

    return uri;
  }

  private String mapRequest(String request) {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(request));
    WebFluxTagProvider webFluxTagProvider = new WebFluxTagProvider();

    Iterable<Tag> tags =
        webFluxTagProvider.webFluxTagsProvider().httpRequestTags(exchange, new Exception());

    return getUri(tags);
  }
}
