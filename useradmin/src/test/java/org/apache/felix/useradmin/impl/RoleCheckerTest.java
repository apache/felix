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

import junit.framework.TestCase;

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;


/**
 * Test cases for {@link RoleChecker}.
 */
public class RoleCheckerTest extends TestCase {
    
    private RoleChecker m_roleChecker;
    private Role m_anyone;

    /**
     * Tests that a user always implies itself.
     */
    public void testUserAlwaysImpliesItself() {
        User user = RoleFactory.createUser("foo");
        
        assertTrue(m_roleChecker.isImpliedBy(user, user));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testUserDoesNotImplyNotImpliedGroup() {
        User user = RoleFactory.createUser("foo");
        Group group = RoleFactory.createGroup("bar");
        
        assertFalse(m_roleChecker.isImpliedBy(user, group));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testUserImpliesImpliedGroup() {
        User user = RoleFactory.createUser("foo");
        
        Group group = RoleFactory.createGroup("bar");
        group.addRequiredMember(m_anyone);
        group.addMember(user);

        assertTrue(m_roleChecker.isImpliedBy(group, user));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testGroupDoesNotImplyNotImpliedUser() {
        User user = RoleFactory.createUser("foo");
        
        Group group = RoleFactory.createGroup("bar");
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(user, group));
    }

    /**
     * Test that a group does never imply a group to which it is a required member.
     */
    public void testGroupDoesNotImplySameRequiredGroup() {
        User user = RoleFactory.createUser("foo");
        
        Group group = RoleFactory.createGroup("bar");
        group.addRequiredMember(group);
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(group, group));
    }

    /**
     * Test that a group does never imply a group to which it is a basic member.
     */
    public void testGroupDoesNotImplySameGroup() {
        User user = RoleFactory.createUser("foo");
        
        Group group = RoleFactory.createGroup("bar");
        group.addMember(group);
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(group, group));
    }

    /**
     * Test that a membership can be implied for users belonging to multiple required groups.
     */
    public void testRequiredRolesMultipleRequiredGroupsOk() {
        User elmer = RoleFactory.createUser("elmer");
        User pepe = RoleFactory.createUser("pepe");
        User bugs = RoleFactory.createUser("bugs");
        User daffy = RoleFactory.createUser("daffy");
        
        Group administrators = RoleFactory.createGroup("administrators");
        administrators.addRequiredMember(m_anyone);
        administrators.addMember(elmer);
        administrators.addMember(pepe);
        administrators.addMember(bugs);

        Group family = RoleFactory.createGroup("family");
        family.addRequiredMember(m_anyone);
        family.addMember(elmer);
        family.addMember(pepe);
        family.addMember(daffy);

        Group alarmSystemActivation = RoleFactory.createGroup("alarmSystemActivation");
        alarmSystemActivation.addMember(m_anyone);
        alarmSystemActivation.addRequiredMember(administrators);
        alarmSystemActivation.addRequiredMember(family);

        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, elmer));
        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, pepe));
        assertFalse(m_roleChecker.isImpliedBy(alarmSystemActivation, bugs));
        assertFalse(m_roleChecker.isImpliedBy(alarmSystemActivation, daffy));
    }

    /**
     * Test that a membership can be implied for users belonging to multiple non-required groups.
     */
    public void testRequiredRolesMultipleGroupsOk() {
        User elmer = RoleFactory.createUser("elmer");
        User pepe = RoleFactory.createUser("pepe");
        User bugs = RoleFactory.createUser("bugs");
        User daffy = RoleFactory.createUser("daffy");
        
        Group administrators = RoleFactory.createGroup("administrators");
        administrators.addRequiredMember(m_anyone);
        administrators.addMember(elmer);
        administrators.addMember(pepe);
        administrators.addMember(bugs);

        Group family = RoleFactory.createGroup("family");
        family.addRequiredMember(m_anyone);
        family.addMember(elmer);
        family.addMember(pepe);
        family.addMember(daffy);

        Group alarmSystemActivation = RoleFactory.createGroup("alarmSystemActivation");
        alarmSystemActivation.addRequiredMember(m_anyone);
        alarmSystemActivation.addMember(administrators);
        alarmSystemActivation.addMember(family);

        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, elmer));
        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, pepe));
        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, bugs));
        assertTrue(m_roleChecker.isImpliedBy(alarmSystemActivation, daffy));
    }

    /**
     * Test that a membership can be implied for users belonging to multiple non-required groups.
     */
    public void testVotersRequiredMembersOk() {
        Group citizens = RoleFactory.createGroup("citizen");
        citizens.addRequiredMember(m_anyone);
        
        Group adults = RoleFactory.createGroup("adult");
        adults.addRequiredMember(m_anyone);
        
        Group voters = RoleFactory.createGroup("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(m_anyone);
        
        
        // Elmer belongs to the citizens and adults...
        User elmer = RoleFactory.createUser("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        User pepe = RoleFactory.createUser("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        User bugs = RoleFactory.createUser("bugs");
        adults.addMember(bugs);
        
        // Daffy is not an adult, neither a citizen...
        User daffy = RoleFactory.createUser("daffy");

        assertTrue(m_roleChecker.isImpliedBy(voters, elmer));
        assertFalse(m_roleChecker.isImpliedBy(voters, pepe));
        assertFalse(m_roleChecker.isImpliedBy(voters, bugs));
        assertFalse(m_roleChecker.isImpliedBy(voters, daffy));
    }
    
    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_anyone = RoleFactory.createRole(Role.USER_ANYONE);

        m_roleChecker = new RoleChecker();
    }
}
