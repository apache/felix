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
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks Tracking interceptor transforming services
 */
public class TestTransformingServices extends Common {

    private ComponentInstance provider;

    @Before
    public void setUp() {
        provider = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooProvider");
    }

    @Test
    public void testTransformationOfFoo() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".AddLocationTrackingInterceptor", configuration);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.check());
        @SuppressWarnings("unchecked") Map<String, ?> props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();
    }

    /**
     * Same as previous but the interceptor arrives after the instance.
     */
    @Test
    public void testDelayedTransformationOfFoo() {
        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components" +
                ".FooConsumer");

        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.check());
        @SuppressWarnings("unchecked") Map<String, ?> props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isNull();
        assertThat(props.get("hidden")).isNotNull();

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".AddLocationTrackingInterceptor", configuration);

        assertThat(check.check());
        props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();
    }

    /**
     * The interceptor makes the instance valid.
     */
    @Test
    public void testTransformationMakingFilterMatch() {
        // Create the FooConsumer
        Properties configuration = new Properties();
        Properties filters = new Properties();
        filters.put("foo", "(location=kitchen)");
        configuration.put("requires.filters", filters);
        ComponentInstance consumer = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer", configuration);

        // Invalid instance
        assertThat(consumer.getInstanceDescription().getState()).isEqualTo(ComponentInstance.INVALID);

        // Create the interceptor
        Properties config = new Properties();
        config.put("target", "(dependency.id=foo)");
        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".interceptors.AddLocationTrackingInterceptor", config);

        assertThat(consumer.getInstanceDescription().getState()).isEqualTo(ComponentInstance.VALID);

        CheckService check = osgiHelper.getServiceObject(CheckService.class);

        assertThat(check.check());
        Map<String, ?> props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();

        // Removing the interceptor should revert to the base set.
        interceptor.dispose();
        System.out.println(consumer.getInstanceDescription().getDescription());
        assertThat(consumer.getInstanceDescription().getState()).isEqualTo(ComponentInstance.INVALID);
    }

    /**
     * Checks the behavior when services arrives and leaves.
     */
    @Test
    public void testTransformationOfDynamicFoo() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".AddLocationTrackingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance consumer = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.check());
        @SuppressWarnings("unchecked") Map<String, ?> props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();

        // Create another provider
        ComponentInstance provider2 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooProvider");

        check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.check());
        props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();

        // Provider 1 leaves
        provider.dispose();

        // The second provider is also transformed.
        check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.check());
        props = (Map<String, ?>) check.getProps().get("props");
        assertThat(props.get("location")).isEqualTo("kitchen");
        assertThat(props.get("hidden")).isNull();

        provider2.dispose();

        System.out.println(consumer.getInstanceDescription().getDescription());

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.INVALID);
    }
}
