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
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestCallbacks extends Common {

    @Test
    public void testWithPostRegistrationOnly() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Callbacks-reg-only");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        Integer reg = (Integer) check.getProps().get("registered");
        Integer unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(0), unreg);

        ci.stop();

        reg = (Integer) check.getProps().get("registered");
        unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(0), unreg);
    }

    @Test
    public void testWithBoth() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Callbacks-both");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        Integer reg = (Integer) check.getProps().get("registered");
        Integer unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(0), unreg);

        ci.stop();

        reg = (Integer) check.getProps().get("registered");
        unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(1), unreg);
    }

    @Test
    public void testWithPostUnregistrationOnly() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Callbacks-unreg-only");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        Integer reg = (Integer) check.getProps().get("registered");
        Integer unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(0), reg);
        assertEquals(new Integer(0), unreg);

        ci.stop();

        reg = (Integer) check.getProps().get("registered");
        unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(0), reg);
        assertEquals(new Integer(1), unreg);
    }

    @Test
    public void testWithTwoPairsOfCallbacks() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Callbacks-both-2");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        Integer reg = (Integer) check.getProps().get("registered");
        Integer unreg = (Integer) check.getProps().get("unregistered");
        Integer reg2 = (Integer) check.getProps().get("registered2");
        Integer unreg2 = (Integer) check.getProps().get("unregistered2");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertNotNull(reg2);
        assertNotNull(unreg2);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(0), unreg);
        assertEquals(new Integer(1), reg2);
        assertEquals(new Integer(0), unreg2);

        ci.stop();

        reg = (Integer) check.getProps().get("registered");
        unreg = (Integer) check.getProps().get("unregistered");
        reg2 = (Integer) check.getProps().get("registered2");
        unreg2 = (Integer) check.getProps().get("unregistered2");
        assertNotNull(reg2);
        assertNotNull(unreg2);
        assertEquals(new Integer(1), reg);
        assertEquals(new Integer(1), unreg);
        assertEquals(new Integer(1), reg2);
        assertEquals(new Integer(1), unreg2);
    }

    @Test
    public void testWithOnePairForTwoService() {
        ComponentInstance ci = ipojoHelper.createComponentInstance("PS-Callbacks-both-1");
        // Controller set to true.
        osgiHelper.waitForService(FooService.class.getName(), null, 5000);
        osgiHelper.waitForService(CheckService.class.getName(), null, 5000);

        CheckService check = (CheckService) osgiHelper.getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);

        Integer reg = (Integer) check.getProps().get("registered");
        Integer unreg = (Integer) check.getProps().get("unregistered");
        assertNotNull(reg);
        assertNotNull(unreg);
        assertEquals(new Integer(2), reg);
        assertEquals(new Integer(0), unreg);

        ci.stop();

        reg = (Integer) check.getProps().get("registered");
        unreg = (Integer) check.getProps().get("unregistered");
        assertEquals(new Integer(2), reg);
        assertEquals(new Integer(2), unreg);
    }
}
