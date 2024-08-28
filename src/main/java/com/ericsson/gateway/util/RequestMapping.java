/*
 * Copyright Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class RequestMapping {

  private RequestMapping() {}

  /**
   * @param request is a string with the specific value of variables such an id, namespace etc.
   * @exception NullPointerException if request is null
   * @return a string with a variables in a general form such an {id}, {namespace} etc.
   */
  public static String mapRequest(final String request) {
    if (isValidRequest(request)) {
      Optional<Map<Pattern, String>> requestMap = neededMap(request);

      if (requestMap.isPresent()) {
        Optional<Pattern> requestPattern =
            requestMap.get().keySet().stream()
                .filter(pattern -> pattern.matcher(request).matches())
                .findAny();

        if (requestPattern.isPresent()) {
          return requestMap.get().get(requestPattern.get());
        }
      }
    }

    return request;
  }

  private static Optional<Map<Pattern, String>> neededMap(String request) {
    Map<String, Map<Pattern, String>> requestMap = RequestMappingUtils.requestMap();
    Optional<String> pattern = requestMap.keySet().stream().filter(request::startsWith).findAny();

    return pattern.map(requestMap::get);
  }

  private static boolean isValidRequest(String request) {
    Objects.requireNonNull(request, "request must not be null");
    List<String> requestParts = Arrays.asList(request.split("/"));

    return !(requestParts.contains("null") || requestParts.contains("undefined"));
  }
}
