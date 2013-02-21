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
import org.apache.felix.ipojo.runtime.core.components.inherited.ProcessParentImplementation;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertNotNull;

public class TestClass extends Common {

    private Factory pi4, pi5, pi6, pi7;

    @Before
    public void setUp() {
        pi4 = ipojoHelper.getFactory("PS-PI4");
        pi5 = ipojoHelper.getFactory("PS-PI5");
        pi6 = ipojoHelper.getFactory("PS-PI6");
        pi7 = ipojoHelper.getFactory("PS-PI7");
    }

    @Test
    public void testIP4() {
        ipojoHelper.createComponentInstance(pi4.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName("org.apache.felix.ipojo.runtime.core.components.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check itself", ref1);

        ProcessParentImplementation itself = (ProcessParentImplementation) osgiHelper.getServiceObject(ref1);

        itself.processChild();
    }

    @Test
    public void testIP5() {
        ipojoHelper.createComponentInstance(pi5.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName("org.apache.felix.ipojo.runtime.core.components.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent", ref1);

        ProcessParentImplementation itself = (ProcessParentImplementation) osgiHelper.getServiceObject(ref1);

        itself.processChild();

    }

    @Test
    public void testIP6() {
        ipojoHelper.createComponentInstance(pi6.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName("org.apache.felix.ipojo.runtime.core.components.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent-parent", ref1);

        ProcessParentImplementation itself = (ProcessParentImplementation) osgiHelper.getServiceObject(ref1);

        itself.processChild();
    }

    @Test
    public void testIP7() {
        ipojoHelper.createComponentInstance(pi7.getName(), "ci");

        ServiceReference ref1 = ipojoHelper.getServiceReferenceByName("org.apache.felix.ipojo.runtime.core.components.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent-parent", ref1);

        ProcessParentImplementation itself = (ProcessParentImplementation) osgiHelper.getServiceObject(ref1);

        itself.processChild();

        ServiceReference ref5 = ipojoHelper.getServiceReferenceByName(FooService.class.getName(), "ci");
        assertNotNull("Check FS", ref5);
    }
}
