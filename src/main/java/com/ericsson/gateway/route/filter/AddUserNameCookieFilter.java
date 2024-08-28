/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.route.filter;

import com.ericsson.gateway.provider.KeycloakProperties;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

@Component
public class AddUserNameCookieFilter implements GatewayFilterFactory<Config> {

  private final Logger logger = LoggerFactory.getLogger(AddUserNameCookieFilter.class);

  private static final String USER_NAME = "userName";
  private static final String TENANT_NAME = "tenantName";

  @Autowired ReactiveOAuth2AuthorizedClientService clientService;

  @Autowired KeycloakProperties keyCloakProperties;

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      final AtomicReference<ServerWebExchange> updatedExchange = new AtomicReference<>(exchange);
      exchange
          .getSession()
          .subscribe(session -> updatedExchange.set(addUserDataHeader(exchange, session)));
      return chain.filter(updatedExchange.get());
    };
  }

  @Override
  public Class<Config> getConfigClass() {
    return Config.class;
  }

  private ServerWebExchange addUserDataHeader(ServerWebExchange exchange, WebSession session) {
    try {
      final Optional<OAuth2AuthenticationToken> token = extractTokenFromSession(session);
      if (token.isPresent()) {
        final Map<String, Object> attributes = token.get().getPrincipal().getAttributes();

        final String userName = (String) attributes.get(keyCloakProperties.getUserName());
        final String tenantName = (String) attributes.get(keyCloakProperties.getTenantName());

        final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
        cookies.add(USER_NAME, ResponseCookie.from(USER_NAME, userName).path("/").build());
        if (tenantName != null) {
          cookies.add(TENANT_NAME, ResponseCookie.from(TENANT_NAME, tenantName).path("/").build());
        }

        exchange.getResponse().getCookies().addAll(cookies);
      }
    } catch (final Exception ex) {
      logger.error("userHeader filter addition Failed", ex);
    }
    return exchange;
  }

  private Optional<OAuth2AuthenticationToken> extractTokenFromSession(WebSession session) {
    final SecurityContext context = session.getAttribute("SPRING_SECURITY_CONTEXT");
    if (null != context) {
      return Optional.ofNullable((OAuth2AuthenticationToken) context.getAuthentication());
    }
    return Optional.empty();
  }

  @Override
  public Config newConfig() {
    return new Config();
  }
}
