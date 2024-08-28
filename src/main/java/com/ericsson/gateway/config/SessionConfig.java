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

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSession;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
@EnableSpringWebSession
public class SessionConfig {

  @Value("${session.defaultMaxInactiveInterval}")
  int defaultMaxInactiveInterval;

  @Value("${session.cookieMaxAge}")
  int cookieMaxAge;

  @Bean
  public ReactiveSessionRepository<MapSession> sessionRepository() {
    ReactiveMapSessionRepository sessionRepo =
        new ReactiveMapSessionRepository(new ConcurrentHashMap<>());
    sessionRepo.setDefaultMaxInactiveInterval(defaultMaxInactiveInterval);
    return sessionRepo;
  }

  @Bean
  public WebSessionIdResolver headerWebSessionIdResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.setCookieMaxAge(Duration.ofSeconds(cookieMaxAge));
    resolver.setCookieName("JSESSIONID");
    return resolver;
  }
}
