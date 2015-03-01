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
package org.apache.felix.dm.runtime.itest.tests;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.Assert;

import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.FactoryConfigurationAdapterAnnotation.ServiceProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Use case: Verify that an annotated Configuration Factory Adapter Service is properly created when a factory configuration
 * is created from Config Admin.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryConfigurationAdapterAnnotationTest extends TestBase {
    
    private final static int MAXWAIT = 5000;

    @SuppressWarnings("serial")
    public void testFactoryConfigurationAdapterAnnotation() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ServiceProvider.ENSURE);
        ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        try {
            // Create a factory configuration in order to instantiate the ServiceProvider
            org.osgi.service.cm.Configuration cf = cm.createFactoryConfiguration("FactoryPidTest", null);
            cf.update(new Hashtable() {
                {
                    put("foo2", "bar2");
                }
            });
            // Wait for the ServiceProvider activation.
            e.waitForStep(2, MAXWAIT);
            // Update conf
            cf.update(new Hashtable() {
                {
                    put("foo2", "bar2_modified");
                }
            });
            // Wait for effective update
            e.waitForStep(4, MAXWAIT);
            // Remove configuration.
            cf.delete();
            // Check if ServiceProvider has been stopped.
            e.waitForStep(6, MAXWAIT);
            e.ensure();
            sr.unregister();
        }
        catch (IOException err) {
            err.printStackTrace();
            Assert.fail("can't create factory configuration");
        }
    }
}
