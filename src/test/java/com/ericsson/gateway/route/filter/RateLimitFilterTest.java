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

import com.ericsson.gateway.BasicSpringBootTest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RateLimitFilterTest extends BasicSpringBootTest {

  @Autowired RateLimitFilter filter;

  @Test
  void successReturnFromRateLimitFilter()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = filter.getClass().getDeclaredMethod("apply", Config.class);
    method.setAccessible(true);
    method.invoke(filter, new Config());
  }
}
