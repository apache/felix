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

package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.runtime.core.components.FooProviderType1;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * Check the different method to create POJO object.
 */
public class TestPOJOCreation extends BaseTest {

    private ComponentInstance ci_lazzy;
    private ComponentInstance ci_immediate;
    private ComponentInstance ci_immediate_singleton;

    private Architecture lazzyArch;
    private Architecture immeArch;
    private Architecture immeArchSing;


    private ComponentInstance ci_lazzy_sing;
    private ComponentInstance ci_lazzy_sev;

    private Architecture lazzyArchSing;
    private Architecture lazzyArchSev;
    private ComponentInstance ci_lazzy_singM;
    private ComponentInstance ci_lazzy_sevM;

    private Architecture lazzyArchSingM;
    private Architecture lazzyArchSevM;

    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }

    @Before
    public void setUp() {
        String factName = "ManipulationCreation-FooProviderType-1";
        String compName = "FooProvider-1";
        ci_lazzy = ipojoHelper.createComponentInstance(factName, compName);

        String factName2 = "ManipulationCreation-ImmediateFooProviderType";
        String compName2 = "FooProvider-2";
        ci_immediate = ipojoHelper.createComponentInstance(factName2, compName2);

        String factName3 = "ManipulationCreation-ImmediateFooProviderTypeSingleton";
        String compName3 = "FooProvider-3";
        ci_immediate_singleton = ipojoHelper.createComponentInstance(factName3, compName3);

        String factName4 = "ManipulationCreation-FooProviderType-1-Sing";
        String compName4 = "FooProvider-1-Sing";
        ci_lazzy_sing = ipojoHelper.createComponentInstance(factName4, compName4);

        String factName5 = "ManipulationCreation-FooProviderType-1-Sev";
        String compName5 = "FooProvider-1-Sev";
        ci_lazzy_sev = ipojoHelper.createComponentInstance(factName5, compName5);

        String factName6 = "ManipulationCreation-FooProviderType-1-SingM";
        String compName6 = "FooProvider-1-SingM";
        ci_lazzy_singM = ipojoHelper.createComponentInstance(factName6, compName6);

        String factName7 = "ManipulationCreation-FooProviderType-1-SevM";
        String compName7 = "FooProvider-1-SevM";
        ci_lazzy_sevM = ipojoHelper.createComponentInstance(factName7, compName7);

        lazzyArch = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName + ")");
        immeArch = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName2 + ")");
        immeArchSing = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName3 + ")");
        lazzyArchSing = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName4 + ")");
        lazzyArchSev = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName5 + ")");
        lazzyArchSingM = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName6 + ")");
        lazzyArchSevM = (Architecture) osgiHelper.getServiceObject(Architecture.class.getName(), "(architecture.instance=" + compName7 + ")");
    }

    public void tearDown() {
        lazzyArch = null;
        immeArch = null;
        immeArchSing = null;
        lazzyArchSing = null;
        lazzyArchSev = null;
        lazzyArchSingM = null;
        lazzyArchSevM = null;
        ipojoHelper.dispose();
    }

    /**
     * Check lazy creation.
     */
    @Test
    public void testLazyCreation() {
        assertEquals("Check that no objects are created ", 0, ((PrimitiveInstanceDescription) lazzyArch.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArch.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check lazy and singleton creation.
     */
    @Test
    public void testLazyCreationSingleton() {
        assertEquals("Check that no objects are created ", 0, ((PrimitiveInstanceDescription) lazzyArchSing.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy_sing.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy_sing.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArchSing.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check lazy and "several" creation.
     */
    @Test
    public void testLazyCreationSeveral() {
        assertEquals("Check that no objects are created ", 0, ((PrimitiveInstanceDescription) lazzyArchSev.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy_sev.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy_sev.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        FooService fs2 = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertTrue("Check the FooService invocation-2", fs2.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArchSev.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check immediate creation.
     */
    @Test
    public void testImmediateCreation() {
        assertEquals("Check that one object is created ", 1, ((PrimitiveInstanceDescription) immeArch.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_immediate.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_immediate.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) immeArch.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check bundle context injection.
     */
    @Test
    public void testBundleContext() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        Properties p = fs.fooProps();
        assertNotNull("Check the bundle context", p.get("context"));
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArch.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Test immediate singleton creation.
     */
    @Test
    public void testImmediateSingletonCreation() {
        assertEquals("Check that one object is created ", 1, ((PrimitiveInstanceDescription) immeArchSing.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_immediate_singleton.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_immediate_singleton.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) immeArchSing.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check creation through a factory method.
     * (lazy & singleton creation)
     */
    @Test
    public void testLazyCreationSingletonM() {
        assertEquals("Check that no objects are created ", 0, ((PrimitiveInstanceDescription) lazzyArchSingM.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy_singM.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy_singM.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        FooService fs2 = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertTrue("Check the FooService invocation", fs2.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArchSingM.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Check creation through a factory method.
     * (lazy & several creation)
     */
    @Test
    public void testLazyCreationSeveralM() {
        assertEquals("Check that no objects are created ", 0, ((PrimitiveInstanceDescription) lazzyArchSevM.getInstanceDescription()).getCreatedObjects().length);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), ci_lazzy_sevM.getInstanceName());
        assertNotNull("Check that a FooService from " + ci_lazzy_sevM.getInstanceName() + " is available", ref);
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation", fs.foo());
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArchSevM.getInstanceDescription()).getCreatedObjects().length);
        FooService fs2 = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("Check the FooService invocation-2", fs2.foo());
        // Only one object as the getService method is called only once (service factory) despite the policy="method".
        assertEquals("Check the creation of 1 object", 1, ((PrimitiveInstanceDescription) lazzyArchSevM.getInstanceDescription()).getCreatedObjects().length);
    }

    /**
     * Test a custom constructor.
     * Not manipulated.
     */
    @Test
    public void testCustomConstuctor() {
        FooService fs = new FooProviderType1(0, "foo", bc);
        Properties props = fs.fooProps();
        assertEquals("Check bar", 0, ((Integer) props.get("bar")).intValue());
        assertEquals("Check foo", "foo", props.get("foo"));
        assertEquals("Check context", bc, props.get("context"));
    }

    @Test
    public void testSuperCall() {
        try {
            ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.CallSuperConstructor");
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSuperCallWithNew() {
        try {
            ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.CallSuperConstructorWithNew");
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSuperSuperCallWithNew() {
        try {
            ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.CallSuperSuperConstructorWithNew");
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSuperCallWithBC() {
        try {
            ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.CallSuperConstructorWithBC");
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }


}