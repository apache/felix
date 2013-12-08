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
import org.apache.felix.ipojo.runtime.core.components.InnerClasses;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestNestedClasses extends BaseTest {

    private ComponentInstance instance;
    private CheckService service;

    @Before
    public void setUp() {
        Properties map = new Properties();
        map.put("publicObject", "publicObject");
        map.put("publicInt", new Integer(0));
        map.put("packageObject", "packageObject");
        map.put("packageInt", new Integer(1));
        map.put("protectedObject", "protectedObject");
        map.put("protectedInt", new Integer(2));
        map.put("privateObject", "privateObject");
        map.put("privateInt", new Integer(3));
        map.put("nonObject", "nonObject");
        map.put("nonInt", new Integer(4));
        instance = ipojoHelper.createComponentInstance("inners", map);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check service availability", ref);
        service = (CheckService) osgiHelper.getServiceObject(ref);
    }

    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testPrivateInnerClass() {
        Map data = (Map) service.getProps().get("privateInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testProtectedInnerClass() {
        Map data = (Map) service.getProps().get("protectedInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testPackageInnerClass() {
        Map data = (Map) service.getProps().get("packageInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testPublicInnerClass() {
        Map data = (Map) service.getProps().get("publicInner");
        assertNotNull("Check data existence", data);
        System.out.println(data);
        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testConstructorInnerClass() {
        Map data = (Map) service.getProps().get("constructorInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testStaticInnerClass() {
        Map data = (Map) service.getProps().get("staticInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check static", new Boolean(true), data.get("static"));
        assertEquals("Check static int", new Integer(6), data.get("staticint"));

    }

    @Test
    public void testAnonymousClassDeclaredInStaticField() {
        assertEquals(service.getProps().get("call"), 1);
    }

    @Test
    public void testPackageStaticInnerClass() {
        Map data = (Map) service.getProps().get("packageStaticInner");
        assertNotNull("Check data existence", data);

        assertEquals("Check static", new Boolean(true), data.get("static"));
        assertEquals("Check static int", new Integer(6), data.get("staticint"));

    }

    @Test
    public void testAnonymousInnerClass() {
        Map data = (Map) service.getProps().get("anonymous");
        assertNotNull("Check data existence", data);

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));

    }

    @Test
    public void testInnerAccess() {
        Map map = (Map) service.getProps();
        assertNotNull("Check map existence", map);

        InnerClasses.PublicNested p = (InnerClasses.PublicNested) map.get("public");
        Map data = p.doSomething();

        assertEquals("Check public object", "publicObject", data.get("publicObject"));
        assertEquals("Check public int", new Integer(0), data.get("publicInt"));
        assertEquals("Check protected object", "protectedObject", data.get("protectedObject"));
        assertEquals("Check protected int", new Integer(2), data.get("protectedInt"));
        assertEquals("Check package object", "packageObject", data.get("packageObject"));
        assertEquals("Check package int", new Integer(1), data.get("packageInt"));
        assertEquals("Check private object", "privateObject", data.get("privateObject"));
        assertEquals("Check private int", new Integer(3), data.get("privateInt"));
        assertEquals("Check non-managed object", "not-managed", data.get("nonObject"));
        assertEquals("Check non-managed int", new Integer(5), data.get("nonInt"));
    }


    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }

}
