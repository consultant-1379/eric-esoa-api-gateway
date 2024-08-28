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

public final class RequestConstants {
  public static final String VNF_INSTANCE_REQUEST = "/vnflcm/v1/vnf_instances/";
  public static final String VNF_INSTANCE_ID_REQUEST = VNF_INSTANCE_REQUEST + "{vnfInstanceId}";
  public static final String VNF_LCM_OP_OCCS_REQUEST = "/vnflcm/v1/vnf_lcm_op_occs/";
  public static final String VNF_LCM_OP_OCCS_ID_REQUEST =
      VNF_LCM_OP_OCCS_REQUEST + "{vnfLcmOpOccId}";
  public static final String CLUSTER_CONFIG_REQUEST = "/vnflcm/v1/clusterconfigs/";
  public static final String CLUSTER_CONFIG_NAME_REQUEST =
      CLUSTER_CONFIG_REQUEST + "{clusterConfigName}";
  public static final String CHARTS_REQUEST = "/vnfm/onboarding/api/v1/charts/";
  public static final String IMAGES_REQUEST = "/vnfm/onboarding/api/v1/images/";
  public static final String IMAGES_NAME_REQUEST = IMAGES_REQUEST + "{imageName}";
  public static final String PACKAGES_REQUEST = "/vnfm/onboarding/api/v1/packages/";
  public static final String PACKAGES_ID_REQUEST = PACKAGES_REQUEST + "{id}";
  public static final String NAMESPACE_REQUEST = "/vnflcm/v1/validateNamespace/";
  public static final String RESOURCES_REQUEST = "/vnfm/container/api/v1/resources/";
  public static final String RESOURCES_ID_REQUEST = RESOURCES_REQUEST + "{resourceId}";
  public static final String VNF_PACKAGES_REQUEST = "/vnfm/onboarding/api/vnfpkgm/v1/vnf_packages/";
  public static final String VNF_PACKAGES_ID_REQUEST = VNF_PACKAGES_REQUEST + "{id}";

  private RequestConstants() {}
}
