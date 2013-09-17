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

import static org.fest.assertions.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bundle;

/**
 * Check a configuration using Knopflerfish SCR 4.0.2
 * iPOJO consumer is bound to a Hello Service implementation using SCR.
 *
 *
 * This test is intended to pass on KF only
 */
public class TestConsumingProviderUsingKnopflerfishSCR4_0_2 extends Common {

    public static final String SCR_URL = "http://www.knopflerfish.org/releases/4.0" +
            ".0/maven2/org/knopflerfish/bundle/component/4.0.2/component-4.0.2.jar";

    @Override
    public Option[] bundles() {
        return new Option[] {
                bundle(SCR_URL),
                bundle("http://www.knopflerfish.org/releases/4.0.0/maven2/org/knopflerfish/bundle/cm-API/4.0" +
                        ".1/cm-API-4.0.1.jar"),
                bundle("http://www.knopflerfish.org/releases/4.0.0/maven2/org/knopflerfish/bundle/kxml-LIB/2.3.0" +
                        ".kf4-001/kxml-LIB-2.3.0.kf4-001.jar"),
                SCRHelloProvider(),
                iPOJOHelloConsumer()
        };
    }

    @Test
    public void test() {
        if (! isKnopflerfish()) {
            System.out.println("Test ignored - running only on Knopflerfish");
            return;
        }
        CheckService checker = osgiHelper.getServiceObject(CheckService.class);
        assertThat(checker).isNotNull();
        assertThat(checker.data().get("result")).isEqualTo("hello john doe");
    }
}
