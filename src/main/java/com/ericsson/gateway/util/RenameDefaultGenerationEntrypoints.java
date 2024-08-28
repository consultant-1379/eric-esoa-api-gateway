/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import com.ericsson.gateway.exception.ApiGatewayErrorCode;
import com.ericsson.gateway.exception.ApiGatewayGenericException;
import java.lang.reflect.Field;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.ui.LoginPageGeneratingWebFilter;
import org.springframework.security.web.server.ui.LogoutPageGeneratingWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.WebFilter;

public class RenameDefaultGenerationEntrypoints {

  private static final Logger logger =
      LoggerFactory.getLogger(RenameDefaultGenerationEntrypoints.class);

  private RenameDefaultGenerationEntrypoints() {}

  private static void renameDefaultLoginEntrypoint(ServerHttpSecurity http)
      throws ApiGatewayGenericException {
    try {
      Field webFilterField = http.getClass().getSuperclass().getDeclaredField("webFilters");
      webFilterField.setAccessible(true);
      List<WebFilter> webFilters = (List<WebFilter>) webFilterField.get(http);
      for (WebFilter webfilter : webFilters) {
        if (((Ordered) webfilter).getOrder()
            == SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING.getOrder()) {
          Field[] loginWebFilterFields = webfilter.getClass().getDeclaredFields();
          for (Field loginWebFilterField : loginWebFilterFields) {
            if ("webFilter".equals(loginWebFilterField.getName())) {
              loginWebFilterField.setAccessible(true);

              LoginPageGeneratingWebFilter loginWebFilter =
                  (LoginPageGeneratingWebFilter) loginWebFilterField.get(webfilter);
              Field matcherField = loginWebFilter.getClass().getDeclaredField("matcher");
              matcherField.setAccessible(true);
              ServerWebExchangeMatcher matcher =
                  ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/disabled-login");
              matcherField.set(loginWebFilter, matcher);
            }
          }
        }
      }
    } catch (NoSuchFieldException
        | SecurityException
        | IllegalArgumentException
        | IllegalAccessException ex) {
      logger.error("exception while disabling default login page", ex);
      throw new ApiGatewayGenericException(
          ApiGatewayErrorCode.STARTUP_EXCEPTION_LOGIN, ex.getMessage());
    }
  }

  private static void renameDefaultLogoutEntrypoint(ServerHttpSecurity http)
      throws ApiGatewayGenericException {
    try {
      Field webFilterField = http.getClass().getSuperclass().getDeclaredField("webFilters");
      webFilterField.setAccessible(true);
      List<WebFilter> webFilters = (List<WebFilter>) webFilterField.get(http);
      for (WebFilter webfilter : webFilters) {
        if (((Ordered) webfilter).getOrder()
            == SecurityWebFiltersOrder.LOGOUT_PAGE_GENERATING.getOrder()) {
          Field[] loginWebFilterFields = webfilter.getClass().getDeclaredFields();
          for (Field loginWebFilterField : loginWebFilterFields) {
            if ("webFilter".equals(loginWebFilterField.getName())) {
              loginWebFilterField.setAccessible(true);

              LogoutPageGeneratingWebFilter loginWebFilter =
                  (LogoutPageGeneratingWebFilter) loginWebFilterField.get(webfilter);
              Field matcherField = loginWebFilter.getClass().getDeclaredField("matcher");
              matcherField.setAccessible(true);
              ServerWebExchangeMatcher matcher =
                  ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/disabled-logout");
              matcherField.set(loginWebFilter, matcher);
            }
          }
        }
      }
    } catch (NoSuchFieldException
        | SecurityException
        | IllegalArgumentException
        | IllegalAccessException ex) {
      logger.error("exception while disabling default logout page", ex);
      throw new ApiGatewayGenericException(
          ApiGatewayErrorCode.STARTUP_EXCEPTION_LOGOUT, ex.getMessage());
    }
  }

  public static void disableGeneratedPages(ServerHttpSecurity http)
      throws ApiGatewayGenericException {
    renameDefaultLoginEntrypoint(http);
    renameDefaultLogoutEntrypoint(http);
  }
}
