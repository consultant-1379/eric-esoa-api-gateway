/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.security.authorization.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Resource configured in the Authorization Server. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtectedResource {

  @JsonProperty("_id")
  private String id;

  private String name;
  private Set<String> uris;
  private String type;
  private Set<String> scopes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<String> getUris() {
    return uris;
  }

  public void setUris(Set<String> uris) {
    this.uris = uris;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  @JsonSetter
  public void setScopes(List<Map<String, Object>> scopes) {
    this.scopes =
        scopes != null
            ? scopes.stream().map(m -> (String) m.get("name")).collect(Collectors.toSet())
            : Collections.emptySet();
  }

  @Override
  public String toString() {
    return "ProtectedResource{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", uris="
        + uris
        + ", type='"
        + type
        + '\''
        + ", scopes="
        + scopes
        + '}';
  }
}
