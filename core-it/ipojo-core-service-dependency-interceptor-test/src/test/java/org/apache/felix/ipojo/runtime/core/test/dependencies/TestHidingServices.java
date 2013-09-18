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

package org.apache.felix.ipojo.runtime.core.test.dependencies;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks Tracking interceptor hiding services
 */
public class TestHidingServices extends Common {

    private ComponentInstance provider;

    @Before
    public void setUp() {
        provider = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooProvider");
    }

    @Test
    public void testHidingServiceAndReconfiguration() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".HidingTrackingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        // The provider is rejected => Invalid instance
        assertThat(instance.getState()).isEqualTo(ComponentInstance.INVALID);

        Setter setter = osgiHelper.getServiceObject(Setter.class, null);
        setter.set("toto");

        // The provider is now accepted
        assertThat(instance.getState()).isEqualTo(ComponentInstance.VALID);
    }

    /**
     * Same as previous but the interceptor arrives after the instance.
     */
    @Test
    public void testHidingAServiceAfterItsBinding() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");


        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        assertThat(instance.getState()).isEqualTo(ComponentInstance.VALID);

        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".interceptors.HidingTrackingInterceptor", configuration);

        // The provider is rejected => Invalid instance
        assertThat(instance.getState()).isEqualTo(ComponentInstance.INVALID);

        Setter setter = osgiHelper.getServiceObject(Setter.class, null);
        setter.set("toto");

        // The provider is now accepted
        assertThat(instance.getState()).isEqualTo(ComponentInstance.VALID);

        setter.set("hidden");

        // The provider is rejected => Invalid instance
        assertThat(instance.getState()).isEqualTo(ComponentInstance.INVALID);

        interceptor.dispose();

        // The provider is now accepted
        assertThat(instance.getState()).isEqualTo(ComponentInstance.VALID);
    }

    @Test
    public void testArchitecture() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");


        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        assertThat(instance.getState()).isEqualTo(ComponentInstance.VALID);

        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".interceptors.HidingTrackingInterceptor", configuration);

        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("servicetrackinginterceptor");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("target=\"(dependency.id=foo)\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("instance.name=\"" + interceptor.getInstanceName() + "\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("bundle.id=\"" + getTestBundle().getBundleId() + "\"");
    }


}
