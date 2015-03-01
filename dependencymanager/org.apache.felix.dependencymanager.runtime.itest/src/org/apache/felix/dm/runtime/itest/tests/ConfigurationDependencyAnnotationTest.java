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

import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.ConfigurationDependencyAnnotation.ConfigurableComponent;
import org.apache.felix.dm.runtime.itest.components.ConfigurationDependencyAnnotation.ConfigurableComponentWithDynamicExtraConfiguration;
import org.junit.Assert;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyAnnotationTest extends TestBase {

    private final static int MAXWAIT = 5000;

    
    /**
     * Tests the ConfigurationDependency annotation.
     */
    @SuppressWarnings({ "serial", "rawtypes", "unchecked" })
    public void testConfigurationDependencyAnnotation() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ConfigurableComponent.ENSURE);
        ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        try {
            org.osgi.service.cm.Configuration cf = cm.getConfiguration(ConfigurableComponent.class.getName(), null);
            cf.update(new Hashtable() {
                {
                    put("foo", "bar");
                }
            });
            e.waitForStep(1, MAXWAIT);
            cf.delete();
            e.waitForStep(3, MAXWAIT);
            e.ensure();
            sr.unregister();
        } catch (IOException err) {
            err.printStackTrace();
            Assert.fail("can't create factory configuration");
        }
    }
    
    /**
     * Tests a Component two ConfigurationDependency (the second one is "instance bound" 
     * and its pid is declared from the init method).
     */
   @SuppressWarnings({ "serial", "rawtypes", "unchecked" })
    public void testConfigurationDependencyWithAnotherExtraDynamicConfigurationAnnotation() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ConfigurableComponentWithDynamicExtraConfiguration.ENSURE);
        ConfigurationAdmin cm = (ConfigurationAdmin) context.getService(context.getServiceReference(ConfigurationAdmin.class.getName()));
        try {
            org.osgi.service.cm.Configuration cf = cm.getConfiguration(ConfigurableComponentWithDynamicExtraConfiguration.class.getName(), null);
            cf.update(new Hashtable() {
                {
                    put("foo", "bar");
                    put("dynamicPid", "dynamicPid"); // Pid of the second Configuration Dependency.
                }
            });
            org.osgi.service.cm.Configuration extraCf = cm.getConfiguration("dynamicPid", null);
            extraCf.update(new Hashtable() {
                {
                    put("foo2", "bar2");
                }
            });

            e.waitForStep(4, MAXWAIT);
            cf.delete();
            extraCf.delete();
            e.waitForStep(5, MAXWAIT);
            e.ensure();
            sr.unregister();
        } catch (IOException err) {
            err.printStackTrace();
            Assert.fail("can't create factory configuration");
        }
    }

}
