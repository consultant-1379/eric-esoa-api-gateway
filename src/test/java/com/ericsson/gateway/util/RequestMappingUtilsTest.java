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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Test;

public class RequestMappingUtilsTest {

  @Test
  public void shouldReturnMap() {
    Map<String, Map<Pattern, String>> map = RequestMappingUtils.requestMap();

    assertNotEquals(null, map);

    assertFalse(map.isEmpty());
  }
}
