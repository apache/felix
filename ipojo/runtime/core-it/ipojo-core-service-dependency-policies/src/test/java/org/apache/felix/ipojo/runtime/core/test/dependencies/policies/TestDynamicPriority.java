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
package org.apache.felix.ipojo.runtime.core.test.dependencies.policies;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestDynamicPriority extends Common {

    ComponentInstance instance1, instance3;
    ComponentInstance fooProvider;
    ComponentInstance fooProvider2;

    @Before
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name", "FooProvider-1");
            prov.put("service.ranking", "1");
            fooProvider = ipojoHelper.getFactory("RankedFooProviderType").createComponentInstance(prov);
            fooProvider.stop();

            Properties prov2 = new Properties();
            prov2.put("instance.name", "FooProvider-2");
            prov2.put("service.ranking", "0");
            fooProvider2 = ipojoHelper.getFactory("RankedFooProviderType").createComponentInstance(prov2);
            fooProvider2.stop();

            Properties i1 = new Properties();
            i1.put("instance.name", "Simple");
            instance1 = ipojoHelper.getFactory("DPSimpleCheckServiceProvider").createComponentInstance(i1);

            Properties i3 = new Properties();
            i3.put("instance.name", "Object");
            instance3 = ipojoHelper.getFactory("DPObjectCheckServiceProvider").createComponentInstance(i3);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @After
    public void tearDown() {
        instance1.dispose();
        instance3.dispose();
        fooProvider.dispose();
        fooProvider2.dispose();
        instance1 = null;
        instance3 = null;
        fooProvider = null;
        fooProvider2 = null;
    }

    @Test
    public void testSimple() {
        ServiceReference arch_ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance1.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        fooProvider.start();
        fooProvider2.start();

        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);

        ServiceReference cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        // Check grade
        Integer grade = (Integer) cs.getProps().get("int");
        assertEquals("Check first grade", 1, grade.intValue());

        fooProvider.stop(); // Turn off the best provider.

        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);

        cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check second grade", 0, grade.intValue());

        fooProvider.start(); // Turn on the best provider.

        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);

        cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check third grade", 1, grade.intValue());


        // Increase the second provider grade.
        ServiceReference fs_ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check foo service (2) reference", fs_ref);
        FooService fs = (FooService) getContext().getService(fs_ref);

        fs.foo(); // Increase the grade (now = 2)

        cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check fourth grade", 2, grade.intValue());

        // Increase the other provider grade.
        fs_ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), fooProvider.getInstanceName());
        assertNotNull("Check foo service (1) reference", fs_ref);
        fs = (FooService) getContext().getService(fs_ref);
        fs.foo(); //(grade = 3)

        cs_ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check fifth grade", 3, grade.intValue());

        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(fs_ref);
        fooProvider.stop();
        fooProvider2.stop();
    }
}
