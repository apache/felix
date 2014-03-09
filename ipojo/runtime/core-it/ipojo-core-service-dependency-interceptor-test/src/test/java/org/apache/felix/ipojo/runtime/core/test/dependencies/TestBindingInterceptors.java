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
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks binding interceptors.
 */
public class TestBindingInterceptors extends Common {

    private ComponentInstance provider;

    @Before
    public void setUp() {
        provider = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooProvider");
    }

    @Test
    public void testProxyBindingInterceptorBeforeInstanceCreation() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".ProxyBindingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components" +
                ".FooConsumer");

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() + ")",
                1000, true);
        CheckService check = (CheckService) osgiHelper.getRawServiceObject(ref);

        assertThat(check.check());

        // Extract monitored data
        CheckService checkService = osgiHelper.getServiceObject(CheckService.class,
                "(factory.name=org.apache.felix.ipojo.runtime.core.test.interceptors.ProxyBindingInterceptor)");

        assertThat(checkService).isNotNull();
        assertThat(checkService.getProps().get("bound")).isEqualTo(1);
        assertThat(checkService.getProps().get("foo")).isEqualTo(1);

        provider.dispose();

        assertThat(checkService.getProps().get("bound")).isEqualTo(1);
        assertThat(checkService.getProps().get("unbound")).isEqualTo(1);
    }

    /**
     * Same as previous but the interceptor arrives after the instance.
     */
    @Test
    public void testProxyBindingInterceptorAfterInstanceCreation() {
        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components" +
                ".FooConsumer");

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() + ")",
                1000, true);
        CheckService check = (CheckService) osgiHelper.getRawServiceObject(ref);

        assertThat(check.check());

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".ProxyBindingInterceptor", configuration);

        // Extract monitored data
        CheckService checkService = osgiHelper.getServiceObject(CheckService.class,
                "(factory.name=org.apache.felix.ipojo.runtime.core.test.interceptors.ProxyBindingInterceptor)");

        // Nothing was intercepted.
        assertThat(checkService).isNotNull();
        assertThat(checkService.getProps().get("bound")).isNull();
        assertThat(checkService.getProps().get("foo")).isNull();

        // Force rebinding.
        provider.stop();
        provider.start();

        check.check();

        // Things should have been intercepted
        assertThat(checkService).isNotNull();
        assertThat(checkService.getProps().get("bound")).isEqualTo(1);
        assertThat(checkService.getProps().get("foo")).isEqualTo(1);

        provider.dispose();

        // Two unget calls, as we intercepted the first one (provider.stop()).
        assertThat(checkService.getProps().get("unbound")).isEqualTo(2);
    }

    /**
     * Checks that two interceptors are called sequentially.
     */
    @Test
    public void testWithTwoInterceptors() {
        // First, only one interceptor.

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".ProxyBindingInterceptor", configuration);


        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components" +
                ".FooConsumer");

        ServiceReference ref = osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() + ")",
                1000, true);
        CheckService check = (CheckService) osgiHelper.getRawServiceObject(ref);

        assertThat(check.check());

        // Create the second interceptor, but it's too late to modify the first binding.
        configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".EnhancingBindingInterceptor", configuration);

        assertThat(check.getProps().get("enhanced")).isNull();

        // Extract monitored data
        CheckService checkService = osgiHelper.getServiceObject(CheckService.class,
                "(factory.name=org.apache.felix.ipojo.runtime.core.test.interceptors.ProxyBindingInterceptor)");

        assertThat(checkService).isNotNull();
        assertThat(checkService.getProps().get("bound")).isEqualTo(1);
        assertThat(checkService.getProps().get("foo")).isEqualTo(1);

        // Force re-binding.
        provider.stop();
        provider.start();

        assertThat(check.getProps().get("enhanced")).isNotNull();
    }

    @Test
    public void testArchitecture() {
        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".interceptors.ProxyBindingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        osgiHelper.waitForService(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() + ")",
                1000, true);

        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("servicebindinginterceptor");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("target=\"(dependency.id=foo)\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("instance.name=\"" + interceptor.getInstanceName() + "\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("bundle.id=\"" + getTestBundle().getBundleId() + "\"");
    }

}
