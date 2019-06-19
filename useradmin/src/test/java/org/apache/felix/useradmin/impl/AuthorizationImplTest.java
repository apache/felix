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

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Test cases for {@link AuthorizationImpl}.
 */
public class AuthorizationImplTest extends TestCase {

    private RoleRepository m_roleRepository;
    private Role m_anyone;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_roleRepository = new RoleRepository(new MemoryRoleRepositoryStore());
        
        m_anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
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
        User elmer = RoleFactory.createUser("Elmer");
        User fudd = RoleFactory.createUser("Fudd");
        User marvin = RoleFactory.createUser("Marvin");
        User pepe = RoleFactory.createUser("Pepe");
        User daffy =RoleFactory.createUser("Daffy");
        User foghorn = RoleFactory.createUser("Foghorn");
        
        // Not explicitly mentioned; but needed to comply with the semantics
        alarmSystemControl.addRequiredMember(m_anyone);
        internetAccess.addRequiredMember(m_anyone);
        temperatureControl.addRequiredMember(m_anyone);
        photoAlbumEdit.addRequiredMember(m_anyone);
        photoAlbumView.addRequiredMember(m_anyone);
        portForwarding.addRequiredMember(m_anyone);

        administrators.addRequiredMember(m_anyone);
        buddies.addRequiredMember(m_anyone);
        children.addRequiredMember(m_anyone);
        adults.addRequiredMember(m_anyone);
        residents.addRequiredMember(m_anyone);

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
        AuthorizationImpl auth = new AuthorizationImpl(foghorn, m_roleRepository);

        assertFalse(auth.hasRole(alarmSystemControl.getName()));
        assertFalse(auth.hasRole(internetAccess.getName()));
        assertFalse(auth.hasRole(temperatureControl.getName()));
        assertFalse(auth.hasRole(photoAlbumEdit.getName()));
        assertTrue(auth.hasRole(photoAlbumView.getName()));
        assertFalse(auth.hasRole(portForwarding.getName()));

        // Test with the user "fudd"...
        auth = new AuthorizationImpl(fudd, m_roleRepository);

        assertFalse(auth.hasRole(alarmSystemControl.getName()));
        assertTrue(auth.hasRole(internetAccess.getName()));
        assertTrue(auth.hasRole(temperatureControl.getName()));
        assertTrue(auth.hasRole(photoAlbumEdit.getName()));
        assertTrue(auth.hasRole(photoAlbumView.getName()));
        assertFalse(auth.hasRole(portForwarding.getName()));

        // Test with the user "elmer"...
        auth = new AuthorizationImpl(elmer, m_roleRepository);

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
        Group citizens = createGroup("citizen");
        citizens.addRequiredMember(m_anyone);
        
        Group adults = createGroup("adult");
        adults.addRequiredMember(m_anyone);
        
        Group voters = createGroup("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(m_anyone);
        
        // Elmer belongs to the citizens and adults...
        User elmer = createUser("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        User pepe = createUser("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        User bugs = createUser("bugs");
        adults.addMember(bugs);
        
        // Daffy is not an adult, neither a citizen...
        User daffy = createUser("daffy");
        
        AuthorizationImpl auth;

        auth = new AuthorizationImpl(elmer, m_roleRepository);
        assertTrue(auth.hasRole("adult"));
        assertTrue(auth.hasRole("citizen"));
        assertTrue(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(pepe, m_roleRepository);
        assertFalse(auth.hasRole("adult"));
        assertTrue(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(bugs, m_roleRepository);
        assertTrue(auth.hasRole("adult"));
        assertFalse(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));

        auth = new AuthorizationImpl(daffy, m_roleRepository);
        assertFalse(auth.hasRole("adult"));
        assertFalse(auth.hasRole("citizen"));
        assertFalse(auth.hasRole("voter"));
        assertFalse(auth.hasRole("non-existing-role"));
    }

    /**
     * Test that the tests for membership work correctly. 
     */
    public void testGetRolesOk() {
        Group citizens = createGroup("citizen");
        citizens.addRequiredMember(m_anyone);
        
        Group adults = createGroup("adult");
        adults.addRequiredMember(m_anyone);
        
        Group voters = createGroup("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(m_anyone);
        
        // Elmer belongs to the citizens and adults...
        User elmer = createUser("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        User pepe = createUser("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        User bugs = createUser("bugs");
        adults.addMember(bugs);

        // Daffy is not an adult, neither a citizen...
        User daffy = createUser("daffy");

        // Donald is not an adult, neither a citizen...
        User donald = RoleFactory.createUser("donald");
        
        AuthorizationImpl auth;

        auth = new AuthorizationImpl(elmer, m_roleRepository);
        assertSameRoles(new String[]{ "elmer", "adult", "citizen", "voter" }, auth.getRoles());

        auth = new AuthorizationImpl(pepe, m_roleRepository);
        assertSameRoles(new String[]{ "pepe", "citizen" }, auth.getRoles());

        auth = new AuthorizationImpl(bugs, m_roleRepository);
        assertSameRoles(new String[]{ "bugs", "adult" }, auth.getRoles());

        auth = new AuthorizationImpl(daffy, m_roleRepository);
        assertSameRoles(new String[]{ "daffy" }, auth.getRoles());

        auth = new AuthorizationImpl(donald, m_roleRepository);
        assertNull(auth.getRoles());
    }

    private void assertSameRoles(String[] expected, String[] roles) {
        assertTrue("Expected " + expected.length + " roles, got " + roles.length + "!", expected.length == roles.length);
        
        List e = new ArrayList(Arrays.asList(expected));
        List r = new ArrayList(Arrays.asList(roles));
        e.removeAll(r);
        
        assertTrue("Not seen: " + e, e.isEmpty());
    }

    private Group createGroup(String name) {
        return (Group) m_roleRepository.addRole(name, Role.GROUP);
    }

    private User createUser(String name) {
        return (User) m_roleRepository.addRole(name, Role.USER);
    }
}
