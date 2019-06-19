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


import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.RoleImpl;
import org.apache.felix.useradmin.impl.role.UserImpl;
import org.osgi.service.useradmin.Role;

import junit.framework.TestCase;

/**
 * Test case for {@link GroupImpl}.
 */
public class GroupImplTest extends TestCase {

    /**
     * Tests that adding a role as basic member twice does not cause duplication.
     */
    public void testAddBasicMemberWithExistingBasicMemberOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(new UserImpl("bar")));
        assertFalse(group.addMember(new UserImpl("bar"))); // should be ignored...

        assertEquals(1, group.getMembers().length);
        assertNull(group.getRequiredMembers());
    }

    /**
     * Tests that adding a role as required member works if it is not contained at all. 
     */
    public void testAddBasicMemberWithExistingRequiredMemberOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addRequiredMember(new UserImpl("bar")));
        assertFalse(group.addMember(new UserImpl("bar"))); // should be ignored...

        assertNull(group.getMembers());
        assertEquals(1, group.getRequiredMembers().length);
    }

    /**
     * Tests that adding a role as basic member while another role with the same name exists does not cause duplication.
     */
    public void testAddBasicMemberWithExistingRoleOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(new UserImpl("bar")));
        assertFalse(group.addMember(new RoleImpl("bar"))); // should be ignored...

        assertEquals(1, group.getMembers().length);
        assertNull(group.getRequiredMembers());
    }

    /**
     * Tests that adding a role as basic member works if it is not contained at all.
     */
    public void testAddNonExistingMemberOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(new UserImpl("bar")));
        
        assertEquals(1, group.getMembers().length);
        assertNull(group.getRequiredMembers());
    }

    /**
     * Tests that adding a role as basic member while it exists as required member does not cause duplication. 
     */
    public void testAddNonExistingRequiredMemberOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addRequiredMember(new UserImpl("bar")));

        assertNull(group.getMembers());
        assertEquals(1, group.getRequiredMembers().length);
    }

    /**
     * Tests that adding a role as required member works if it is not contained at all. 
     */
    public void testAddRequiredMemberWithExistingBasicMemberOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(new UserImpl("bar")));
        assertFalse(group.addRequiredMember(new UserImpl("bar"))); // should be ignored...

        assertEquals(1, group.getMembers().length);
        assertNull(group.getRequiredMembers());
    }

    /**
     * Tests that adding a role as required member while another role with the same name exists does not cause duplication.
     */
    public void testAddRequiredMemberWithExistingRoleOk() {
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addRequiredMember(new UserImpl("bar")));
        assertFalse(group.addRequiredMember(new RoleImpl("bar"))); // should be ignored...

        assertNull(group.getMembers());
        assertEquals(1, group.getRequiredMembers().length);
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.RoleImpl#getType()}.
     */
    public void testGetType() {
        GroupImpl group = new GroupImpl("foo");
        
        assertEquals(Role.GROUP, group.getType());
    }

    /**
     * Tests that {@link GroupImpl#hashCode()} yields predictable results.
     */
    public void testHashCodeOk() {
        GroupImpl group1 = new GroupImpl("foo");
        GroupImpl group2 = new GroupImpl("foo");
        GroupImpl group3 = new GroupImpl("bar");
        
        assertTrue(group1.hashCode() == group2.hashCode());
        assertFalse(group1.hashCode() == group3.hashCode());
        assertFalse(group2.hashCode() == group3.hashCode());
    }

    /**
     * Tests that removing an basic required member works.
     */
    public void testRemoveExistingBasicMemberOk() {
        UserImpl role1 = new UserImpl("bar");
        UserImpl role2 = new UserImpl("qux");
        
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(role1));
        assertTrue(group.addRequiredMember(role2));

        assertEquals(1, group.getMembers().length);
        assertEquals(1, group.getRequiredMembers().length);
        
        assertTrue(group.removeMember(role1));

        assertNull(group.getMembers());
        assertEquals(1, group.getRequiredMembers().length);
    }

    /**
     * Tests that removing an existing required member works.
     */
    public void testRemoveExistingRequiredMemberOk() {
        UserImpl role1 = new UserImpl("bar");
        UserImpl role2 = new UserImpl("qux");
        
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(role1));
        assertTrue(group.addRequiredMember(role2));

        assertEquals(1, group.getMembers().length);
        assertEquals(1, group.getRequiredMembers().length);
        
        assertTrue(group.removeMember(role2));

        assertEquals(1, group.getMembers().length);
        assertNull(group.getRequiredMembers());
    }

    /**
     * Tests that removing an basic required member works.
     */
    public void testRemoveNonExistingMemberOk() {
        UserImpl role1 = new UserImpl("bar");
        UserImpl role2 = new UserImpl("qux");
        UserImpl role3 = new UserImpl("quu");
        
        GroupImpl group = new GroupImpl("foo");
        assertTrue(group.addMember(role1));
        assertTrue(group.addRequiredMember(role2));

        assertEquals(1, group.getMembers().length);
        assertEquals(1, group.getRequiredMembers().length);
        
        assertFalse(group.removeMember(role3));

        assertEquals(1, group.getMembers().length);
        assertEquals(1, group.getRequiredMembers().length);
    }
}
