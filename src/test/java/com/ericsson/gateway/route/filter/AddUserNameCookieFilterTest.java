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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.ericsson.gateway.provider.KeycloakProperties;
import com.ericsson.gateway.security.authorization.client.TokenAPI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AddUserNameCookieFilterTest {

  private static final String TENANTNAME = "tenantName";
  private static final String TENANTNAME_VALUE = "Tenant1";

  private static final String USERNAME = "userName";
  private static final String USERNAME_VALUE = "user1";

  @InjectMocks private AddUserNameCookieFilter addUserNameCookieFilter;

  @Mock private ReactiveOAuth2AuthorizedClientService clientService;

  @Spy private final KeycloakProperties keyCloakProperties = new KeycloakProperties();

  @Mock private TokenAPI tokenAPI;

  @Mock private GatewayFilterChain gatewayFilterChain;

  private MockServerWebExchange exchange;

  @BeforeEach
  void setup() {
    exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test"));
  }

  @Test
  void shouldAddTenantnameCookieToResponse() {
    final Map<String, Object> props = new HashMap<>();
    props.put("tenant_name", TENANTNAME_VALUE);
    setupSession(props);

    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    addUserNameCookieFilter.apply(new Config()).filter(exchange, gatewayFilterChain).block();

    final MockServerHttpResponse response = exchange.getResponse();
    assertEquals(TENANTNAME_VALUE, response.getCookies().getFirst(TENANTNAME).getValue());
  }

  @Test
  void shouldNotAddTenantnameCookieToResponseIfDoesNotExist() {
    final Map<String, Object> props = new HashMap<>();
    props.put("preferred_username", USERNAME_VALUE);
    setupSession(props);

    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    addUserNameCookieFilter.apply(new Config()).filter(exchange, gatewayFilterChain).block();

    final MockServerHttpResponse response = exchange.getResponse();
    assertNull(response.getCookies().getFirst(TENANTNAME));
  }

  @Test
  void shouldAddUsernameCookieToResponse() {
    final Map<String, Object> props = new HashMap<>();
    props.put("preferred_username", USERNAME_VALUE);
    props.put("tenant_name", TENANTNAME_VALUE);
    setupSession(props);

    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    addUserNameCookieFilter.apply(new Config()).filter(exchange, gatewayFilterChain).block();

    final MockServerHttpResponse response = exchange.getResponse();
    assertEquals(USERNAME_VALUE, response.getCookies().getFirst(USERNAME).getValue());
  }

  @Test
  void shouldAddNoCookiesIfNoSpringSecurityContext() {
    when(gatewayFilterChain.filter(Mockito.any())).thenReturn(Mono.empty());

    addUserNameCookieFilter.apply(new Config()).filter(exchange, gatewayFilterChain).block();

    final MockServerHttpResponse response = exchange.getResponse();
    assertEquals(0, response.getCookies().size());
  }

  private void setupSession(final Map<String, Object> props) {
    final SecurityContextImpl securityContext =
        new SecurityContextImpl(createOAuth2AuthenticationToken(props));
    exchange.getSession().block().getAttributes().put("SPRING_SECURITY_CONTEXT", securityContext);
  }

  private OAuth2AuthenticationToken createOAuth2AuthenticationToken(
      final Map<String, Object> props) {
    props.put("sub", "sub1");

    final OAuth2User user =
        new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")), props, "sub");

    return new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak");
  }
}
