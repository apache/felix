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
package org.apache.felix.cm.integration;


import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.impl.Activator;
import org.apache.felix.cm.impl.RequiredConfigurationPluginTracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPlugin;


@RunWith(JUnit4TestRunner.class)
public class ActivationTest extends ConfigurationTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }

    @Override
    protected Option[] additionalConfiguration() {
        return options(frameworkProperty(Activator.CM_CONFIG_PLUGINS).value("p1"));
    }

    @Test
    public void test_activation() throws IOException
    {
        // plugin p1 not registered, CA must not be available
        checkNoConfigurationAdmin();

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RequiredConfigurationPluginTracker.PROPERTY_NAME, "p1");

        final ServiceRegistration<ConfigurationPlugin> reg = this.bundleContext
                .registerService(ConfigurationPlugin.class, new ConfigurationPlugin() {

                    @Override
                    public void modifyConfiguration(ServiceReference<?> reference,
                            Dictionary<String, Object> properties) {
                    }
                }, props);

        // p1 is registered now
        this.getConfigurationAdmin();

        // unregister p1
        reg.unregister();

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
        }
        // no p1 registered
        checkNoConfigurationAdmin();
    }

    @Test
    public void test_no_activation() throws IOException {
        // plugin p1 not registered, CA must not be available
        checkNoConfigurationAdmin();

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(RequiredConfigurationPluginTracker.PROPERTY_NAME, "p2");

        final ServiceRegistration<ConfigurationPlugin> reg = this.bundleContext
                .registerService(ConfigurationPlugin.class, new ConfigurationPlugin() {

            @Override
            public void modifyConfiguration(ServiceReference<?> reference, Dictionary<String, Object> properties) {
            }
        }, props);

        // p1 still not registered
        checkNoConfigurationAdmin();

        // unregister p2
        reg.unregister();

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
        }
        // no p1 registered
        checkNoConfigurationAdmin();
    }

    private void checkNoConfigurationAdmin() {
        ConfigurationAdmin ca = null;
        try {
            ca = this.configAdminTracker.waitForService(3000L);
        } catch (InterruptedException e) {
            // ignore
        }
        assertNull(ca);
    }
}
