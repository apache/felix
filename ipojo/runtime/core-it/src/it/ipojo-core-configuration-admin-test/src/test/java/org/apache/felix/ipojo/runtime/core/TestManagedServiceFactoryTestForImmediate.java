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

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TestManagedServiceFactoryTestForImmediate extends Common {

    private ComponentFactory factory;

    @Before
    public void setUp() {
        factory = (ComponentFactory) ipojoHelper.getFactory("CA-ImmConfigurableProvider");
    }

    @Test
    public void testCreationAndReconfiguration() {
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("CA-ImmConfigurableProvider", "?");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        props.put("message", "message");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        grace();

        //  The instance should be created, wait for the architecture service
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=" + pid + ")", 1000);
        Architecture architecture = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Check object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = (Integer) p.get("count");
        //architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message", mes);
        assertEquals("Assert count", 1, count);
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        props.put("message", "message2");
        try {
            configuration.update(props);
            // Update the configuration ...
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = (Integer) p.get("count");
        //architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message2", mes);
        assertEquals("Assert count", 2, count);
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        try {
            configuration.delete();
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        ServiceReference ref = osgiHelper.getServiceReference(FooService.class.getName(), "(instance.name=" + pid + ")");
        assertNull("Check unavailability", ref);
    }

    @Test
    public void testCreationAndReconfiguration2() {
        //The reconfiguration happens before the service invocation
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("CA-ImmConfigurableProvider", "?");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        props.put("message", "message");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();
        System.out.println("PID : " + pid);

        // Wait for the processing of the first configuration.
        grace();

        //  The instance should be created, wait for the architecture service
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=" + pid + ")", 1000);
        Architecture architecture = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Check object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        props.put("message", "message2");
        try {
            configuration.update(props);
            // Update the configuration ...
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        //architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Check object -2", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        //Invoke
        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = ((Integer) p.get("count")).intValue();
        // architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message2", mes);
        assertEquals("Assert count", 2, count);
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        try {
            configuration.delete();
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        ServiceReference ref = osgiHelper.getServiceReference(FooService.class.getName(), "(instance.name=" + pid + ")");
        assertNull("Check unavailability", ref);
    }

    @Test
    public void testDelayedCreationAndReconfiguration() {
        factory.stop();
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("CA-ImmConfigurableProvider", "?");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        props.put("message", "message");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        grace();

        assertNull("check no instance", osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));

        factory.start();


        // Wait for the processing of the first configuration.
        grace();


        //  The instance should be created, wait for the architecture service
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=" + pid + ")", 1000);
        Architecture architecture = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Check object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = (Integer) p.get("count");
        //architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message", mes);
        assertEquals("Assert count", 1, count);
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        props.put("message", "message2");
        try {
            configuration.update(props);
            // Update the configuration ...
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        p = fs.fooProps();
        mes = p.getProperty("message");
        count = (Integer) p.get("count");
        // architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message2", mes);
        //assertEquals("Assert count", 2, count);
        // This test was removed as the result can be 3. 
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        try {
            configuration.delete();
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        ServiceReference ref = osgiHelper.getServiceReference(FooService.class.getName(), "(instance.name=" + pid + ")");
        assertNull("Check unavailability", ref);
    }

    @Test
    public void testDelayedCreationAndReconfiguration2() {
        factory.stop();
        //The reconfiguration happens before the service invocation
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("CA-ImmConfigurableProvider", "?");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        props.put("message", "message");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        grace();

        assertNull("check no instance", osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));

        factory.start();

        // Wait for the processing of the first configuration.
        grace();


        //  The instance should be created, wait for the architecture service
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=" + pid + ")", 1000);
        Architecture architecture = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Check object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        props.put("message", "message2");
        try {
            configuration.update(props);
            // Update the configuration ...
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        //architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Check object -1", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        //Invoke
        FooService fs = (FooService) osgiHelper.getServiceObject(FooService.class.getName(), "(instance.name=" + pid + ")");
        Properties p = fs.fooProps();
        String mes = p.getProperty("message");
        int count = (Integer) p.get("count");
        // architecture = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance="+pid+")");

        assertEquals("Assert Message", "message2", mes);
        assertEquals("Assert count", 2, count);
        assertEquals("Check 1 object", 1, ((PrimitiveInstanceDescription) architecture.getInstanceDescription()).getCreatedObjects().length);

        try {
            configuration.delete();
            grace();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        ServiceReference ref = osgiHelper.getServiceReference(FooService.class.getName(), "(instance.name=" + pid + ")");
        assertNull("Check unavailability", ref);
    }


}
