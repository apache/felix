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

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.runtime.core.services.BarService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * Check component type description.
 */
public class TestComponentDesc extends Common {
	
	ServiceReference sr_fooProvider1;
	ServiceReference sr_fooProvider2;
	ServiceReference sr_fooProviderDyn2;
	ServiceReference sr_fooProvider3;
	ServiceReference sr_foobarProvider;
	
	Factory fooProvider1;
	Factory fooProvider2;
	Factory fooProviderDyn2;
	Factory fooProvider3;
	Factory foobarProvider;
	
    @Before
	public void setUp() {

        sr_fooProvider1 = osgiHelper.getServiceReferenceByPID(Factory.class, "Factories-FooProviderType-1");
        sr_fooProvider2 = osgiHelper.getServiceReferenceByPID(Factory.class, "Factories-FooProviderType-2");
        sr_fooProviderDyn2 = osgiHelper.getServiceReferenceByPID(Factory.class, "Factories-FooProviderType-Dyn2");
        sr_fooProvider3 = osgiHelper.getServiceReferenceByPID(Factory.class, "Factories-FooProviderType-3");
        sr_foobarProvider = osgiHelper.getServiceReferenceByPID(Factory.class, "Factories-FooBarProviderType-1");

		fooProvider1 = ipojoHelper.getFactory("Factories-FooProviderType-1");
		fooProvider2 = ipojoHelper.getFactory("Factories-FooProviderType-2");
		fooProviderDyn2 = ipojoHelper.getFactory("Factories-FooProviderType-Dyn2");
		fooProvider3 = ipojoHelper.getFactory("Factories-FooProviderType-3");
		foobarProvider = ipojoHelper.getFactory("Factories-FooBarProviderType-1");
	}
	
	/**
	 * Check simple providing.
	 */
    @Test
	public void testFooProvider1() {
		// Test SR properties
//		String impl = (String) sr_fooProvider1.getProperty("component.class");
//		assertEquals("Check component.class", impl, "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		String[] specs = (String[]) sr_fooProvider1.getProperty("component.providedServiceSpecifications");
		assertEquals("Check component.providedServiceSpecifications length", specs.length, 1);
		assertEquals("Check component.providedServiceSpecifications", FooService.class.getName(), specs[0]);
		
		PropertyDescription[] pd = (PropertyDescription[]) sr_fooProvider1.getProperty("component.properties");
		assertEquals("Check component.properties length", pd.length, 0);
		
		// Test factory
		assertEquals("Check factory name", fooProvider1.getName(), "Factories-FooProviderType-1");
		Element cd = fooProvider1.getDescription();
		
//		assertEquals("Check implementation class ", cd.getAttribute("implementation-class"), impl);
		
		Element[] specs2 = cd.getElements("provides");
		assertEquals("Check specs length", specs2.length, 1);
		assertEquals("Check specs", FooService.class.getName(), specs2[0].getAttribute("specification"));
		
		Element[] pd2 = cd.getElements("property");
		assertNull("Check props null", pd2);
		
		// Check Description equality
		ComponentTypeDescription desc = (ComponentTypeDescription) sr_fooProvider1.getProperty("component.description");
		assertNotNull("check description equality", desc);
	}
	
	/**
	 * Check component properties.
	 */
    @Test
	public void testFooProvider2() {
		// Test SR properties
//		String impl = (String) sr_fooProvider2.getProperty("component.class");
//		assertEquals("Check component.class", impl, "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		String[] specs = (String[]) sr_fooProvider2.getProperty("component.providedServiceSpecifications");
		assertEquals("Check component.providedServiceSpecifications length", specs.length, 1);
		assertEquals("Check component.providedServiceSpecifications", FooService.class.getName(), specs[0]);
		
		PropertyDescription[] pd = (PropertyDescription[]) sr_fooProvider2.getProperty("component.properties");
		assertEquals("Check component.properties length", pd.length, 5);
		
		assertEquals("Check component.properties name [" + 0 + "]", "int", pd[0].getName());
		assertEquals("Check component.properties type [" + 0 + "]", "int", pd[0].getType());
		assertEquals("Check component.properties value [" + 0 + "]", "2", pd[0].getValue());
		
		assertEquals("Check component.properties name [" + 1 + "]", "long", pd[1].getName());
		assertEquals("Check component.properties type [" + 1 + "]", "long", pd[1].getType());
		assertEquals("Check component.properties value [" + 1 + "]", "40", pd[1].getValue());
		
		assertEquals("Check component.properties name [" + 2 + "]", "string", pd[2].getName());
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd[2].getType());
		assertEquals("Check component.properties value [" + 2 + "]", "foo", pd[2].getValue());
		
		assertEquals("Check component.properties name [" + 3 + "]", "strAProp", pd[3].getName());
		assertEquals("Check component.properties type [" + 3 + "]", "java.lang.String[]", pd[3].getType());
		
		assertEquals("Check component.properties name [" + 4 + "]", "intAProp", pd[4].getName());
		assertEquals("Check component.properties type [" + 4 + "]", "int[]", pd[4].getType());
		
		// Test factory
		assertEquals("Check factory name", fooProvider2.getName(), "Factories-FooProviderType-2");
		Element cd = fooProvider2.getDescription();
        
//        assertEquals("Check implementation class ", cd.getAttribute("implementation-class"), impl);
		
        Element[] specs2 = cd.getElements("provides");
        assertEquals("Check specs length", specs2.length, 1);
        assertEquals("Check specs", FooService.class.getName(), specs2[0].getAttribute("specification"));
		
        Element[] pd2 = cd.getElements("property");
		assertEquals("Check props length", pd2.length, 5);
		
		assertEquals("Check component.properties name [" + 0 + "]", "int", pd2[0].getAttribute("name"));
		assertEquals("Check component.properties type [" + 0 + "]", "int", pd2[0].getAttribute("type"));
		assertEquals("Check component.properties value [" + 0 + "]", "2", pd2[0].getAttribute("value"));
		
		assertEquals("Check component.properties name [" + 1 + "]", "long", pd2[1].getAttribute("name"));
		assertEquals("Check component.properties type [" + 1 + "]", "long", pd2[1].getAttribute("type"));
		assertEquals("Check component.properties value [" + 1 + "]", "40", pd2[1].getAttribute("value"));
		
		assertEquals("Check component.properties name [" + 2 + "]", "string", pd2[2].getAttribute("name"));
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd2[2].getAttribute("type"));
		assertEquals("Check component.properties value [" + 2 + "]", "foo", pd2[2].getAttribute("value"));
		
		assertEquals("Check component.properties name [" + 3 + "]", "strAProp", pd2[3].getAttribute("name"));
		assertEquals("Check component.properties type [" + 3 + "]", "java.lang.String[]", pd2[3].getAttribute("type"));
		
		assertEquals("Check component.properties name [" + 4 + "]", "intAProp", pd2[4].getAttribute("name"));
		assertEquals("Check component.properties type [" + 4 + "]", "int[]", pd2[4].getAttribute("type"));
		
		// Check Description equality
		ComponentTypeDescription desc = (ComponentTypeDescription) sr_fooProvider2.getProperty("component.description");
        assertNotNull("check description equality", desc);

        // Check that we have the complete metadata
        assertNotNull(fooProvider2.getComponentMetadata());
	}
	
	/**
	 * Check component properties (dynamic).
	 */
    @Test
	public void testFooProviderDyn2() {
		// Test SR properties
//		String impl = (String) sr_fooProviderDyn2.getProperty("component.class");
//		assertEquals("Check component.class", impl, "org.apache.felix.ipojo.test.scenarios.component.FooProviderTypeDyn2");
		
		String[] specs = (String[]) sr_fooProviderDyn2.getProperty("component.providedServiceSpecifications");
		assertEquals("Check component.providedServiceSpecifications length", specs.length, 1);
		assertEquals("Check component.providedServiceSpecifications", FooService.class.getName(), specs[0]);
		
		PropertyDescription[] pd = (PropertyDescription[]) sr_fooProviderDyn2.getProperty("component.properties");
		assertEquals("Check component.properties length", pd.length, 5);
		
		assertEquals("Check component.properties name [" + 0 + "]", "int", pd[0].getName());
		assertEquals("Check component.properties type [" + 0 + "]", "int", pd[0].getType());
		assertEquals("Check component.properties value [" + 0 + "]", "4", pd[0].getValue());
		
		assertEquals("Check component.properties name [" + 1 + "]", "boolean", pd[1].getName());
		assertEquals("Check component.properties type [" + 1 + "]", "boolean", pd[1].getType());
		
		assertEquals("Check component.properties name [" + 2 + "]", "string", pd[2].getName());
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd[2].getType());
		
		assertEquals("Check component.properties name [" + 3 + "]", "strAProp", pd[3].getName());
		assertEquals("Check component.properties type [" + 3 + "]", "java.lang.String[]", pd[3].getType());
		
		assertEquals("Check component.properties name [" + 4 + "]", "intAProp", pd[4].getName());
		assertEquals("Check component.properties type [" + 4 + "]", "int[]", pd[4].getType());
		
		// Test factory
		assertEquals("Check factory name", fooProviderDyn2.getName(), "Factories-FooProviderType-Dyn2");
		Element cd = fooProviderDyn2.getDescription();
		
//        assertEquals("Check implementation class ", cd.getAttribute("implementation-class"), impl);
        
        Element[] specs2 = cd.getElements("provides");
        assertEquals("Check specs length", specs2.length, 1);
        assertEquals("Check specs", FooService.class.getName(), specs2[0].getAttribute("specification"));
        
        Element[] pd2 = cd.getElements("property");
        assertEquals("Check props length", pd2.length, 5);
		
		assertEquals("Check component.properties name [" + 0 + "]", "int", pd2[0].getAttribute("name"));
		assertEquals("Check component.properties type [" + 0 + "]", "int", pd2[0].getAttribute("type"));
		assertEquals("Check component.properties value [" + 0 + "]", "4", pd2[0].getAttribute("value"));
		
		assertEquals("Check component.properties name [" + 1 + "]", "boolean", pd2[1].getAttribute("name"));
		assertEquals("Check component.properties type [" + 1 + "]", "boolean", pd2[1].getAttribute("type"));
		
		assertEquals("Check component.properties name [" + 2 + "]", "string", pd2[2].getAttribute("name"));
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd2[2].getAttribute("type"));
		
		assertEquals("Check component.properties name [" + 3 + "]", "strAProp", pd2[3].getAttribute("name"));
		assertEquals("Check component.properties type [" + 3 + "]", "java.lang.String[]", pd2[3].getAttribute("type"));
		
		assertEquals("Check component.properties name [" + 4 + "]", "intAProp", pd2[4].getAttribute("name"));
		assertEquals("Check component.properties type [" + 4 + "]", "int[]", pd2[4].getAttribute("type"));
		
		// Check Description equality
		ComponentTypeDescription desc = (ComponentTypeDescription) sr_fooProviderDyn2.getProperty("component.description");
		assertNotNull("check description equality", desc);

        // Check that we have the complete metadata
        assertNotNull(fooProvider2.getComponentMetadata());
	}
	
	/**
	 * Check component properties.
	 */
    @Test
	public void testFooProvider3() {
		// Test SR properties
//		String impl = (String) sr_fooProvider3.getProperty("component.class");
//		assertEquals("Check component.class", impl, "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		String[] specs = (String[]) sr_fooProvider3.getProperty("component.providedServiceSpecifications");
		assertEquals("Check component.providedServiceSpecifications length", specs.length, 1);
		assertEquals("Check component.providedServiceSpecifications", FooService.class.getName(), specs[0]);
		
		PropertyDescription[] pd = (PropertyDescription[]) sr_fooProvider3.getProperty("component.properties");
		assertEquals("Check component.properties length (" + pd.length +")", pd.length, 3);
		
		assertEquals("Check component.properties name [" + 0 + "]", "foo", pd[0].getName());
		
		assertEquals("Check component.properties name [" + 1 + "]", "bar", pd[1].getName());
		
		assertEquals("Check component.properties name [" + 2 + "]", "baz", pd[2].getName());
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd[2].getType());
		
		// Test factory
		assertEquals("Check factory name", fooProvider3.getName(), "Factories-FooProviderType-3");
		Element cd = fooProvider3.getDescription();
        
//		assertEquals("Check implementation class ", cd.getAttribute("implementation-class"), impl);
        
        Element[] specs2 = cd.getElements("provides");
        assertEquals("Check specs length", specs2.length, 1);
        assertEquals("Check specs", FooService.class.getName(), specs2[0].getAttribute("specification"));
        
        Element[] pd2 = cd.getElements("property");
        assertEquals("Check props length", pd2.length, 3);
		
		assertEquals("Check component.properties name [" + 0 + "]", "foo", pd2[0].getAttribute("name"));
		
		assertEquals("Check component.properties name [" + 1 + "]", "bar", pd2[1].getAttribute("name"));
		
		assertEquals("Check component.properties name [" + 2 + "]", "baz", pd2[2].getAttribute("name"));
		assertEquals("Check component.properties type [" + 2 + "]", "java.lang.String", pd2[2].getAttribute("type"));
		
		// Check Description equality
		ComponentTypeDescription desc = (ComponentTypeDescription) sr_fooProvider3.getProperty("component.description");
		assertNotNull("check description equality", desc);
	}
	
	/**
	 * Test two services provider.
	 */
    @Test
	public void testFooBar() {
		//	Test SR properties
//		String impl = (String) sr_foobarProvider.getProperty("component.class");
//		assertEquals("Check component.class", impl, "org.apache.felix.ipojo.test.scenarios.component.FooBarProviderType1");
		
		String[] specs = (String[]) sr_foobarProvider.getProperty("component.providedServiceSpecifications");
		assertEquals("Check component.providedServiceSpecifications length", specs.length, 2);

        assertContains("Check component.providedServiceSpecifications 1", specs, FooService.class.getName());
        assertContains("Check component.providedServiceSpecifications 2", specs, BarService.class.getName());
		
		PropertyDescription[] pd = (PropertyDescription[]) sr_foobarProvider.getProperty("component.properties");
		assertEquals("Check component.properties length", pd.length, 0);
		
		// Test factory
		assertEquals("Check factory name", foobarProvider.getName(), "Factories-FooBarProviderType-1");
		Element cd = foobarProvider.getDescription();
		
//        assertEquals("Check implementation class ", cd.getAttribute("implementation-class"), impl);
		
        Element[] specs2 = cd.getElements("provides");
        assertEquals("Check specs length", specs2.length, 2);
        assertTrue("Check specs", containsSpecification(FooService.class.getName(), specs2));
        assertTrue("Check specs", containsSpecification(BarService.class.getName(), specs2));
        
        Element[] pd2 = cd.getElements("property");
        assertNull("Check props null", pd2);
		
		// Check Description equality
        ComponentTypeDescription desc = (ComponentTypeDescription) sr_foobarProvider.getProperty("component.description");
		assertNotNull("check description equality", desc);

        // Check that we have the complete metadata
        assertNotNull(foobarProvider.getComponentMetadata());
	}
	
	private boolean containsSpecification(String value, Element[] array) {
	    for (int i = 0; array != null && i < array.length; i++) {
            if (array[i] != null && array[i].containsAttribute("specification") && array[i].getAttribute("specification").equals(value)) {
                return true;
            }
        }
        return false;
	}
	
	
	

}
