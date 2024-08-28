/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ericsson.gateway.security")
public class SecurityProperties {

  private String[] allowedPaths;

  public String[] getAllowedPaths() {
    return allowedPaths;
  }

  public void setAllowedPaths(String[] allowedPaths) {
    this.allowedPaths = allowedPaths;
  }
}
