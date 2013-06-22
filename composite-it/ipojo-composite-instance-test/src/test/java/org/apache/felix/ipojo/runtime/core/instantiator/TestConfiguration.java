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

package org.apache.felix.ipojo.runtime.core.instantiator;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestConfiguration extends Common {

    private ComponentFactory compositeFactory;

    @Before
    public void setUp() {
        compositeFactory = (ComponentFactory) ipojoHelper.getFactory("CONF-MySuperComposite");
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testDefaultInstantiation() throws InvalidSyntaxException {
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = compositeFactory.createComponentInstance(props);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(under);
        ServiceReference[] refs = sc.getServiceReferences(FooService.class.getName(), null);
        assertEquals(2, refs.length);
        for (int i = 0; i < refs.length; i++) {
            assertEquals(3, ((Integer) refs[i].getProperty("int")).intValue());
            assertEquals("foo", (String) refs[i].getProperty("string"));
        }
        under.dispose();
    }

    @Test
    public void testConfiguredInstantiation() throws InvalidSyntaxException {
        Properties props = new Properties();
        props.put("instance.name", "under");
        props.put("string", "bar");
        props.put("int", "25");
        ComponentInstance under = null;
        try {
            under = compositeFactory.createComponentInstance(props);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(under);
        ServiceReference[] refs = sc.getServiceReferences(FooService.class.getName(), null);
        assertEquals(2, refs.length);
        for (int i = 0; i < refs.length; i++) {
            assertEquals(25, ((Integer) refs[i].getProperty("int")).intValue());
            assertEquals("bar", (String) refs[i].getProperty("string"));
        }
        under.dispose();
    }


}
