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

import org.apache.felix.useradmin.impl.RoleChecker;
import org.apache.felix.useradmin.impl.RoleRepository;
import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.UserImpl;

import junit.framework.TestCase;


/**
 * Test cases for {@link RoleChecker}.
 */
public class RoleCheckerTest extends TestCase {
    
    private RoleChecker m_roleChecker;

    /**
     * Tests that a user always implies itself.
     */
    public void testUserAlwaysImpliesItself() {
        UserImpl user = new UserImpl("foo");
        
        assertTrue(m_roleChecker.isImpliedBy(user, user));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testUserDoesNotImplyNotImpliedGroup() {
        UserImpl user = new UserImpl("foo");
        GroupImpl group = new GroupImpl("bar");
        
        assertFalse(m_roleChecker.isImpliedBy(user, group));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testUserImpliesImpliedGroup() {
        UserImpl user = new UserImpl("foo");
        
        GroupImpl group = new GroupImpl("bar");
        group.addRequiredMember(RoleRepository.USER_ANYONE);
        group.addMember(user);

        assertTrue(m_roleChecker.isImpliedBy(group, user));
    }

    /**
     * Test that a user does never imply a group to which it is not a member.
     */
    public void testGroupDoesNotImplyNotImpliedUser() {
        UserImpl user = new UserImpl("foo");
        
        GroupImpl group = new GroupImpl("bar");
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(user, group));
    }

    /**
     * Test that a group does never imply a group to which it is a required member.
     */
    public void testGroupDoesNotImplySameRequiredGroup() {
        UserImpl user = new UserImpl("foo");
        
        GroupImpl group = new GroupImpl("bar");
        group.addRequiredMember(group);
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(group, group));
    }

    /**
     * Test that a group does never imply a group to which it is a basic member.
     */
    public void testGroupDoesNotImplySameGroup() {
        UserImpl user = new UserImpl("foo");
        
        GroupImpl group = new GroupImpl("bar");
        group.addMember(group);
        group.addMember(user);
        
        assertFalse(m_roleChecker.isImpliedBy(group, group));
    }

    /**
     * Test that a membership can be implied for users belonging to multiple required groups.
     */
    public void testRequiredRolesMultipleRequiredGroupsOk() {
        UserImpl elmer = new UserImpl("elmer");
        UserImpl pepe = new UserImpl("pepe");
        UserImpl bugs = new UserImpl("bugs");
        UserImpl daffy = new UserImpl("daffy");
        
        GroupImpl administrators = new GroupImpl("administrators");
        administrators.addRequiredMember(RoleRepository.USER_ANYONE);
        administrators.addMember(elmer);
        administrators.addMember(pepe);
        administrators.addMember(bugs);

        GroupImpl family = new GroupImpl("family");
        family.addRequiredMember(RoleRepository.USER_ANYONE);
        family.addMember(elmer);
        family.addMember(pepe);
        family.addMember(daffy);

        GroupImpl alarmSystemActivation = new GroupImpl("alarmSystemActivation");
        alarmSystemActivation.addMember(RoleRepository.USER_ANYONE);
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
        UserImpl elmer = new UserImpl("elmer");
        UserImpl pepe = new UserImpl("pepe");
        UserImpl bugs = new UserImpl("bugs");
        UserImpl daffy = new UserImpl("daffy");
        
        GroupImpl administrators = new GroupImpl("administrators");
        administrators.addRequiredMember(RoleRepository.USER_ANYONE);
        administrators.addMember(elmer);
        administrators.addMember(pepe);
        administrators.addMember(bugs);

        GroupImpl family = new GroupImpl("family");
        family.addRequiredMember(RoleRepository.USER_ANYONE);
        family.addMember(elmer);
        family.addMember(pepe);
        family.addMember(daffy);

        GroupImpl alarmSystemActivation = new GroupImpl("alarmSystemActivation");
        alarmSystemActivation.addRequiredMember(RoleRepository.USER_ANYONE);
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
        GroupImpl citizens = new GroupImpl("citizen");
        citizens.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl adults = new GroupImpl("adult");
        adults.addRequiredMember(RoleRepository.USER_ANYONE);
        
        GroupImpl voters = new GroupImpl("voter");
        voters.addRequiredMember(citizens);
        voters.addRequiredMember(adults);
        voters.addMember(RoleRepository.USER_ANYONE);
        
        
        // Elmer belongs to the citizens and adults...
        UserImpl elmer = new UserImpl("elmer");
        citizens.addMember(elmer);
        adults.addMember(elmer);
        
        // Pepe belongs to the citizens, but is not an adult...
        UserImpl pepe = new UserImpl("pepe");
        citizens.addMember(pepe);
        
        // Bugs is an adult, but is not a citizen...
        UserImpl bugs = new UserImpl("bugs");
        adults.addMember(bugs);
        
        // Daffy is not an adult, neither a citizen...
        UserImpl daffy = new UserImpl("daffy");

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
        
        m_roleChecker = new RoleChecker();
    }
}
