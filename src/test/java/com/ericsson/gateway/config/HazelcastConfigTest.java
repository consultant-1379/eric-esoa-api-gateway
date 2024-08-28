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

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HazelcastConfig.class)
class HazelcastConfigTest {

  @Autowired private ConfigurableApplicationContext context;

  @Test
  void shouldDisableMulticast() {
    // given
    // when
    HazelcastInstance hazelcastInstance = (HazelcastInstance) context.getBean("hazelcastInstance");
    boolean isMulticastDisabled =
        hazelcastInstance.getConfig().getNetworkConfig().getJoin().getMulticastConfig().isEnabled();
    // then
    assertFalse(isMulticastDisabled);
  }
}
