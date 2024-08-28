/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.provider;

import org.springframework.beans.factory.annotation.Value;

public class KeycloakProperties {

  private String providerName = "keycloak";
  private String attributeKey = "sub";
  private String userName = "preferred_username";
  private String tenantName = "tenant_name";

  @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
  private String issuerUrl;

  @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
  private String clientSecret;

  public String getIssuerUrl() {
    return issuerUrl;
  }

  public void setIssuerUrl(String issuerUrl) {
    this.issuerUrl = issuerUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getProviderName() {
    return providerName;
  }

  public String getAttributeKey() {
    return attributeKey;
  }

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public void setAttributeKey(String attributeKey) {
    this.attributeKey = attributeKey;
  }
}
