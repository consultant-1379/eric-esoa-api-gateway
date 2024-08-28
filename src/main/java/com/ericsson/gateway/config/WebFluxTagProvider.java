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

import static com.ericsson.gateway.util.RequestMapping.mapRequest;

import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTags;
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebFluxTagProvider {

  @Bean
  public WebFluxTagsProvider webFluxTagsProvider() {
    return (exchange, exception) -> {
      Tag urlTag;
      urlTag = Tag.of("uri", mapRequest(exchange.getRequest().getURI().getPath()));
      return Arrays.asList(
          WebFluxTags.method(exchange),
          urlTag,
          WebFluxTags.exception(exception),
          WebFluxTags.status(exchange),
          WebFluxTags.outcome(exchange, exception));
    };
  }
}
