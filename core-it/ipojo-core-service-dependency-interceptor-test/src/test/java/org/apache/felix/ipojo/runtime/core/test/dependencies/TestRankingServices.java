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
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks Tracking interceptor transforming services
 */
public class TestRankingServices extends Common {

    private ComponentInstance provider1;
    private ComponentInstance provider2;

    @Test
    public void testRanking() {

        // Provider with grade 0 first
        provider1 = provider(0);
        provider2 = provider(1);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".FilterRankingInterceptor", configuration);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        // Check we are using provider 2
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(1);

        provider2.dispose();

        assertThat(check.getProps().get("grade")).isEqualTo(0);
    }

    @Test
    public void testRankingWhenInterceptorIsComingAfterTheBattle() {

        // Provider with grade 0 first
        provider1 = provider(0);
        provider2 = provider(1);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(0);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".FilterRankingInterceptor", configuration);

        // Check we are using provider 2
        assertThat(check.getProps().get("grade")).isEqualTo(1);

        provider2.dispose();

        assertThat(check.getProps().get("grade")).isEqualTo(0);
    }

    @Test
    public void testRankingChanges() {

        // Provider with grade 0 first
        provider1 = provider(0);
        provider2 = provider(1);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".FilterRankingInterceptor", configuration);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        // Check we are using provider 2
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(1);

        Setter setter = osgiHelper.getServiceObject(Setter.class);
        setter.set("true");

        assertThat(check.getProps().get("grade")).isEqualTo(0);
    }

    @Test
    public void testRestorationOfTheComparatorWhenTheInterceptorLeaves() {

        // Provider with grade 0 first
        provider1 = provider(0);
        provider2 = provider(1);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".test.interceptors.FilterRankingInterceptor", configuration);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        // Check we are using provider 2
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(1);

        interceptor.dispose();

        assertThat(check.getProps().get("grade")).isEqualTo(0);
    }

    @Test
    public void testDynamicServices() {

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".FilterRankingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance consumer = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.INVALID);

        provider1 = provider(0);

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.VALID);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(0);

        provider2 = provider(1);

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.VALID);
        check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(1);

        provider2.dispose();

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.VALID);
        check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(0);

        provider1.dispose();

        assertThat(consumer.getState()).isEqualTo(ComponentInstance.INVALID);
    }

    @Test
    public void testArchitecture() {
        // Provider with grade 0 first
        provider1 = provider(0);
        provider2 = provider(1);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ComponentInstance interceptor = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core" +
                ".test.interceptors.FilterRankingInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.FooConsumer");

        // Check we are using provider 2
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);

        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("servicerankinginterceptor");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("target=\"(dependency.id=foo)\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("instance.name=\"" + interceptor.getInstanceName() + "\"");
        assertThat(instance.getInstanceDescription().getDescription().toString()).contains
                ("bundle.id=\"" + getTestBundle().getBundleId() + "\"");
    }

    private ComponentInstance provider(int i) {
        Dictionary<String, String> configuration = new Hashtable<String, String>();
        configuration.put("grade", Integer.toString(i));
        return ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooProvider",
                configuration);
    }
}
