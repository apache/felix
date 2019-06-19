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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Test cases for {@link RoleRepository}.
 */
public class RoleRepositoryTest extends TestCase {

    private RoleRepository m_roleRepository;
    private CountDownLatch m_latch;
    
    /**
     * Tests whether adding a new role to a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddBasicRoleYieldsEventOk() throws Exception {
        final Group role = (Group) m_roleRepository.addRole("foo", Role.GROUP);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                Role anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
                role.addMember(anyone);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a null {@link RoleChangeListener} instance does not work.
     */
    public void testAddNullRoleChangeListenerFail() throws Exception {
        try {
            m_roleRepository.addRoleChangeListener(null);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that adding a predefined role is not allowed.
     */
    public void testAddPredefineRoleFails() {
        try {
            assertNull(m_roleRepository.addRole(Role.USER_ANYONE, Role.ROLE));
            fail("IllegalArgumentException expected!");
        }
        catch (IllegalArgumentException e)
        {
            // Ok; expected
        }
    }

    /**
     * Tests whether adding a new role to a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddRequiredRoleYieldsEventOk() throws Exception {
        final Group role = (Group) m_roleRepository.addRole("foo", Role.GROUP);

        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                Role anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
                role.addRequiredMember(anyone);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a valid {@link RoleChangeListener} instance works.
     */
    public void testAddRoleChangeListenerOk() throws Exception {
        // Should succeed...
        m_roleRepository.addRoleChangeListener(new RoleChangeListener() {
            public void propertyAdded(Role role, Object key, Object value) {
            }
            
            public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
            }
            
            public void propertyRemoved(Role role, Object key) {
            }
            
            public void roleAdded(Role role) {
            }
            
            public void roleRemoved(Role role) {
            }
        });
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#addRole(Role)}.
     */
    public void testAddRoleOfSameTypeTwiceFail() {
        assertNotNull(m_roleRepository.addRole("foo", Role.USER));
        assertEquals(1, m_roleRepository.getRoles(null).size());

        assertNull(m_roleRepository.addRole("foo", Role.USER));
        assertEquals(1, m_roleRepository.getRoles(null).size());
    }

    /**
     * Tests that adding a role works.
     */
    public void testAddRoleOk() {
        assertNotNull(m_roleRepository.addRole("foo", Role.USER));
        assertEquals(1, m_roleRepository.getRoles(null).size());
    }

    /**
     * Tests whether adding a new property to a role causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddRolePropertyYieldsEventOk() throws Exception {
        m_latch = new CountDownLatch(1);
        
        final Role role = m_roleRepository.addRole("john.doe", Role.USER);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().put("key", "value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a null-role does not work and yields an exception.
     */
    public void testAddRoleWithNullRoleFail() {
        try {
            m_roleRepository.addRole(null, Role.USER);
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that adding a role with an invalid type does not work and yields an exception.
     */
    public void testAddRoleWithInvalidRoleTypeFail() {
        try {
            m_roleRepository.addRole("role", Role.ROLE);
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#addRole(Role)}.
     */
    public void testAddRoleWithSameNameTwiceFail() {
        assertNotNull(m_roleRepository.addRole("foo", Role.USER));
        assertEquals(1, m_roleRepository.getRoles(null).size());

        assertNull(m_roleRepository.addRole("foo", Role.GROUP));
        assertEquals(1, m_roleRepository.getRoles(null).size());
    }

    /**
     * Tests whether adding a new credential to a user causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddUserCredentialYieldsEventOk() throws Exception {
        m_latch = new CountDownLatch(1);
        
        final User role = (User) m_roleRepository.addRole("john.doe", Role.USER);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().put("key", "value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests whether changing an existing property to a role causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testChangeRolePropertyYieldsEventOk() throws Exception {
        final Role role = m_roleRepository.addRole("john.doe", Role.USER);
        role.getProperties().put("key", "value");
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().put("key", "other-value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests whether changing an existing credential for a user causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testChangeUserCredentialYieldsEventOk() throws Exception {
        final User role = (User) m_roleRepository.addRole("john.doe", Role.USER);
        role.getCredentials().put("key", "value");
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().put("key", "other-value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoleByName(java.lang.String)}.
     */
    public void testGetRoleByName() {
        Role role1 = m_roleRepository.addRole("foo", Role.USER);
        Role role2 = m_roleRepository.addRole("bar", Role.GROUP);

        assertEquals(role1, m_roleRepository.getRoleByName("foo"));
        assertEquals(role2, m_roleRepository.getRoleByName("bar"));
        assertNull(m_roleRepository.getRoleByName("qux"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(org.osgi.framework.Filter)}.
     */
    public void testGetRolesWithFilterOk() throws Exception {
        Role role1 = m_roleRepository.addRole("foo", Role.USER);
        role1.getProperties().put("key", "value1");
        role1.getProperties().put("keyA", "valueA");
        Role role2 = m_roleRepository.addRole("bar", Role.GROUP);
        role2.getProperties().put("key", "value2");
        role2.getProperties().put("keyB", "value1");
        
        String filter;

        filter = "(key=value1)";
        assertSameRoles(new Role[]{ role1 }, m_roleRepository.getRoles(filter));

        filter = "(key=value2)";
        assertSameRoles(new Role[]{ role2 }, m_roleRepository.getRoles(filter));

        filter = "(key=value*)";
        assertSameRoles(new Role[]{ role1, role2 }, m_roleRepository.getRoles(filter));

        filter = "(|(key=value1)(keyB=value1))";
        assertSameRoles(new Role[]{ role1, role2 }, m_roleRepository.getRoles(filter));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(String, String)}.
     */
    public void testGetRolesWithKeyValuePairOk() throws Exception {
        Role role1 = m_roleRepository.addRole("foo", Role.USER);
        role1.getProperties().put("key", "value1");
        role1.getProperties().put("keyA", "valueA");
        Role role2 = m_roleRepository.addRole("bar", Role.GROUP);
        role2.getProperties().put("key", "value2");
        role2.getProperties().put("keyB", "value1");

        assertSameRoles(new Role[]{ role1 }, m_roleRepository.getRoles("key", "value1"));
        assertSameRoles(new Role[]{ role2 }, m_roleRepository.getRoles("key", "value2"));
        assertSameRoles(new Role[0], m_roleRepository.getRoles("key", "value"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(org.osgi.framework.Filter)}.
     */
    public void testGetRolesWithoutFilterOk() {
        Role role1 = m_roleRepository.addRole("foo", Role.USER);
        Role role2 = m_roleRepository.addRole("bar", Role.GROUP);
        
        assertSameRoles(new Role[]{ role2, role1 }, m_roleRepository.getRoles(null));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoleByName(java.lang.String)}.
     */
    public void testGetUserAnyoneRoleByName() {
        Role anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
        assertEquals(Role.USER_ANYONE, anyone.getName());
        assertEquals(Role.ROLE, anyone.getType());
    }

    /**
     * Tests whether removing a role from a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveBasicRoleYieldsEventOk() throws Exception {
        final Role anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
        final Group role = (Group) m_roleRepository.addRole("bar", Role.GROUP);
        role.addMember(anyone);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.removeMember(anyone);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemoveExistingRoleOk() {
        assertNotNull(m_roleRepository.addRole("foo", Role.USER));
        
        assertTrue(m_roleRepository.removeRole("foo"));
        assertEquals(0, m_roleRepository.getRoles(null).size());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemoveNonExistingRoleOk() {
        assertNotNull(m_roleRepository.addRole("foo", Role.USER));

        assertFalse(m_roleRepository.removeRole("qux"));
        assertEquals(1, m_roleRepository.getRoles(null).size());
    }

    /**
     * Tests that removing a null {@link RoleChangeListener} instance does not work.
     */
    public void testRemoveNullRoleChangeListenerFail() throws Exception {
        try {
            m_roleRepository.removeRoleChangeListener(null);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemovePredefinedRoleFails() {
        m_roleRepository.addRole("foo", Role.USER);

        assertFalse(m_roleRepository.removeRole(Role.USER_ANYONE));

        assertEquals(1, m_roleRepository.getRoles(null).size());
    }

    /**
     * Tests whether removing a role from a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveRequiredRoleYieldsEventOk() throws Exception {
        final Role anyone = m_roleRepository.getRoleByName(Role.USER_ANYONE);
        final Group role = (Group) m_roleRepository.addRole("bar", Role.GROUP);
        role.addRequiredMember(anyone);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.removeMember(anyone);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing a valid {@link RoleChangeListener} instance works.
     */
    public void testRemoveRoleChangeListenerOk() throws Exception {
        // Should succeed...
        m_roleRepository.removeRoleChangeListener(new RoleChangeListener() {
            public void propertyAdded(Role role, Object key, Object value) {
            }
            
            public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
            }
            
            public void propertyRemoved(Role role, Object key) {
            }
            
            public void roleAdded(Role role) {
            }
            
            public void roleRemoved(Role role) {
            }
        });
    }

    /**
     * Tests whether removing a property from a role causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveRolePropertyYieldsEventOk() throws Exception {
        final Role role = m_roleRepository.addRole("john.doe", Role.USER);
        role.getProperties().put("key", "value");
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().remove("key");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing a null-role does not work and yields an exception.
     */
    public void testRemoveRoleWithNullRoleFail() {
        try {
            m_roleRepository.removeRole(null);
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests whether removing a credential from a user causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveUserCredentialYieldsEventOk() throws Exception {
        final User role = (User) m_roleRepository.addRole("john.doe", Role.USER);
        role.getCredentials().put("key", "value");
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().remove("key");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();

        m_roleRepository = new RoleRepository(new MemoryRoleRepositoryStore());
        m_roleRepository.addRoleChangeListener(new RoleChangeListener() {
            public void propertyAdded(Role role, Object key, Object value) {
                if (m_latch != null) {
                    m_latch.countDown();
                }
            }

            public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
                if (m_latch != null) {
                    m_latch.countDown();
                }
            }
            
            public void propertyRemoved(Role role, Object key) {
                if (m_latch != null) {
                    m_latch.countDown();
                }
            }

            public void roleAdded(Role role) {
                if (m_latch != null) {
                    m_latch.countDown();
                }
            }

            public void roleRemoved(Role role) {
                if (m_latch != null) {
                    m_latch.countDown();
                }
            }
        });
    }

    /**
     * Asserts that a given collection of roles has the correct expected contents.
     * 
     * @param expected
     * @param roles
     */
    private void assertSameRoles(Role[] expected, Collection roles) {
        assertTrue("Expected " + expected.length + " roles, got " + roles.size() + "!", expected.length == roles.size());
        
        List e = new ArrayList(Arrays.asList(expected));
        e.removeAll(roles);
        
        assertTrue("Not seen: " + e, e.isEmpty());
    }
}
