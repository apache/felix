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
import org.osgi.framework.ServiceReference;

import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks Tracking interceptor bound to several dependencies.
 */
public class TestInterceptorsOnSeveralDependencies extends Common {

    private ComponentInstance provider;

    @Before
    public void setUp() {
        provider = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooProvider");
    }

    @Test
    public void testBeingBoundToSeveralDependencies() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".interceptors.PropertyTrackingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance1 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        ComponentInstance instance2 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        assertThat(instance1.getState()).isEqualTo(ComponentInstance.VALID);
        assertThat(instance2.getState()).isEqualTo(ComponentInstance.VALID);

        final ServiceReference ref1 = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(),
                instance1.getInstanceName());
        final ServiceReference ref2 = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(),
                instance2.getInstanceName());

        CheckService cs1 = (CheckService) osgiHelper.getRawServiceObject(ref1);
        CheckService cs2 = (CheckService) osgiHelper.getRawServiceObject(ref2);

        @SuppressWarnings("unchecked") Map<String, ?> props1 = (Map<String, ?>) cs1.getProps().get("props");
        @SuppressWarnings("unchecked") Map<String, ?> props2 = (Map<String, ?>) cs2.getProps().get("props");
        assertThat(props1.get("location")).isEqualTo("kitchen");
        assertThat(props2.get("location")).isEqualTo("kitchen");

        Setter setter = osgiHelper.getServiceObject(Setter.class, null);
        setter.set("bedroom");

        props1 = (Map<String, ?>) cs1.getProps().get("props");
        props2 = (Map<String, ?>) cs2.getProps().get("props");
        assertThat(props1.get("location")).isEqualTo("bedroom");
        assertThat(props2.get("location")).isEqualTo("bedroom");

        interceptor.dispose();

        props1 = (Map<String, ?>) cs1.getProps().get("props");
        props2 = (Map<String, ?>) cs2.getProps().get("props");
        assertThat(props1.get("location")).isNull();
        assertThat(props2.get("location")).isNull();

    }
}
