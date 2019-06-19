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
package org.apache.felix.useradmin.impl;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * Test cases for {@link UserAdminImpl}. 
 */
public class UserAdminImplTest extends TestCase {
    
    /**
     * Provides a stub implementation for {@link EventAdmin}.
     */
    static class StubEventAdmin implements EventAdmin {
        
        public void postEvent(Event event) {
        }

        public void sendEvent(Event event) {
        }
    }

    /**
     * Provides a stub implementation for {@link UserAdminListenerList}.
     */
    static class StubUserAdminListenerList implements UserAdminListenerList {

        public UserAdminListener[] getListeners() {
            return new UserAdminListener[0];
        }
    }

    private UserAdminImpl m_userAdmin;
    private RoleRepository m_roleRepository;
    private EventDispatcher m_dispatcher;

    /**
     * Tests that adding a basic member to a group works.
     */
    public void testAddGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        
        assertTrue(group1.addMember(user1));
        assertFalse(group1.addMember(user1));
    }

    /**
     * Tests that adding a required member to a group works.
     */
    public void testAddRequiredGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        
        assertTrue(group1.addRequiredMember(user1));
        assertFalse(group1.addRequiredMember(user1));
    }
    
    /**
     * Tests that adding a property of an invalid type to a role does not work and yields an exception.
     */
    public void testAddRolePropertyOfInvalidTypeFail() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        
        try {
            user1.getProperties().put("key", Integer.valueOf(1));
            
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that adding a property to a role works.
     */
    public void testAddRolePropertyOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);

        assertNull(user1.getProperties().get("key"));
        
        user1.getProperties().put("key", "value");

        assertEquals("value", user1.getProperties().get("key"));
    }

    /**
     * Tests that adding a credential of an invalid type to a user does not work and yields an exception.
     */
    public void testAddUserCredentialInvalidTypeFails() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        
        try {
            user1.getCredentials().put("key", Integer.valueOf(1));
            
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that adding a String credential to a user works.
     */
    public void testAddUserCredentialOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);

        assertNull(user1.getCredentials().get("key"));

        user1.getCredentials().put("key", "value");

        assertEquals("value", user1.getCredentials().get("key"));
        
        user1.getCredentials().put("key2", "value2".getBytes());

        assertTrue(Arrays.equals("value2".getBytes(), (byte[]) user1.getCredentials().get("key2")));
    }

    /**
     * Tests that testing for basic group membership works.
     */
    public void testBasicGroupMembershipOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        User user2 = (User) m_userAdmin.createRole("user2", Role.USER);
        User user3 = (User) m_userAdmin.createRole("user3", Role.USER);

        Group reqGroup = (Group) m_userAdmin.createRole("reqGroup", Role.GROUP);
        reqGroup.addMember(user1);
        reqGroup.addMember(user2);
        reqGroup.addMember(user3);
        
        Group group = (Group) m_userAdmin.createRole("group", Role.GROUP);
        group.addRequiredMember(reqGroup);
        group.addMember(user1);
        group.addMember(user2);

        Authorization auth = m_userAdmin.getAuthorization(user1);
        assertTrue(auth.hasRole("group"));
        
        auth = m_userAdmin.getAuthorization(user2);
        assertTrue(auth.hasRole("group"));
        
        auth = m_userAdmin.getAuthorization(user3);
        assertFalse(auth.hasRole("group"));
    }

    /**
     * Tests that changing a property to an invalid type does not work and yields an exception.
     */
    public void testChangeRolePropertyOfInvalidTypeFail() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value");
        
        try {
            user1.getProperties().put("key", Integer.valueOf(1));
            
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that changing a property of a role works.
     */
    public void testChangeRolePropertyOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value");

        assertEquals("value", user1.getProperties().get("key"));
        
        user1.getProperties().put("key", "changed");

        assertEquals("changed", user1.getProperties().get("key"));
    }

    /**
     * Tests that changing a credential of a user works.
     */
    public void testChangeUserCredentialOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        user1.getCredentials().put("key", "value");

        assertEquals("value", user1.getCredentials().get("key"));
        
        user1.getCredentials().put("key", "changed");

        assertEquals("changed", user1.getCredentials().get("key"));
    }

    /**
     * Tests that creating an existing group does not succeed and yields null.
     */
    public void testCreateExistingGroupFail() {
        Role role = null;

        role = m_userAdmin.createRole("group1", Role.GROUP);
        assertNotNull(role);

        role = m_userAdmin.createRole("group1", Role.GROUP);
        assertNull(role);
    }

    /**
     * Tests that creating an existing role does not succeed and yields null.
     */
    public void testCreateExistingUserFail() {
        Role role = null;

        role = m_userAdmin.createRole("user1", Role.USER);
        assertNotNull(role);

        role = m_userAdmin.createRole("user1", Role.USER);
        assertNull(role);
    }

    /**
     * Tests that creating a role without a name does not succeed and yields an exception.
     */
    public void testCreateInvalidRoleNameFail() {
        try {
            m_userAdmin.createRole(null, Role.USER);
            
            fail("Expected an IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that creating a role with an invalid type does not succeed and yields an exception.
     */
    public void testCreateInvalidRoleTypeFail() {
        try {
            m_userAdmin.createRole("role1", Role.ROLE);
            
            fail("Expected an IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that creating a non-existing group succeeds and yields a valid role instance.
     */
    public void testCreateNonExistingGroupOk() {
        Role role = null;
        
        role = m_userAdmin.createRole("group1", Role.GROUP);
        assertNotNull(role);
        assertEquals("group1", role.getName());
    }

    /**
     * Tests that creating a non-existing role succeeds and yields a valid role instance.
     */
    public void testCreateNonExistingUserOk() {
        Role role = null;
        
        role = m_userAdmin.createRole("user1", Role.USER);
        assertNotNull(role);
        assertEquals("user1", role.getName());
    }

    /**
     * Tests that creating a role without a name does not succeed and yields an exception.
     */
    public void testCreateRoleWithEmptyNameFail() {
        try {
            m_userAdmin.createRole("", Role.USER);
            
            fail("Expected an IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that creating a {@link UserAdminImpl} with a null event dispatcher fails.
     */
    public void testCreateUserAdminImplWithNullDispatcherFail() {
        try {
            new UserAdminImpl(m_roleRepository, null);
            
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that creating a {@link UserAdminImpl} with a null role repository fails.
     */
    public void testCreateUserAdminImplWithNullRepositoryFail() {
        try {
            new UserAdminImpl(null, m_dispatcher);
            
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that obtaining the authorization for a non-existing user yields null.
     */
    public void testGetAuthorizationForAnonymousUserOk() {
        Authorization auth = m_userAdmin.getAuthorization(null);
        
        assertNotNull(auth);

        assertNull(auth.getRoles());
        assertNull(auth.getName());
    }

    /**
     * Tests that obtaining the authorization for a non-existing user yields null.
     */
    public void testGetAuthorizationForNonExistingUserOk() {
        User nonExistingUser = RoleFactory.createUser("non-existing-user");
        Authorization auth = m_userAdmin.getAuthorization(nonExistingUser);

        assertNotNull(auth);

        assertNull(auth.getRoles());
        assertNotNull(auth.getName());
    }

    /**
     * Tests that getting a existing group with an unique key-value pair does not work and yields null.
     */
    public void testGetExistingGroupFail() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key1", "value1");
        user1.getProperties().put("key2", "constant");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key1", "value2");
        user1.getProperties().put("key2", "constant");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key1", "value3");

        assertNull(m_userAdmin.getUser("key1", "value3"));
    }

    /**
     * Tests that getting roles based on existing names works correctly.
     */
    public void testGetExistingRolesOk() {
        m_userAdmin.createRole("user1", Role.USER);
        m_userAdmin.createRole("user2", Role.USER);
        m_userAdmin.createRole("user3", Role.USER);
        m_userAdmin.createRole("group1", Role.GROUP);
        
        assertEquals("user1", m_userAdmin.getRole("user1").getName());
        assertEquals("user2", m_userAdmin.getRole("user2").getName());
        assertEquals("user3", m_userAdmin.getRole("user3").getName());
        assertEquals("group1", m_userAdmin.getRole("group1").getName());
    }

    /**
     * Tests that getting an existing user with a non unique key-value pair does not work and yields null.
     */
    public void testGetExistingUserWithNonUniqueKeyValueFail() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key1", "value1");
        user1.getProperties().put("key2", "constant");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key1", "value2");
        user2.getProperties().put("key2", "constant");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key1", "value3");

        assertNull(m_userAdmin.getUser("key2", "constant"));
    }

    /**
     * Tests that getting an existing user with an unique key-value pair works and yields the expected result.
     */
    public void testGetExistingUserWithUniqueKeyValueOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key1", "value1");
        user1.getProperties().put("key2", "constant");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key1", "value2");
        user2.getProperties().put("key2", "constant");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key1", "value3");

        assertEquals(user1, m_userAdmin.getUser("key1", "value1"));
        assertEquals(user2, m_userAdmin.getUser("key1", "value2"));
    }

    /**
     * Tests that retrieving the basic members from a group works.
     */
    public void testGetGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        assertNull(group1.getMembers());
        
        assertTrue(group1.addMember(user1));
        
        assertEquals(1, group1.getMembers().length);
    }

    /**
     * Tests that getting a non existing role by its name does not work and yields null.
     */
    public void testGetNonExistingRoleFails() {
        assertNull(m_userAdmin.getRole("user1"));
    }

    /**
     * Tests that getting a non-existing user does not work and yields null.
     */
    public void testGetNonExistingUserFail() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key1", "value1");
        user1.getProperties().put("key2", "constant");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key1", "value2");
        user1.getProperties().put("key2", "constant");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key1", "value3");

        assertNull(m_userAdmin.getUser("key1", "value4"));
    }

    /**
     * Tests that getting roles based on a OSGi-filter that does not match any roles yields null.
     */
    public void testGetNonMatchingRolesOk() throws Exception {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value1");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key", "value2");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key", "value3");

        Role[] roles = m_userAdmin.getRoles("(nonExisting=value*)");
        assertNull(roles);
    }

    /**
     * Tests that getting predefined roles based on their names works correctly.
     */
    public void testGetPredefinedRolesOk() {
        assertEquals(Role.USER_ANYONE, m_userAdmin.getRole(Role.USER_ANYONE).getName());
    }

    /**
     * Tests that getting a removed role yields null.
     */
    public void testGetRemovedRoleFail() {
        m_userAdmin.createRole("user1", Role.USER);
        assertEquals("user1", m_userAdmin.getRole("user1").getName());
        
        assertTrue(m_userAdmin.removeRole("user1"));
        assertNull(m_userAdmin.getRole("user1"));
    }

    /**
     * Tests that retrieving the required members from a group works.
     */
    public void testGetRequiredGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        assertNull(group1.getRequiredMembers());
        
        assertTrue(group1.addRequiredMember(user1));
        
        assertEquals(1, group1.getRequiredMembers().length);
    }

    /**
     * Tests that getting the various names of defined roles works and yields the expected result.
     */
    public void testGetRoleNamesOk() {
        m_userAdmin.createRole("user1", Role.USER);
        m_userAdmin.createRole("group1", Role.GROUP);

        assertEquals(Role.USER_ANYONE, m_userAdmin.getRole(Role.USER_ANYONE).getName());
        assertEquals("user1", m_userAdmin.getRole("user1").getName());
        assertEquals("group1", m_userAdmin.getRole("group1").getName());
    }

    /**
     * Tests that getting multiple roles based on a OSGi-filter works and yields the correct result.
     */
    public void testGetRolesWithFilterOk() throws Exception {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value1");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key", "value2");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key", "value3");
        Role group2 = m_userAdmin.createRole("group2", Role.GROUP);
        group2.getProperties().put("key", "otherValue3");

        Role[] roles = m_userAdmin.getRoles("(key=value*)");
        assertNotNull(roles);

        assertEquals(3, roles.length);
        
        List roleList = Arrays.asList(roles);
        assertTrue(roleList.contains(user1));
        assertTrue(roleList.contains(user2));
        assertTrue(roleList.contains(group1));
    }

    /**
     * Tests that getting roles based on an invalid OSGi-filter yields an exception.
     */
    public void testGetRolesWithInvalidFilterFails() throws Exception {
        try {
            m_userAdmin.getRoles("(nonExisting=value*");

            fail("Expected an InvalidSyntaxException!");
        } catch (InvalidSyntaxException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that getting multiple roles based on a OSGi-filter works and yields the correct result.
     */
    public void testGetRolesWithoutFilterOk() throws Exception {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value1");
        Role user2 = m_userAdmin.createRole("user2", Role.USER);
        user2.getProperties().put("key", "value2");
        Role group1 = m_userAdmin.createRole("group1", Role.GROUP);
        group1.getProperties().put("key", "value3");
        Role group2 = m_userAdmin.createRole("group2", Role.GROUP);
        group2.getProperties().put("key", "otherValue3");

        Role[] roles = m_userAdmin.getRoles(null);
        assertNotNull(roles);

        assertEquals(4, roles.length);
        
        List roleList = Arrays.asList(roles);
        assertTrue(roleList.contains(user1));
        assertTrue(roleList.contains(user2));
        assertTrue(roleList.contains(group1));
        assertTrue(roleList.contains(group2));
    }

    /**
     * Tests that getting the various types of defined roles works and yields the expected result.
     */
    public void testGetRoleTypesOk() {
        m_userAdmin.createRole("user1", Role.USER);
        m_userAdmin.createRole("group1", Role.GROUP);

        assertEquals(Role.ROLE, m_userAdmin.getRole(Role.USER_ANYONE).getType());
        assertEquals(Role.USER, m_userAdmin.getRole("user1").getType());
        assertEquals(Role.GROUP, m_userAdmin.getRole("group1").getType());
    }

    /**
     * Tests that testing for group membership with anonymous users works.
     */
    public void testGroupMembershipWithAnonymousUserOk() {
        Role user = m_userAdmin.createRole("user", Role.USER);

        Group group = (Group) m_userAdmin.createRole("group", Role.GROUP);
        group.addMember(user);

        Authorization auth = m_userAdmin.getAuthorization(null);
        assertTrue(auth.hasRole(Role.USER_ANYONE));
        assertFalse(auth.hasRole("group"));
    }

    /**
     * Tests that testing for group membership with "user.anyone" works.
     */
    public void testGroupMembershipWithUserAnyoneOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        User user2 = (User) m_userAdmin.createRole("user2", Role.USER);
        User user3 = (User) m_userAdmin.createRole("user3", Role.USER);
        User user4 = (User) m_userAdmin.createRole("user4", Role.USER);

        Group reqGroup = (Group) m_userAdmin.createRole("reqGroup", Role.GROUP);
        reqGroup.addMember(user1);
        reqGroup.addMember(user2);
        
        Group group = (Group) m_userAdmin.createRole("group", Role.GROUP);
        group.addRequiredMember(reqGroup);
        group.addMember(m_userAdmin.getRole(Role.USER_ANYONE));

        Authorization auth = m_userAdmin.getAuthorization(user1);
        assertTrue(auth.hasRole("group"));

        auth = m_userAdmin.getAuthorization(user2);
        assertTrue(auth.hasRole("group"));

        auth = m_userAdmin.getAuthorization(user3);
        assertFalse(auth.hasRole("group"));

        auth = m_userAdmin.getAuthorization(user4);
        assertFalse(auth.hasRole("group"));
    }

    /**
     * Tests that testing the credentials for a user works and yields the correct result.
     */
    public void testHasUserCredentialOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        user1.getCredentials().put("key1", "value");
        user1.getCredentials().put("key2", "value".getBytes());

        assertTrue(user1.hasCredential("key1", "value"));
        assertTrue(user1.hasCredential("key1", "value".getBytes()));
        assertTrue(user1.hasCredential("key2", "value"));
        assertTrue(user1.hasCredential("key2", "value".getBytes()));
        assertFalse(user1.hasCredential("otherKey", "value"));
    }

    /**
     * Tests that removing an existing role works.
     */
    public void testRemoveExistingRoleOk() {
        Role role = null;

        role = m_userAdmin.createRole("group1", Role.GROUP);
        assertNotNull(role);

        assertTrue(m_userAdmin.removeRole("group1"));
        assertFalse(m_userAdmin.removeRole("group1"));
    }

    /**
     * Tests that removing a non existing role does not work, yields a false result.
     */
    public void testRemoveNonExistingRoleOk() {
        assertFalse(m_userAdmin.removeRole("group1"));
    }

    /**
     * Tests that removing a predefined role does not work, and yields a false result.
     */
    public void testRemovePredefinedRoleOk() {
        assertFalse(m_userAdmin.removeRole(Role.USER_ANYONE));
    }

    /**
     * Tests that remove a property of a role works.
     */
    public void testRemoveRolePropertyOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        user1.getProperties().put("key", "value");

        assertEquals("value", user1.getProperties().get("key"));
        
        user1.getProperties().remove("key");

        assertNull(user1.getProperties().get("key"));
    }

    /**
     * Tests that remove of a role also removes that role as member from any group (FELIX-3755).
     */
    public void testRemoveRoleRemovesItAsGroupMemberOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        Role user2 = m_userAdmin.createRole("user2", Role.USER);

        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        group1.addMember(user1);

        Group group2 = (Group) m_userAdmin.createRole("group2", Role.GROUP);
        group2.addMember(user1);
        group2.addMember(user2);
        
        // Remove user...
        m_userAdmin.removeRole(user1.getName());

        // Retrieve an up-to-date instance of the first group...
        group1 = (Group) m_userAdmin.getRole("group1");
        assertNull(group1.getMembers());

        // Retrieve an up-to-date instance of the second group...
        group2 = (Group) m_userAdmin.getRole("group2");

        Role[] members = group2.getMembers();
		assertNotNull(members);
		assertEquals(1, members.length);
        assertEquals(user2, members[0]);
    }

    /**
     * Tests that remove of a role also removes that role as required member from any group (FELIX-3755).
     */
    public void testRemoveRoleRemovesItAsRequiredGroupMemberOk() {
        Role user1 = m_userAdmin.createRole("user1", Role.USER);
        Role user2 = m_userAdmin.createRole("user2", Role.USER);

        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        group1.addRequiredMember(user1);
        group1.addMember(user2);

        Group group2 = (Group) m_userAdmin.createRole("group2", Role.GROUP);
        group2.addRequiredMember(user1);
        group2.addRequiredMember(user2);

        // Remove user...
        m_userAdmin.removeRole(user1.getName());
        
        // Retrieve an up-to-date instance of the group...
        group1 = (Group) m_userAdmin.getRole("group1");
        
        assertNull(group1.getRequiredMembers());

        Role[] members = group1.getMembers();
		assertNotNull(members);
        assertEquals(1, members.length);
        assertEquals(user2, members[0]);
        
        // Retrieve an up-to-date instance of the group...
        group2 = (Group) m_userAdmin.getRole("group2");
        
        assertNull(group2.getMembers());

        members = group2.getRequiredMembers();
		assertNotNull(members);
        assertEquals(1, members.length);
        assertEquals(user2, members[0]);
    }

    /**
     * Tests that remove a credential of a user works.
     */
    public void testRemoveUserCredentialOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        user1.getCredentials().put("key", "value");

        assertEquals("value", user1.getCredentials().get("key"));
        
        user1.getCredentials().remove("key");

        assertNull(user1.getCredentials().get("key"));
    }

    /**
     * Tests that removing a basic member from a group works.
     */
    public void testRemovingGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        
        assertTrue(group1.addMember(user1));
        
        assertTrue(group1.removeMember(user1));
        assertFalse(group1.removeMember(user1));
    }

    /**
     * Tests that removing a required member from a group works.
     */
    public void testRemovingRequiredGroupMemberOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);
        
        assertTrue(group1.addRequiredMember(user1));
        
        assertTrue(group1.removeMember(user1));
        assertFalse(group1.removeMember(user1));
    }

    /**
     * Tests that testing for required group membership works.
     */
    public void testRequiredGroupMembershipOk() {
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        User user2 = (User) m_userAdmin.createRole("user2", Role.USER);
        User user3 = (User) m_userAdmin.createRole("user3", Role.USER);

        Group reqGroup = (Group) m_userAdmin.createRole("reqGroup", Role.GROUP);
        reqGroup.addMember(user1);
        reqGroup.addMember(user2);
        reqGroup.addMember(user3);
        
        Group group = (Group) m_userAdmin.createRole("group", Role.GROUP);
        group.addRequiredMember(reqGroup);

        Authorization auth = m_userAdmin.getAuthorization(user1);
        assertFalse(auth.hasRole("group"));

        auth = m_userAdmin.getAuthorization(user2);
        assertFalse(auth.hasRole("group"));
        
        auth = m_userAdmin.getAuthorization(user3);
        assertFalse(auth.hasRole("group"));
    }

    /**
     * Tests that the list of roles in an {@link Authorization} does not contain the any-user, although it is defined as group member.
     */
    public void testUserAnyoneIsNotPartOfAuthorizedRolesOk() {
        Role userAnyone = m_userAdmin.getRole(Role.USER_ANYONE);
        User user1 = (User) m_userAdmin.createRole("user1", Role.USER);
        Group group1 = (Group) m_userAdmin.createRole("group1", Role.GROUP);

        assertTrue(group1.addRequiredMember(user1));
        assertTrue(group1.addMember(userAnyone));
        
        Authorization auth = m_userAdmin.getAuthorization(user1);
        assertNotNull(auth);
        
        assertTrue(auth.hasRole("group1"));
        
        String[] roles = auth.getRoles();
        assertNotNull(roles);
        
        for (int i = 0; i < roles.length; i++) {
            assertFalse(Role.USER_ANYONE.equals(roles[i]));
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_roleRepository = new RoleRepository(new MemoryRoleRepositoryStore());
        m_dispatcher = new EventDispatcher(new StubEventAdmin(), new StubUserAdminListenerList());

        m_userAdmin = new UserAdminImpl(m_roleRepository, m_dispatcher);
        m_dispatcher.start();
    }
    
    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        m_dispatcher.stop();
        
        super.tearDown();
    }

}
