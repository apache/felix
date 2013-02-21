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
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestMethodDelayedMultipleDependencies extends Common {

    ComponentInstance instance3, instance4, instance5, instance6, instance7;
    ComponentInstance fooProvider1, fooProvider2;

    @Before
    public void setUp() {
        try {

            Properties i3 = new Properties();
            i3.put("instance.name", "Object");
            instance3 = ipojoHelper.getFactory("MObjectMultipleCheckServiceProvider").createComponentInstance(i3);
            instance3.stop();

            Properties i4 = new Properties();
            i4.put("instance.name", "Ref");
            instance4 = ipojoHelper.getFactory("MRefMultipleCheckServiceProvider").createComponentInstance(i4);
            instance4.stop();

            Properties i5 = new Properties();
            i5.put("instance.name", "Both");
            instance5 = ipojoHelper.getFactory("MBothMultipleCheckServiceProvider").createComponentInstance(i5);
            instance5.stop();

            Properties i6 = new Properties();
            i6.put("instance.name", "Map");
            instance6 = ipojoHelper.getFactory("MMapMultipleCheckServiceProvider").createComponentInstance(i6);
            instance6.stop();

            Properties i7 = new Properties();
            i7.put("instance.name", "Dict");
            instance7 = ipojoHelper.getFactory("MDictMultipleCheckServiceProvider").createComponentInstance(i7);
            instance7.stop();

            Properties prov = new Properties();
            prov.put("instance.name", "FooProvider1");
            fooProvider1 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov);

            Properties prov2 = new Properties();
            prov2.put("instance.name", "FooProvider2");
            fooProvider2 = ipojoHelper.getFactory("FooProviderType-1").createComponentInstance(prov2);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        instance3.dispose();
        instance4.dispose();
        instance5.dispose();
        instance6.dispose();
        instance7.dispose();
        fooProvider1.dispose();
        fooProvider2.dispose();
        instance3 = null;
        instance4 = null;
        instance5 = null;
        instance6 = null;
        instance7 = null;
        fooProvider1 = null;
        fooProvider2 = null;
    }

    @Test public void testObject() {
        instance3.start();

        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 2);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

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
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        instance3.stop();
    }

    @Test public void testRef() {
        instance4.start();
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance4.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance4.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer) props.get("refB")).intValue(), 2);
        assertEquals("check ref unbind callback invocation - 1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

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
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance4.stop();
        getContext().ungetService(cs_ref);
    }

    @Test public void testBoth() {
        instance5.start();
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer) props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer) props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer) props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 1", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer) props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 3", ((Integer) props.get("bothU")).intValue(), 1);
        assertEquals("check map bind callback invocation - 3", ((Integer) props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 3", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 3", ((Integer) props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 3", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 3", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance5.stop();
        getContext().ungetService(cs_ref);
    }

    @Test public void testMap() {
        instance6.start();
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance6.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance6.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer) props.get("mapB")).intValue(), 2);
        assertEquals("check map unbind callback invocation - 1", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer) props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 1", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 3", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 3", ((Integer) props.get("mapB")).intValue(), 2);
        assertEquals("check map unbind callback invocation - 3", ((Integer) props.get("mapU")).intValue(), 1);
        assertEquals("check dict bind callback invocation - 3", ((Integer) props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 3", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 3", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance6.stop();
        getContext().ungetService(cs_ref);
    }

    @Test public void testDict() {
        instance7.start();
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance7.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance7.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean) props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer) props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer) props.get("dictB")).intValue(), 2);
        assertEquals("check dict unbind callback invocation - 1", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer) props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long) props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double) props.get("double")).doubleValue(), 2.0, 0);

        fooProvider1.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);

        cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean) props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 3", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 3", ((Integer) props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 3", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 3", ((Integer) props.get("dictB")).intValue(), 2);
        assertEquals("check dict unbind callback invocation - 3", ((Integer) props.get("dictU")).intValue(), 1);
        assertEquals("Check FS invocation (int) - 3", ((Integer) props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long) props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double) props.get("double")).doubleValue(), 1.0, 0);

        fooProvider2.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance7.stop();
        getContext().ungetService(cs_ref);
    }


}
