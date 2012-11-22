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
package org.apache.felix.useradmin.filestore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Test case for {@link RoleRepositorySerializer}. 
 */
public class RoleRepositorySerializerTest extends TestCase {

    private Role m_userAnyone;
    private User m_user1;
    private User m_user2;
    private User m_user3;
    private User m_user4;
    private Group m_group1;
    private Group m_group2;
    private Group m_group3;
    private Map m_repository;
    
    private RoleRepositorySerializer m_serializer;

    /**
     * Tests that writing and reading a repository with a role, four users and a three group works as expected.
     */
    public void testRWRepositoryWithRolesUsersAndGroupsOk() throws Exception {
        m_group1.addRequiredMember(m_userAnyone);
        m_group1.addMember(m_user1);

        m_group2.addMember(m_group1);
        m_group2.addMember(m_user2);
        m_group2.addRequiredMember(m_user1);
        
        m_group3.addMember(m_user3);
        m_group3.addMember(m_user4);
        m_group3.addRequiredMember(m_userAnyone);
        
        addToRepository(m_user1);
        addToRepository(m_user2);
        addToRepository(m_user3);
        addToRepository(m_user4);
        addToRepository(m_group1);
        addToRepository(m_group2);
        addToRepository(m_group3);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);

        assertEquals(7, result.size());
        assertEquals(m_user1, (User) result.get(m_user1.getName()));
        assertEquals(m_user2, (User) result.get(m_user2.getName()));
        assertEquals(m_user3, (User) result.get(m_user3.getName()));
        assertEquals(m_user4, (User) result.get(m_user4.getName()));
        assertEquals(m_group1, (Group) result.get(m_group1.getName()));
        assertEquals(m_group2, (Group) result.get(m_group2.getName()));
        assertEquals(m_group3, (Group) result.get(m_group3.getName()));
    }

    /**
     * Tests that writing and reading a repository with a single group works as expected.
     */
    public void testRWRepositoryWithSingleGroupOk() throws Exception {
        addToRepository(m_group1);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);
        
        assertEquals(1, result.size());

        assertEquals(m_group1, (Role) result.get(m_group1.getName()));
    }

    /**
     * Tests that writing and reading a repository without roles works as expected.
     */
    public void testRWRepositoryWithoutRolesOk() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);
        
        assertEquals(0, result.size());
    }

    /**
     * Tests that writing and reading a repository with a single user role works as expected.
     */
    public void testRWRepositoryWithSingleUserOk() throws Exception {
        addToRepository(m_user1);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);
        
        assertEquals(1, result.size());

        assertEquals(m_user1, (User) result.get(m_user1.getName()));
    }

    /**
     * Tests that writing and reading a repository with two user roles works as expected.
     */
    public void testRWRepositoryWithTwoUsersOk() throws Exception {
        addToRepository(m_user1);
        addToRepository(m_user2);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);
        
        assertEquals(2, result.size());
        assertEquals(m_user1, (User) result.get(m_user1.getName()));
        assertEquals(m_user2, (User) result.get(m_user2.getName()));
    }

    /**
     * Tests that reading a repository that has a cyclic group reference can be read ok.
     */
    public void testRWRepositoryWithCyclicGroupReference() throws Exception {
        m_group1.addMember(m_group2);
        m_group2.addMember(m_group1);
        m_group2.addMember(m_user1);

        addToRepository(m_user1);
        addToRepository(m_group2);
        addToRepository(m_group1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        Map result = m_serializer.deserialize(dis);
        assertNotNull(result);
        
        assertEquals(3, result.size());
        assertEquals(m_group1, (Group) result.get(m_group1.getName()));
        assertEquals(m_group2, (Group) result.get(m_group2.getName()));
        assertEquals(m_user1, (User) result.get(m_user1.getName()));
    }

    /**
     * Tests that reading a repository that is missing a referenced basic role from a group fails.
     */
    public void testRWRepositoryWithUnreferencedBasicRoleInGroupFail() throws Exception {
        m_group1.addMember(m_userAnyone);
        m_group1.addRequiredMember(m_user1);

        // "Forget" to add the user1
        addToRepository(m_group1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        try {
            m_serializer.deserialize(dis);
            
            fail("IOException expected!");
        } catch (IOException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that reading a repository that is missing a referenced required role from a group fails.
     */
    public void testRWRepositoryWithUnreferencedRequiredRoleInGroupFail() throws Exception {
        m_group1.addRequiredMember(m_userAnyone);
        m_group1.addMember(m_user1);
        
        // "Forget" to add the user1!
        addToRepository(m_group1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        m_serializer.serialize(m_repository, dos); // should succeed!
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        
        try {
            m_serializer.deserialize(dis);
            
            fail("IOException expected!");
        } catch (IOException e) {
            // Ok; expected
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();

        m_serializer = new RoleRepositorySerializer();

        m_repository = new HashMap();

        m_userAnyone = RoleFactory.createRole(Role.USER_ANYONE);
        
        setProperties(m_userAnyone);
        
        m_user1 = createUser(1);
        m_user2 = createUser(2);
        m_user3 = createUser(3);
        m_user4 = createUser(4);

        m_group1 = createGroup(1);
        m_group2 = createGroup(2);
        m_group3 = createGroup(3);
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private void addToRepository(Role role) {
        if (!Role.USER_ANYONE.equals(role.getName())) {
            m_repository.put(role.getName(), role);
        }
    }
    
    private void assertEquals(Dictionary expected, Dictionary obtained) {
        assertNotNull(expected);
        assertNotNull(obtained);
        
        assertEquals(expected.size(), obtained.size());
        
        Enumeration enumeration = expected.keys();
        while (enumeration.hasMoreElements()) {
            Object expectedKey = enumeration.nextElement();
            Object expectedValue = expected.get(expectedKey);

            Object obtainedValue = obtained.get(expectedKey);
            if (expectedValue == null) {
                assertNull(obtainedValue);
            } else if (expectedValue.getClass().isArray()) {
                assertTrue(Arrays.equals((byte[]) expectedValue, (byte[]) obtainedValue));
            } else {
                assertEquals(expectedValue, obtainedValue);
            }
        }
    }

    private void assertEquals(Group expected, Group obtained) {
        assertEquals((Object) expected, (Object) obtained);
        
        assertEquals(expected.getProperties(), obtained.getProperties());
        assertEquals(expected.getCredentials(), obtained.getCredentials());
        assertEquals(expected.getMembers(), obtained.getMembers());
        assertEquals(expected.getRequiredMembers(), obtained.getRequiredMembers());
    }

    private void assertEquals(Role expected, Role obtained) {
        assertEquals((Object) expected, (Object) obtained);
        
        assertEquals(expected.getProperties(), obtained.getProperties());
    }

    private void assertEquals(User expected, User obtained) {
        assertEquals((Object) expected, (Object) obtained);
        
        assertEquals(expected.getProperties(), obtained.getProperties());
        assertEquals(expected.getCredentials(), obtained.getCredentials());
    }

    private void assertEquals(Role[] expected, Role[] obtained) {
        if (expected != null) {
            assertNotNull(obtained);
            assertEquals(expected.length, obtained.length);
            
            List e = new ArrayList(Arrays.asList(expected));
            e.removeAll(Arrays.asList(obtained));
            
            assertTrue("Roles not obtained: " + e, e.isEmpty());
            
            Map m = new HashMap();
            for (int i = 0; i < expected.length; i++) {
                m.put(expected[i].getName(), expected[i]);
            }
            
            for (int i = 0; i < obtained.length; i++) {
                assertEquals(m.get(obtained[i].getName()), obtained[i]);
            }
        } else {
            assertNull(obtained);
        }
    }

    private User createUser(int idx) {
        String name = "User" + idx;
        
        User result = RoleFactory.createUser(name);

        setCredentials(result);
        setProperties(result);
        
        return result;
    }

    private Group createGroup(int idx) {
        String name = "Group" + idx;
        
        Group result = RoleFactory.createGroup(name);

        setCredentials(result);
        setProperties(result);
        
        return result;
    }

    private void setCredentials(User user) {
        user.getCredentials().put(user.getName(), user.getName());
        user.getCredentials().put("password", user.getName());
        user.getCredentials().put("certificate", new byte[] { (byte) 0x55, (byte) 0xAA } );
    }

    private void setProperties(Role role) {
        role.getProperties().put(role.getName(), role.getName());
        role.getProperties().put("key1", role.getName());
        role.getProperties().put("key2", "hello world".getBytes());
    }
}
