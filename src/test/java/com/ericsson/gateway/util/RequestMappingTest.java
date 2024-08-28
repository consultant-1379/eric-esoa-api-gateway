/*
 * Copyright Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

public class RequestMappingTest {
  @Test
  public void shouldMapVnfInstanceRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnflcm/v1/vnf_instances/68f20904-b3a2-4615-9187-a9c7a73dd0e");

    assertEquals("/vnflcm/v1/vnf_instances/{vnfInstanceId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfInstanceRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/vnf_instances/null");

    assertEquals("/vnflcm/v1/vnf_instances/null", expectedRequest);
  }

  @Test
  public void shouldMapVnfInstanceBackupRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnflcm/v1/vnf_instances/68f20904-b3a2-4615-9187-a9c7a73dd0e/backups/some_backup_name/some_scope");

    assertEquals(
        "/vnflcm/v1/vnf_instances/{vnfInstanceId}/backups/{backupName}/{scope}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfInstanceBackupRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnflcm/v1/vnf_instances/null/backups/some_backup_name/some_scope");

    assertEquals(
        "/vnflcm/v1/vnf_instances/null/backups/some_backup_name/some_scope", expectedRequest);
  }

  @Test
  public void shouldMapVnfLsmOpOccSRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnflcm/v1/vnf_lcm_op_occs/68f20904-b3a2-4615-9187-a9c7a73dd0e");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/{vnfLcmOpOccId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfLsmOpOccSRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/vnf_lcm_op_occs/undefined");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/undefined", expectedRequest);
  }

  @Test
  public void shouldMapVnfLsmOpOccSRollbackRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnflcm/v1/vnf_lcm_op_occs/68f20904-b3a2-4615-9187-a9c7a73dd0e/rollback");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/{vnfLcmOpOccId}/rollback", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfLsmOpOccSRollbackRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/vnf_lcm_op_occs/null/rollback");

    assertEquals("/vnflcm/v1/vnf_lcm_op_occs/null/rollback", expectedRequest);
  }

  @Test
  public void shouldMapClusterConfigRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/clusterconfigs/some_name");

    assertEquals("/vnflcm/v1/clusterconfigs/{clusterConfigName}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidClusterConfigRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/clusterconfigs/null");

    assertEquals("/vnflcm/v1/clusterconfigs/null", expectedRequest);
  }

  @Test
  public void shouldMapNamespaceRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnflcm/v1/validateNamespace/default/zpakole-ns");

    assertEquals("/vnflcm/v1/validateNamespace/{clusterName}/{namespace}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidNamespaceRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnflcm/v1/validateNamespace/null/zpakole-ns");

    assertEquals("/vnflcm/v1/validateNamespace/null/zpakole-ns", expectedRequest);
  }

  @Test
  public void shouldMapResourceRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnfm/container/api/v1/resources/f4724dce-ea6c-4659-93f1-728195158624");

    assertEquals("/vnfm/container/api/v1/resources/{resourceId}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidResourceRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnfm/container/api/v1/resources/null");

    assertEquals("/vnfm/container/api/v1/resources/null", expectedRequest);
  }

  @Test
  public void shouldMapChartRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnfm/onboarding/api/v1/charts/chart_name");

    assertEquals("/vnfm/onboarding/api/v1/charts/{chartName}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidChartRequest() {
    String expectedRequest = RequestMapping.mapRequest("/vnfm/onboarding/api/v1/charts/null");

    assertEquals("/vnfm/onboarding/api/v1/charts/null", expectedRequest);
  }

  @Test
  public void shouldMapPackagesRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnfm/onboarding/api/v1/packages/68f20904-b3a2-4615-9187-a9c7a73dd0e/deletion/service_model");

    assertEquals(
        "/vnfm/onboarding/api/v1/packages/{id}/{operation}/service_model", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidPackagesRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnfm/onboarding/api/v1/packages/null/supported_operations");

    assertEquals("/vnfm/onboarding/api/v1/packages/null/supported_operations", expectedRequest);
  }

  @Test
  public void shouldMapVnfPackagesRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/f4724dce-ea6c-4659-93f1-728195158624/package_content");

    assertEquals(
        "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/{id}/package_content", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidVnfPackagesRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/null/package_content");

    assertEquals(
        "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/null/package_content", expectedRequest);
  }

  @Test
  public void shouldMapImageRequest() {
    String expectedRequest =
        RequestMapping.mapRequest(
            "/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/2.1.1-21");

    assertEquals("/vnfm/onboarding/api/v1/images/{imageName}/{imageTag}", expectedRequest);
  }

  @Test
  public void shouldNotMapInvalidImageRequest() {
    String expectedRequest =
        RequestMapping.mapRequest("/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/null");

    assertEquals("/vnfm/onboarding/api/v1/images/eric-sec-sip-tls-crd-job/null", expectedRequest);
  }

  @Test
  public void shouldNotMapRequestWithoutVariables() {
    String expectedRequest = RequestMapping.mapRequest("/vnflcm/v1/clusterconfigs");

    assertEquals("/vnflcm/v1/clusterconfigs", expectedRequest);
  }

  @Test
  public void shouldNotMapEmptyRequest() {
    String expectedRequest = RequestMapping.mapRequest("");

    assertEquals("", expectedRequest);
  }

  @Test
  public void shouldNotMapNullRequest() {

    String expectedRequest = RequestMapping.mapRequest("null");

    assertEquals("null", expectedRequest);
  }

  @Test
  public void shouldThrowNPEIfRequestIsNull() {
    Exception exception = assertThrows(NullPointerException.class, RequestMappingTest::execute);

    assertEquals("request must not be null", exception.getMessage());
  }

  private static void execute() {
    RequestMapping.mapRequest(null);
  }
}
