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
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestModifyDependencies extends Common {

    ComponentInstance instance2, instance3, instance4, instance5, instance7, instance8;
    ComponentInstance fooProvider;

    @Before
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name", "FooProvider");
            fooProvider = ipojoHelper.getFactory("FooProviderType-Updatable").createComponentInstance(prov);
            fooProvider.stop();

            Properties i2 = new Properties();
            i2.put("instance.name", "Void");
            instance2 = ipojoHelper.getFactory("VoidModifyCheckServiceProvider").createComponentInstance(i2);

            Properties i3 = new Properties();
            i3.put("instance.name", "Object");
            instance3 = ipojoHelper.getFactory("ObjectModifyCheckServiceProvider").createComponentInstance(i3);

            Properties i4 = new Properties();
            i4.put("instance.name", "Ref");
            instance4 = ipojoHelper.getFactory("RefModifyCheckServiceProvider").createComponentInstance(i4);

            Properties i5 = new Properties();
            i5.put("instance.name", "Both");
            instance5 = ipojoHelper.getFactory("BothModifyCheckServiceProvider").createComponentInstance(i5);

            Properties i7 = new Properties();
            i7.put("instance.name", "Map");
            instance7 = ipojoHelper.getFactory("MapModifyCheckServiceProvider").createComponentInstance(i7);

            Properties i8 = new Properties();
            i8.put("instance.name", "Dictionary");
            instance8 = ipojoHelper.getFactory("DictModifyCheckServiceProvider").createComponentInstance(i8);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @After
    public void tearDown() {
        instance2.dispose();
        instance3.dispose();
        instance4.dispose();
        instance5.dispose();
        instance7.dispose();
        instance8.dispose();
        fooProvider.dispose();
        instance2 = null;
        instance3 = null;
        instance4 = null;
        instance5 = null;
        instance7 = null;
        instance8 = null;
        fooProvider = null;
    }

    @Test public void testVoid() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        Object o = osgiHelper.getServiceObject(cs_ref);
        CheckService cs = (CheckService) o;
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1 (" + ((Integer) props.get("voidB")).intValue() + ")", ((Integer) props.get("voidB")).intValue(), 1);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1", ((Integer) props.get("modified")).intValue(), 1); // Already called inside the method


        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1.1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1.1 (" + ((Integer) props.get("voidB")).intValue() + ")", ((Integer) props.get("voidB")).intValue(), 1);
        assertEquals("check void unbind callback invocation -1.1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1.1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1.1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1.1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1.1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1.1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1.1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3); // 1 (first foo) + 1 (our foo) + 1 (check foo)
        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);

    }

    @Test public void testObject() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 1);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer) props.get("modified")).intValue() + ")", ((Integer) props.get("modified")).intValue(), 1);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3);


        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);
    }

    @Test public void testRef() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance4.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance4.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 1);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer) props.get("modified")).intValue() + ")", ((Integer) props.get("modified")).intValue(), 1);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3);

        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);
    }

    @Test public void testBoth() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 1);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer) props.get("modified")).intValue() + ")", ((Integer) props.get("modified")).intValue(), 1);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3);

        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);
    }


    @Test public void testMap() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance7.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance7.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation -1", ((Integer) props.get("mapB")).intValue(), 1);
        assertEquals("check map unbind callback invocation -1", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation -1", ((Integer) props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation -1", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer) props.get("modified")).intValue() + ")", ((Integer) props.get("modified")).intValue(), 1);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3);

        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);
    }

    @Test public void testDict() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance8.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance8.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean) props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer) props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer) props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer) props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer) props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer) props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer) props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer) props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer) props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation -1", ((Integer) props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation -1", ((Integer) props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation -1", ((Integer) props.get("dictB")).intValue(), 1);
        assertEquals("check dict unbind callback invocation -1", ((Integer) props.get("dictU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer) props.get("modified")).intValue() + ")", ((Integer) props.get("modified")).intValue(), 1);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);

        fs.foo(); // Update

        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer) props.get("modified")).intValue(), 3);

        fooProvider.stop();

        id = ((Architecture) osgiHelper.getServiceObject(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);

        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);
    }

}
