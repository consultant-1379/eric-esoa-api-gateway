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

import static com.ericsson.gateway.util.RequestConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class RequestMappingUtils {
  private static final String ID_REGEX = "[\\w+-]+";

  private static final String VNF_INSTANCE_REGEX = VNF_INSTANCE_REQUEST + ID_REGEX;
  private static final String VNF_INSTANCE_BACKUP_REGEX = "[\\w+/]+";
  private static final String VNF_LCM_OP_OCCS_REGEX = VNF_LCM_OP_OCCS_REQUEST + ID_REGEX;
  private static final String CLUSTER_CONFIG_REGEX = CLUSTER_CONFIG_REQUEST + "\\w+";
  private static final String CHARTS_REGEX = CHARTS_REQUEST + "\\w+";
  private static final String IMAGES_REGEX = IMAGES_REQUEST + ID_REGEX;
  private static final String IMAGES_TAG = "/[\\w+.-]+";
  private static final String OPERATION_REGEX = "/\\w+";
  private static final String PACKAGES_REGEX = PACKAGES_REQUEST + ID_REGEX;
  private static final String NAMESPACE_REGEX = NAMESPACE_REQUEST + "[\\w+/-]+";
  private static final String RESOURCES_REGEX = RESOURCES_REQUEST + ID_REGEX;
  private static final String VNF_PACKAGES_REGEX = VNF_PACKAGES_REQUEST + ID_REGEX;

  private static final Map<Pattern, String> vnfInstanceRequestMap = new HashMap<>();
  private static final Map<Pattern, String> vnfLcmOoOccsRequestMap = new HashMap<>();
  private static final Map<Pattern, String> vnfPackagesRequestMap = new HashMap<>();
  private static final Map<Pattern, String> clusterConfigRequestMap = new HashMap<>();
  private static final Map<Pattern, String> namespaceRequestMap = new HashMap<>();
  private static final Map<Pattern, String> chartsRequestMap = new HashMap<>();
  private static final Map<Pattern, String> imagesRequestMap = new HashMap<>();
  private static final Map<Pattern, String> packagesRequestMap = new HashMap<>();
  private static final Map<Pattern, String> resourceRequestMap = new HashMap<>();

  private static final Map<String, Map<Pattern, String>> requestMap = new HashMap<>();

  static {
    vnfInstanceRequestMap.put(Pattern.compile(VNF_INSTANCE_REGEX), VNF_INSTANCE_ID_REQUEST);
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/instantiate"),
        VNF_INSTANCE_ID_REQUEST + "/instantiate");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/change_package_info"),
        VNF_INSTANCE_ID_REQUEST + "/change_package_info");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/terminate"), VNF_INSTANCE_ID_REQUEST + "/terminate");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/change_vnfpkg"),
        VNF_INSTANCE_ID_REQUEST + "/change_vnfpkg");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/scale"), VNF_INSTANCE_ID_REQUEST + "/scale");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/addNode"), VNF_INSTANCE_ID_REQUEST + "/addNode");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/deleteNode"),
        VNF_INSTANCE_ID_REQUEST + "/deleteNode");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/cleanup"), VNF_INSTANCE_ID_REQUEST + "/cleanup");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/values"), VNF_INSTANCE_ID_REQUEST + "/values");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/backups"), VNF_INSTANCE_ID_REQUEST + "/backups");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/backup/scopes"),
        VNF_INSTANCE_ID_REQUEST + "/backup/scopes");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/backups/" + VNF_INSTANCE_BACKUP_REGEX),
        VNF_INSTANCE_ID_REQUEST + "/backups/{backupName}/{scope}");
    vnfInstanceRequestMap.put(
        Pattern.compile(VNF_INSTANCE_REGEX + "/sync"), VNF_INSTANCE_ID_REQUEST + "/sync");
    vnfLcmOoOccsRequestMap.put(Pattern.compile(VNF_LCM_OP_OCCS_REGEX), VNF_LCM_OP_OCCS_ID_REQUEST);
    vnfLcmOoOccsRequestMap.put(
        Pattern.compile(VNF_LCM_OP_OCCS_REGEX + "/rollback"),
        VNF_LCM_OP_OCCS_ID_REQUEST + "/rollback");
    vnfLcmOoOccsRequestMap.put(
        Pattern.compile(VNF_LCM_OP_OCCS_REGEX + "/fail"), VNF_LCM_OP_OCCS_ID_REQUEST + "/fail");
    clusterConfigRequestMap.put(Pattern.compile(CLUSTER_CONFIG_REGEX), CLUSTER_CONFIG_NAME_REQUEST);
    namespaceRequestMap.put(
        Pattern.compile(NAMESPACE_REGEX), NAMESPACE_REQUEST + "{clusterName}/{namespace}");
    resourceRequestMap.put(Pattern.compile(RESOURCES_REGEX), RESOURCES_ID_REQUEST);
    resourceRequestMap.put(
        Pattern.compile(RESOURCES_REGEX + "/pods"), RESOURCES_ID_REQUEST + "/pods");
    resourceRequestMap.put(
        Pattern.compile(RESOURCES_REGEX + "/vnfcScaleInfo"),
        RESOURCES_ID_REQUEST + "/vnfcScaleInfo");
    resourceRequestMap.put(
        Pattern.compile(RESOURCES_REGEX + "/downgradeInfo"),
        RESOURCES_ID_REQUEST + "/downgradeInfo");
    resourceRequestMap.put(
        Pattern.compile(RESOURCES_REGEX + "/rollbackInfo"), RESOURCES_ID_REQUEST + "/rollbackInfo");
    packagesRequestMap.put(Pattern.compile(PACKAGES_REGEX), PACKAGES_ID_REQUEST);
    packagesRequestMap.put(
        Pattern.compile(PACKAGES_REGEX + "/status"), PACKAGES_ID_REQUEST + "/status");
    packagesRequestMap.put(
        Pattern.compile(PACKAGES_REGEX + "/additional_parameters"),
        PACKAGES_ID_REQUEST + "/additional_parameters");
    packagesRequestMap.put(
        Pattern.compile(PACKAGES_REGEX + OPERATION_REGEX + "/service_model"),
        PACKAGES_ID_REQUEST + "/{operation}/service_model");
    packagesRequestMap.put(
        Pattern.compile(PACKAGES_REGEX + "/supported_operations"),
        PACKAGES_ID_REQUEST + "/supported_operations");
    vnfPackagesRequestMap.put(Pattern.compile(VNF_PACKAGES_REGEX), VNF_PACKAGES_ID_REQUEST);
    vnfPackagesRequestMap.put(
        Pattern.compile(VNF_PACKAGES_REGEX + "/package_content"),
        VNF_PACKAGES_ID_REQUEST + "/package_content");
    vnfPackagesRequestMap.put(
        Pattern.compile(VNF_PACKAGES_REGEX + "/vnfd"), VNF_PACKAGES_ID_REQUEST + "/vnfd");
    chartsRequestMap.put(Pattern.compile(CHARTS_REGEX), CHARTS_REQUEST + "{chartName}");
    imagesRequestMap.put(Pattern.compile(IMAGES_REGEX), IMAGES_NAME_REQUEST);
    imagesRequestMap.put(
        Pattern.compile(IMAGES_REGEX + IMAGES_TAG), IMAGES_NAME_REQUEST + "/{imageTag}");

    requestMap.put(VNF_INSTANCE_REQUEST, vnfInstanceRequestMap);
    requestMap.put(VNF_LCM_OP_OCCS_REQUEST, vnfLcmOoOccsRequestMap);
    requestMap.put(CLUSTER_CONFIG_REQUEST, clusterConfigRequestMap);
    requestMap.put(NAMESPACE_REQUEST, namespaceRequestMap);
    requestMap.put(CHARTS_REQUEST, chartsRequestMap);
    requestMap.put(RESOURCES_REQUEST, resourceRequestMap);
    requestMap.put(PACKAGES_REQUEST, packagesRequestMap);
    requestMap.put(VNF_PACKAGES_REQUEST, vnfPackagesRequestMap);
    requestMap.put(IMAGES_REQUEST, imagesRequestMap);
  }

  private RequestMappingUtils() {}

  public static Map<String, Map<Pattern, String>> requestMap() {
    return requestMap;
  }
}
