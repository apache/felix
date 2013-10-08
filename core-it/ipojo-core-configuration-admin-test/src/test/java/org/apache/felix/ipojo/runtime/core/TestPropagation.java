/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * Test for FELIX-4207 - ipojo @Component with propagation set to true doesn't propagate properties
 */
public class TestPropagation extends Common {

    private String factoryName = "org.apache.felix.ipojo.runtime.core.components.ConfigurableFooProviderWithPropagation";

    @Test
    public void testPropagationFromConfigurationAdminWhenCreatingTheInstance() throws IOException {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("message", "message");
        props.put("propagated", "propagated");
        props.put(".private", "wow");

        Configuration configuration = admin.createFactoryConfiguration(factoryName, "?");
        configuration.update(props);

        ServiceReference ref = osgiHelper.waitForService(FooService.class.getName(),
                "(instance.name=" + configuration.getPid() + ")",
                1000);

        // Check the propagation
        assertEquals(ref.getProperty("propagated"), "propagated");
        assertEquals(ref.getProperty("message"), "message");
        assertNull(ref.getProperty(".private"));
        assertNull(ref.getProperty("private"));

        // Check the the .private property has the right value
        ConfigurationHandlerDescription desc = (ConfigurationHandlerDescription) ipojoHelper.getArchitectureByName(configuration.getPid())
                .getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        PropertyDescription prop = desc.getPropertyByName(".private");
        assertEquals(prop.getValue(), "wow");

        // Update the property
        props.put("message", "message2");
        props.put("propagated", "propagated2");
        props.put(".private", "wow2");
        configuration.update(props);

        grace();

        ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), configuration.getPid());
        // Check the propagation
        assertEquals(ref.getProperty("propagated"), "propagated2");
        assertEquals(ref.getProperty("message"), "message2");

        desc = (ConfigurationHandlerDescription) ipojoHelper.getArchitectureByName(configuration.getPid())
                .getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        prop = desc.getPropertyByName(".private");
        assertEquals(prop.getValue(), "wow2");

        configuration.delete();
    }


}
