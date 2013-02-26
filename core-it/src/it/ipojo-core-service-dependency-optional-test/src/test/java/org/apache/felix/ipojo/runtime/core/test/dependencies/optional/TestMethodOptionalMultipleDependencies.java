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
package org.apache.felix.ipojo.runtime.core.test.dependencies.optional;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestMethodOptionalMultipleDependencies extends Common {

    ComponentInstance instance3, instance4;
    ComponentInstance fooProvider1, fooProvider2;

    @Before
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name", "FooProvider1");
            fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);
            fooProvider1.stop();

            Properties prov2 = new Properties();
            prov2.put("instance.name", "FooProvider2");
            fooProvider2 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov2);
            fooProvider2.stop();

            Properties i3 = new Properties();
            i3.put("instance.name", "Object");
            instance3 = ipojoHelper.getFactory("MObjectOptionalMultipleCheckServiceProvider").createComponentInstance(i3);

            Properties i4 = new Properties();
            i4.put("instance.name", "Ref");
            instance4 = ipojoHelper.getFactory("MRefOptionalMultipleCheckServiceProvider").createComponentInstance(i4);

        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    @After
    public void tearDown() {
        instance3.dispose();
        instance4.dispose();
        fooProvider1.dispose();
        fooProvider2.dispose();
        instance3 = null;
        instance4 = null;
        fooProvider1 = null;
        fooProvider2 = null;
    }

    @Test public void testObject() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation - 0", ((Boolean) props.get("result")).booleanValue()); // False : no provider
        assertEquals("check void bind invocation - 0", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 0", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 0", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 0", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 0", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 0", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 0", ((Integer) props.get("int")).intValue(), 0);
        assertEquals("Check FS invocation (long) - 0", ((Long) props.get("long")).longValue(), 0);
        assertEquals("Check FS invocation (double) - 0", ((Double) props.get("double")).doubleValue(), 0.0, 0);

        fooProvider1.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 1);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 2", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 2", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer) props.get("objectB")).intValue(), 2);
        assertEquals("check object unbind callback invocation - 2", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 2", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 2", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 2", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 2", ((Double) props.get("double")).doubleValue(),  2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer) props.get("objectB")).intValue(), 2);
        assertEquals("check object unbind callback invocation - 3", ((Integer) props.get("objectU")).intValue(), 1);
        assertEquals("check ref bind callback invocation - 3", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 3", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 3", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation - 0", ((Boolean) props.get("result")).booleanValue()); // False : no provider
        assertEquals("check void bind invocation - 0", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 0", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 0", ((Integer) props.get("objectB")).intValue(), 2);
        assertEquals("check object unbind callback invocation - 0", ((Integer) props.get("objectU")).intValue(), 2);
        assertEquals("check ref bind callback invocation - 0", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 0", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 0", ((Integer) props.get("int")).intValue(), 0);
        assertEquals("Check FS invocation (long) - 0", ((Long) props.get("long")).longValue(), 0);
        assertEquals("Check FS invocation (double) - 0", ((Double) props.get("double")).doubleValue(), 0.0, 0);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
    }

    @Test public void testRef() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance4.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance4.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation - 0", ((Boolean) props.get("result")).booleanValue()); // False : no provider
        assertEquals("check void bind invocation - 0", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 0", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 0", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 0", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 0", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 0", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 0", ((Integer) props.get("int")).intValue(), 0);
        assertEquals("Check FS invocation (long) - 0", ((Long) props.get("long")).longValue(), 0);
        assertEquals("Check FS invocation (double) - 0", ((Double) props.get("double")).doubleValue(), 0.0, 0);

        fooProvider1.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer) props.get("refB")).intValue(), 1);
        assertEquals("check ref unbind callback invocation - 1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 2", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 2", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 2", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer) props.get("refB")).intValue(), 2);
        assertEquals("check ref unbind callback invocation - 2", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 2", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 2", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 2", ((Double) props.get("double")).doubleValue(),  2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 3", ((Integer) props.get("refB")).intValue(), 2);
        assertEquals("check ref unbind callback invocation - 3", ((Integer) props.get("refU")).intValue(), 1);
        assertEquals("Check FS invocation (int) - 3", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertFalse("check CheckService invocation - 0", ((Boolean) props.get("result")).booleanValue()); // False : no provider
        assertEquals("check void bind invocation - 0", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 0", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 0", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 0", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 0", ((Integer) props.get("refB")).intValue(), 2);
        assertEquals("check ref unbind callback invocation - 0", ((Integer) props.get("refU")).intValue(), 2);
        assertEquals("Check FS invocation (int) - 0", ((Integer) props.get("int")).intValue(), 0);
        assertEquals("Check FS invocation (long) - 0", ((Long) props.get("long")).longValue(), 0);
        assertEquals("Check FS invocation (double) - 0", ((Double) props.get("double")).doubleValue(), 0.0, 0);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
    }


}
