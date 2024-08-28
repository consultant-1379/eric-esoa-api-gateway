/*
 * Copyright Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.audit;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditorAwareImpl implements AuditorAware<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditorAwareImpl.class);

  @Override
  public Optional<String> getCurrentAuditor() {
    LOGGER.info("getting current auditor");
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    LOGGER.info(
        "authentication.isAuthenticated() : {}",
        (authentication != null ? authentication.isAuthenticated() : "null"));

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    LOGGER.info("authentication name {}", authentication.getName());
    return Optional.of(authentication.getName());
  }
}
