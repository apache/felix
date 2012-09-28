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

import junit.framework.TestCase;

import org.osgi.service.useradmin.Role;

/**
 * Test cases for {@link StubGroupImpl}.
 */
public class StubGroupImplTest extends TestCase {

    /**
     * Tests that {@link StubGroupImpl#addMember(Role)} always fails.
     */
    public void testAddMemberRoleAlwaysFails() {
        try {
            new StubGroupImpl("test").addMember((Role) null);
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#addRequiredMember(Role)} always fails.
     */
    public void testAddRequiredMemberRoleAlwaysFails() {
        try {
            new StubGroupImpl("test").addRequiredMember((Role) null);
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#getMembers()} always fails.
     */
    public void testGetMembersAlwaysFails() {
        try {
            new StubGroupImpl("test").getMembers();
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#getRequiredMembers()} always fails.
     */
    public void testGetRequiredMembersAlwaysFails() {
        try {
            new StubGroupImpl("test").getRequiredMembers();
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#hasCredential(String, Object)} always fails.
     */
    public void testHasCredentialAlwaysFails() {
        try {
            new StubGroupImpl("test").hasCredential("foo", "bar");
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#removeMember(Role)} always fails.
     */
    public void testRemoveMemberAlwaysFails() {
        try {
            new StubGroupImpl("test").removeMember((Role) null);
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that {@link StubGroupImpl#hashCode()} and {@link StubGroupImpl#equals(Object)} works.
     */
    public void testHashCodeAndEqualsOk() {
        StubGroupImpl g1 = new StubGroupImpl("foo");
        StubGroupImpl g2 = new StubGroupImpl("bar");
        StubGroupImpl g3 = new StubGroupImpl("foo");
        StubGroupImpl g4 = new StubGroupImpl(null);

        assertTrue(g1.hashCode() == g3.hashCode());
        assertFalse(g1.hashCode() == g2.hashCode());
        assertFalse(g1.hashCode() == g4.hashCode());

        assertTrue(g1.equals(g3));
        assertTrue(g3.equals(g1));
        assertFalse(g1.equals(g2));
        assertFalse(g1.equals(g4));

        assertTrue(g4.equals(g4));
        assertFalse(g4.equals(g1));

        assertFalse(g1.equals(null));
        assertFalse(g1.equals("qux"));
    }
}
