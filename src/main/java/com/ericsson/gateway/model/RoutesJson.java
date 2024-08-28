/*
 * Copyright Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.model;

import com.ericsson.gateway.audit.Auditable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RoutesJson extends Auditable<String> {
  @Id
  @Column(name = "ROUTE_ID", unique = true, columnDefinition = "VARCHAR(64)")
  private String id;

  @Column(columnDefinition = "VARCHAR(65535)")
  private String routeDefinition;
}
