/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.config;

import com.ericsson.gateway.util.RenameDefaultGenerationEntrypoints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@Profile("override-security")
class SecurityConfigOverride {

  @Bean("test")
  @Primary
  public SecurityWebFilterChain configure(ServerHttpSecurity http) throws Exception {
    http.csrf().disable().authorizeExchange().pathMatchers("/**").permitAll();

    http.headers().disable();
    SecurityWebFilterChain filterChain = http.build();
    RenameDefaultGenerationEntrypoints.disableGeneratedPages(http);
    return filterChain;
  }
}
