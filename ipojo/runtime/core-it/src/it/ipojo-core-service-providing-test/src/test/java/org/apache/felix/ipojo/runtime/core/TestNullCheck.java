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
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestNullCheck extends Common {

    @Test
    public void testNull() {

        String factName = "PS-Null";
        String compName = "NullCheck";
        ServiceReference ref = null;

        // Check that no Foo Service are available
        ref = osgiHelper.getServiceReference(FooService.class.getName());
        assertNull("FS already available", ref);

        // Get the factory to create a component instance
        Factory fact = ipojoHelper.getFactory(factName);
        assertNotNull("Cannot find the factory FooProvider-1", fact);

        // Don't give any configuration so, properties are null.
        ipojoHelper.createComponentInstance(factName, compName);

        // Get a FooService provider
        ref = osgiHelper.getServiceReference(FooService.class.getName(), "(" + "instance.name" + "=" + compName + ")");

        assertNotNull("FS not available", ref);

        // Check service properties
        assertNull(ref.getProperty("prop1"));
        assertNotNull(ref.getProperty("prop2"));

        // Test foo invocation
        FooService fs = (FooService) osgiHelper.getServiceObject(ref);
        assertTrue("FooService invocation failed", fs.foo());

        ref = osgiHelper.getServiceReference(FooService.class.getName(), "(" + "instance.name" + "=" + compName + ")");
        // Check service properties
        assertNotNull(ref.getProperty("prop1"));
        assertNull(ref.getProperty("prop2"));

        ipojoHelper.dispose();

        // Check that there is no more FooService
        ref = osgiHelper.getServiceReference(FooService.class.getName(), null);

        assertNull("FS available, but component instance stopped", ref);

    }

}
