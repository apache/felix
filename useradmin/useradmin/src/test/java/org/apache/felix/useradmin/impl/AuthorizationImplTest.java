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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.UserImpl;
import org.osgi.service.useradmin.Group;

/**
 * Test cases for {@link AuthorizationImpl}.
 */
public class AuthorizationImplTest extends TestCase {

    private RoleRepository m_roleManager;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_roleManager = new RoleRepository(new MemoryRoleRepositoryStore());
    }
    
    /**
     * Test for example presented in section 107.3.2 of OSGi compendium v4.2.
     */
    public void testAuthorizationExampleOk() {
        // Action groups...
        Group alarmSystemControl = createGroup("AlarmSystemControl");
        Group internetAccess = createGroup("InternetAccess");
        Group temperatureControl = createGroup("TemperatureControl");
        Group photoAlbumEdit = createGroup("PhotoAlbumEdit");
        Group photoAlbumView = createGroup("PhotoAlbumView");
        Group portForwarding = createGroup("PortForwarding");
        
        // System user groups...
        Group administrators = createGroup("Administrators");
        Group buddies = createGroup("Buddies");
        Group children = createGroup("Children");
        Group adults = createGroup("Adults");
        Group residents = createGroup("Residents");
        
        // Users
        UserImpl elmer = new UserImpl("Elmer");
        UserImpl fudd = new UserImpl("Fudd");
        UserImpl marvin = new UserImpl("Marvin");
        UserImpl pepe = new UserImpl("Pepe");
        UserImpl daffy = new UserImpl("Daffy");
        UserImpl foghorn = new UserImpl("Foghorn");
        
        // Not explicitly mentioned; but needed to comply with the semantics
        alarmSystemControl.addRequiredMember(RoleRepository.USER_ANYONE);
        internetAccess.addRequiredMember(RoleRepository.USER_ANYONE);
        temperatureControl.addRequiredMember(RoleRepository.USER_ANYONE);
        photoAlbumEdit.addRequiredMember(RoleRepository.USER_ANYONE);
        photoAlbumView.addRequiredMember(RoleRepository.USER_ANYONE);
        portForwarding.addRequiredMember(RoleRepository.USER_ANYONE);

        administrators.addRequiredMember(RoleRepository.USER_ANYONE);
        buddies.addRequiredMember(RoleRepository.USER_ANYONE);
        children.addRequiredMember(RoleRepository.USER_ANYONE);
        adults.addRequiredMember(RoleRepository.USER_ANYONE);
        residents.addRequiredMember(RoleRepository.USER_ANYONE);

        // Table 107.1
        residents.addMember(elmer);
        residents.addMember(fudd);
        residents.addMember(marvin);
        residents.addMember(pepe);
        
        buddies.addMember(daffy);
        buddies.addMember(foghorn);
        
        children.addMember(marvin);
        children.addMember(pepe);
        
        adults.addMember(elmer);
        adults.addMember(fudd);
        
        administrators.addMember(elmer);
        
        // Table 107.2
        alarmSystemControl.addMember(residents);
        alarmSystemControl.addRequiredMember(administrators);
        
        internetAccess.addMember(residents);
        internetAccess.addRequiredMember(adults);
        
        temperatureControl.addMember(residents);
        temperatureControl.addRequiredMember(adults);
        
        photoAlbumEdit.addMember(residents);
        photoAlbumEdit.addMember(children);
        photoAlbumEdit.addMember(adults);
        
        photoAlbumView.addMember(residents);
        photoAlbumView.addMember(buddies);
        
        portForwarding.addMember(residents);
        portForwarding.addRequiredMember(administrators);

        // Test with the user "foghorn"...
        AuthorizationImpl auth = new AuthorizationImpl(foghorn, m_roleManager);

        assertFalse(auth.hasRole(alarmSystemControl.getName()));
        assertFalse(auth.hasRole(internetAccess.getName()));
        assertFalse(auth.hasRole(temperatureControl.getName()));
        assertFalse(auth.hasRole(photoAlbumEdit.getName()));
        assertTrue(auth.hasRole(photoAlbumView.getName()));
        assertFalse(auth.hasRole(portForwarding.getName()));

        // Test with the user "fudd"...
        auth = new AuthorizationImpl(fudd, m_roleManager);

        assertFalse(auth.hasRole(alarmSystemControl.getName()));
        assertTrue(auth.hasRole(internetAccess.getName()));
        assertTrue(auth.hasRole(temperatureControl.getName()));
        assertTrue(auth.hasRole(photoAlbumEdit.getName()));
        assertTrue(auth.hasRole(photoAlbumView.getName()));
        assertFalse(auth.hasRole(portForwarding.getName()));

        // Test with the user "elmer"...
        auth = new AuthorizationImpl(elmer, m_roleManager);

        assertTrue(auth.hasRole(alarmSystemControl.getName()));
        assertTrue(auth.hasRole(internetAccess.getName()));
        assertTrue(auth.hasRole(temperatureControl.getName()));
        assertTrue(auth.hasRole(photoAlbumEdit.getName()));
        assertTrue(auth.hasRole(photoAlbumView.getName()));
        assertTrue(auth.hasRole(portForwarding.getName()));
    }

    /**
     * Test that the tests for membership work correctly. 
     */
    public void testHasRoleOk() {
        GroupImpl citizens = createGroup("citizen");
        citizens.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl adults = createGroup("adult");
        adults.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl voters = createGroup("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(RoleRepository.USER_ANYONE);
        
        // Elmer belongs to the citizens and adults...
        UserImpl elmer = createUser("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        UserImpl pepe = createUser("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        UserImpl bugs = createUser("bugs");
        adults.addMember(bugs);
        
        // Daffy is not an adult, neither a citizen...
        UserImpl daffy = createUser("daffy");
        
        AuthorizationImpl auth;

        auth = new AuthorizationImpl(elmer, m_roleManager);
        assertTrue(auth.hasRole("adult"));
        assertTrue(auth.hasRole("citizen"));
        assertTrue(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(pepe, m_roleManager);
        assertFalse(auth.hasRole("adult"));
        assertTrue(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(bugs, m_roleManager);
        assertTrue(auth.hasRole("adult"));
        assertFalse(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(daffy, m_roleManager);
        assertFalse(auth.hasRole("adult"));
        assertFalse(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));
    }

    /**
     * Test that the tests for membership work correctly. 
     */
    public void testGetRolesOk() {
        GroupImpl citizens = createGroup("citizen");
        citizens.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl adults = createGroup("adult");
        adults.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl voters = createGroup("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(RoleRepository.USER_ANYONE);
        
        // Elmer belongs to the citizens and adults...
        UserImpl elmer = createUser("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        UserImpl pepe = createUser("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        UserImpl bugs = createUser("bugs");
        adults.addMember(bugs);

        // Daffy is not an adult, neither a citizen...
        UserImpl daffy = createUser("daffy");

        // Daffy is not an adult, neither a citizen...
        UserImpl donald = new UserImpl("donald");
        
        AuthorizationImpl auth;

        auth = new AuthorizationImpl(elmer, m_roleManager);
        assertSameRoles(new String[]{ "elmer", "adult", "citizen", "voter" }, auth.getRoles());

        auth = new AuthorizationImpl(pepe, m_roleManager);
        assertSameRoles(new String[]{ "pepe", "citizen" }, auth.getRoles());

        auth = new AuthorizationImpl(bugs, m_roleManager);
        assertSameRoles(new String[]{ "bugs", "adult" }, auth.getRoles());

        auth = new AuthorizationImpl(daffy, m_roleManager);
        assertSameRoles(new String[]{ "daffy" }, auth.getRoles());

        auth = new AuthorizationImpl(donald, m_roleManager);
        assertNull(auth.getRoles());
    }

    private void assertSameRoles(String[] expected, String[] roles) {
        assertTrue("Expected " + expected.length + " roles, got " + roles.length + "!", expected.length == roles.length);
        
        List e = new ArrayList(Arrays.asList(expected));
        List r = new ArrayList(Arrays.asList(roles));
        e.removeAll(r);
        
        assertTrue("Not seen: " + e, e.isEmpty());
    }

    private GroupImpl createGroup(String name) {
        GroupImpl result = new GroupImpl(name);
        m_roleManager.addRole(result);
        return result;
    }

    private UserImpl createUser(String name) {
        UserImpl result = new UserImpl(name);
        m_roleManager.addRole(result);
        return result;
    }
}
