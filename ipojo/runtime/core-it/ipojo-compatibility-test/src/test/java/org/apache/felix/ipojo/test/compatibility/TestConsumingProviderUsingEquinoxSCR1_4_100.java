/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.test.compatibility;

import org.apache.felix.ipojo.test.compatibility.service.CheckService;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.ow2.chameleon.testing.helpers.Dumps;

import static org.fest.assertions.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;

/**
 * Check a configuration using Equinox SCR 1.4.100 (Declarative Services).
 * iPOJO consumer is bound to a Hello Service implementation using SCR.
 *
 *
 * This test is intended to pass on Equinox only.
 */
public class TestConsumingProviderUsingEquinoxSCR1_4_100 extends Common {

    public static String DS_URL = "http://people.apache.org/~clement/ipojo/bundle4tests/org.eclipse.equinox.ds_1.4.101.v20130813-1853.jar";

    public static String UTILS_URL = "http://people.apache.org/~clement/ipojo/bundle4tests/org.eclipse.equinox.util_1.0.500.v20130404-1337.jar";

    @Override
    public Option[] bundles() {
        if (! isEquinox()) {
            return new Option[0];
        }
        return new Option[] {
                bundle(DS_URL),
                bundle(UTILS_URL),
                SCRHelloProvider(),
                iPOJOHelloConsumer()
        };
    }

    @Test
    public void test() {
        if (! isEquinox()) {
            System.out.println("Test executed on Equinox only");
            return;
        }
        CheckService checker = osgiHelper.waitForService(CheckService.class, null, 1000);
        assertThat(checker).isNotNull();
        assertThat(checker.data().get("result")).isEqualTo("hello john doe");
    }
}
