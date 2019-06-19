/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl.role;

import java.util.Dictionary;

import junit.framework.TestCase;


import org.apache.felix.useradmin.impl.role.RoleImpl;
import org.osgi.service.useradmin.Role;

/**
 * Test case for {@link RoleImpl}.
 */
public class RoleImplTest extends TestCase {

    /**
     * Tests that we must enter a valid name upon constructing a {@link RoleImpl}.
     */
    public void testCreateNullNameFail() {
        try {
            new RoleImpl(null);
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that we must enter a valid name upon constructing a {@link RoleImpl}.
     */
    public void testCreateEmptyNameFail() {
        try {
            new RoleImpl(" ");
            
            fail();
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that we can obtain the name of a {@link RoleImpl} as set in its constructor.
     */
    public void testGetName() {
        RoleImpl role = new RoleImpl("foo");
        assertEquals("foo", role.getName());
    }

    /**
     * Tests that we can obtain the properties of a {@link RoleImpl}.
     */
    public void testGetProperties() {
        RoleImpl role = new RoleImpl("foo");

        Dictionary dict = role.getProperties();
        assertNotNull(dict);
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.RoleImpl#getType()}.
     */
    public void testGetType() {
        RoleImpl role = new RoleImpl("foo");
        assertEquals(Role.ROLE, role.getType());
    }

    /**
     * Tests that {@link RoleImpl#hashCode()} yields predictable results.
     */
    public void testHashCodeOk() {
        RoleImpl role1 = new RoleImpl("foo");
        RoleImpl role2 = new RoleImpl("foo");
        RoleImpl role3 = new RoleImpl("bar");
        
        assertTrue(role1.hashCode() == role2.hashCode());
        assertFalse(role1.hashCode() == role3.hashCode());
        assertFalse(role2.hashCode() == role3.hashCode());
    }
}
