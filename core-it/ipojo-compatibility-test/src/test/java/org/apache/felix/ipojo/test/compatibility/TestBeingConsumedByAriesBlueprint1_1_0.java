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
import static org.ops4j.pax.exam.CoreOptions.maven;

/**
 * Check a configuration using Aries Blueprint 1.0.1
 * iPOJO provider is consumed by a component using Blueprint.
 *
 * This test is intended to pass on Felix, KF and Equinox
 */
public class TestBeingConsumedByAriesBlueprint1_1_0 extends Common {

    public static final String BP_ARTIFACTID = "org.apache.aries.blueprint";
    public static final String BP_VERSION = "1.1.0";
    public static final String BP_GROUPID = "org.apache.aries.blueprint";


    @Override
    public Option[] bundles() {
        return new Option[] {
                bundle(maven(BP_GROUPID, BP_ARTIFACTID, BP_VERSION).getURL()),
                bundle(maven("org.apache.aries.proxy", "org.apache.aries.proxy", "1.0.1").getURL()),
                bundle(maven("org.apache.aries", "org.apache.aries.util", "1.1.0").getURL()),
                BPHelloConsumer(),
                iPOJOHelloProvider()
        };
    }

    @Test
    public void test() {
        CheckService checker = osgiHelper.waitForService(CheckService.class, null, 5000, false);
        assertThat(checker).isNotNull();
        assertThat(checker.data().get("result")).isEqualTo("hello john doe");
    }
}
