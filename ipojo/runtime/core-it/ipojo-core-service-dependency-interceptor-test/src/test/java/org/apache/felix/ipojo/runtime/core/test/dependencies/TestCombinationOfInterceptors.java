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
 * Checks the combination of tracking and ranking interceptors.
 *
 * First, the tracking interceptor selects the references, it also transforms them.
 * Then, the ranking interceptor sorts the remaining references.
 */
public class TestCombinationOfInterceptors extends Common {

    private ComponentInstance provider1;
    private ComponentInstance provider2;
    private ComponentInstance provider3;
    private ComponentInstance provider4;
    private ComponentInstance provider5;
    private ComponentInstance provider6;

    @Test
    public void testCombination() {

        provider1 = provider(0);
        provider2 = provider(1);
        provider3 = provider(2);
        provider4 = provider(3);
        provider5 = provider(4);
        provider6 = provider(5);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".TrackerAndRankerInterceptor", configuration);

        // Create the FooConsumer
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooConsumer");

        // Check we are using provider 2
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(5);

        provider6.dispose();

        assertThat(check.getProps().get("grade")).isEqualTo(4);

        // Change range
        Setter setter = osgiHelper.getServiceObject(Setter.class);
        setter.set("LOW REVERSE");
        assertThat(check.getProps().get("grade")).isEqualTo(0);
    }

    @Test
    public void testAdvanced() {

        provider1 = provider(0);
        provider2 = provider(0);
        provider3 = provider(1);
        provider4 = provider(1);
        provider5 = provider(2);
        provider6 = provider(2);

        // Create the interceptor
        Properties configuration = new Properties();
        configuration.put("target", "(dependency.id=foo)");
        ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.interceptors" +
                ".AdvancedTrackerAndRankerInterceptor", configuration);

        // Create the FooConsumer
        ComponentInstance instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test" +
                ".components.AdvancedFooConsumer");

        // Check we are using provider 0
        osgiHelper.waitForService(CheckService.class.getName(), null, 1000, true);
        CheckService check = osgiHelper.getServiceObject(CheckService.class);
        assertThat(check.getProps().get("grade")).isEqualTo(0);

        Dictionary conf = new Hashtable();
        conf.put("grade", "1");
        instance.reconfigure(conf);

        assertThat(check.getProps().get("grade")).isEqualTo(1);

        conf.put("grade", "2");
        instance.reconfigure(conf);
        assertThat(check.getProps().get("grade")).isEqualTo(2);

        conf.put("grade", "3");
        instance.reconfigure(conf);
        assertThat(check.getProps().get("grade")).isNull();
    }


    private ComponentInstance provider(int i) {
        Dictionary<String, String> configuration = new Hashtable<String, String>();
        configuration.put("grade", Integer.toString(i));
        return ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.test.components.FooProvider",
                configuration);
    }
}
