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

package org.apache.felix.ipojo.runtime.core.test.dependencies.inner;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C1;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C2;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C3;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestInnerProxyMix extends Common {

    public static String C1 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C1";
    public static String C2 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C2";
    public static String C3 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C3";

    private ComponentInstance instancec1;
    private ComponentInstance instancec2;
    private ComponentInstance instancec3;

    @Before
    public void setUp() {
        // Create the instances
        instancec1 = ipojoHelper.createComponentInstance(C1);
        instancec2 = ipojoHelper.createComponentInstance(C2);
        instancec3 = ipojoHelper.createComponentInstance(C3);
    }

    @Test
    public void testMix() {
        // Check that everything is OK
        assertEquals(ComponentInstance.VALID, instancec1.getState());
        assertEquals(ComponentInstance.VALID, instancec2.getState());
        assertEquals(ComponentInstance.VALID, instancec3.getState());

        // Call C3
        C3 svc = (C3) osgiHelper.getServiceObject(C3, null);
        assertNotNull(svc);
        assertEquals("called", svc.getFilter().authenticate());

        // So far, all right

        //We stop c1 and c2.
        instancec1.stop();
        instancec2.stop();

        assertEquals(ComponentInstance.INVALID, instancec3.getState()); // C2 dependency invalid

        instancec1.start();
        instancec2.start();

        // Check that everything is OK
        assertEquals(ComponentInstance.VALID, instancec1.getState());
        assertEquals(ComponentInstance.VALID, instancec2.getState());
        assertEquals(ComponentInstance.VALID, instancec3.getState());

        // Call C3
        svc = (C3) osgiHelper.getServiceObject(C3, null);
        assertNotNull(svc);
        assertEquals("called", svc.getFilter().authenticate());
    }

}
