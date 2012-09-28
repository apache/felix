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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.felix.framework.FilterImpl;
import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.UserImpl;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;

/**
 * Test cases for {@link RoleRepository}.
 */
public class RoleRepositoryTest extends TestCase {

    private RoleRepository m_roleManager;
    private CountDownLatch m_latch;
    
    /**
     * Tests whether adding a new role to a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddBasicRoleYieldsEventOk() throws Exception {
        final GroupImpl role = (GroupImpl) m_roleManager.addRole(new GroupImpl("foo"));
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.addMember(RoleRepository.USER_ANYONE);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a null {@link RoleChangeListener} instance does not work.
     */
    public void testAddNullRoleChangeListenerFail() throws Exception {
        try {
            m_roleManager.addRoleChangeListener(null);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that adding a predefined role is not allowed.
     */
    public void testAddPredefineRoleFails() {
        Role role = RoleRepository.USER_ANYONE;
        assertNull(m_roleManager.addRole(role));
        assertEquals(0, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests whether adding a new role to a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddRequiredRoleYieldsEventOk() throws Exception {
        final GroupImpl role = (GroupImpl) m_roleManager.addRole(new GroupImpl("foo"));

        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.addRequiredMember(RoleRepository.USER_ANYONE);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a valid {@link RoleChangeListener} instance works.
     */
    public void testAddRoleChangeListenerOk() throws Exception {
        // Should succeed...
        m_roleManager.addRoleChangeListener(new RoleChangeListener() {
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
        UserImpl role = new UserImpl("foo");

        assertSame(role, m_roleManager.addRole(role));
        assertEquals(1, m_roleManager.getRoles(null).size());

        assertNull(m_roleManager.addRole(role));
        assertEquals(1, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests that adding a role works.
     */
    public void testAddRoleOk() {
        UserImpl role = new UserImpl("foo");
        assertSame(role, m_roleManager.addRole(role));
        assertEquals(1, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests whether adding a new property to a role causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddRolePropertyYieldsEventOk() throws Exception {
        m_latch = new CountDownLatch(1);
        
        final Role role = m_roleManager.addRole(new UserImpl("john.doe"));
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().put("key", "value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a role that does not inherit from RoleImpl does not work and yields an exception.
     */
    public void testAddRoleWithInvalidRoleFail() {
        try {
            m_roleManager.addRole(new Role() {
                public String getName() {
                    return "A User";
                }
                public Dictionary getProperties() {
                    return new Properties();
                }
                public int getType() {
                    return Role.USER;
                }
            });
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that adding a null-role does not work and yields an exception.
     */
    public void testAddRoleWithNullRoleFail() {
        try {
            m_roleManager.addRole(null);
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#addRole(Role)}.
     */
    public void testAddRoleWithSameNameTwiceFail() {
        UserImpl role1 = new UserImpl("foo");
        GroupImpl role2 = new GroupImpl("foo");

        assertSame(role1, m_roleManager.addRole(role1));
        assertEquals(1, m_roleManager.getRoles(null).size());

        assertNull(m_roleManager.addRole(role2));
        assertEquals(1, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests whether adding a new credential to a user causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testAddUserCredentialYieldsEventOk() throws Exception {
        m_latch = new CountDownLatch(1);
        
        final UserImpl role = (UserImpl) m_roleManager.addRole(new UserImpl("john.doe"));
        
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
        final Role role = m_roleManager.addRole(new UserImpl("john.doe"));
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
        final UserImpl role = (UserImpl) m_roleManager.addRole(new UserImpl("john.doe"));
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
        Role role1 = m_roleManager.addRole(new UserImpl("foo"));
        Role role2 = m_roleManager.addRole(new GroupImpl("bar"));

        assertSame(role1, m_roleManager.getRoleByName("foo"));
        assertSame(role2, m_roleManager.getRoleByName("bar"));
        assertNull(m_roleManager.getRoleByName("qux"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(org.osgi.framework.Filter)}.
     */
    public void testGetRolesWithFilterOk() throws Exception {
        Role role1 = m_roleManager.addRole(new UserImpl("foo"));
        role1.getProperties().put("key", "value1");
        role1.getProperties().put("keyA", "valueA");
        Role role2 = m_roleManager.addRole(new GroupImpl("bar"));
        role2.getProperties().put("key", "value2");
        role2.getProperties().put("keyB", "value1");
        
        Filter filter;

        filter = new FilterImpl("(key=value1)");
        assertSameRoles(new Role[]{ role1 }, m_roleManager.getRoles(filter));

        filter = new FilterImpl("(key=value2)");
        assertSameRoles(new Role[]{ role2 }, m_roleManager.getRoles(filter));

        filter = new FilterImpl("(key=value*)");
        assertSameRoles(new Role[]{ role1, role2 }, m_roleManager.getRoles(filter));

        filter = new FilterImpl("(|(key=value1)(keyB=value1))");
        assertSameRoles(new Role[]{ role1, role2 }, m_roleManager.getRoles(filter));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(String, String)}.
     */
    public void testGetRolesWithKeyValuePairOk() throws Exception {
        Role role1 = m_roleManager.addRole(new UserImpl("foo"));
        role1.getProperties().put("key", "value1");
        role1.getProperties().put("keyA", "valueA");
        Role role2 = m_roleManager.addRole(new GroupImpl("bar"));
        role2.getProperties().put("key", "value2");
        role2.getProperties().put("keyB", "value1");
        
        assertSameRoles(new Role[]{ role1 }, m_roleManager.getRoles("key", "value1"));
        assertSameRoles(new Role[]{ role2 }, m_roleManager.getRoles("key", "value2"));
        assertSameRoles(new Role[0], m_roleManager.getRoles("key", "value"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoles(org.osgi.framework.Filter)}.
     */
    public void testGetRolesWithoutFilterOk() {
        Role role1 = m_roleManager.addRole(new UserImpl("foo"));
        Role role2 = m_roleManager.addRole(new GroupImpl("bar"));
        
        assertSameRoles(new Role[]{ role2, role1 }, m_roleManager.getRoles(null));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#getRoleByName(java.lang.String)}.
     */
    public void testGetUserAnyoneRoleByName() {
        assertSame(RoleRepository.USER_ANYONE, m_roleManager.getRoleByName(Role.USER_ANYONE));
    }

    /**
     * Tests whether removing a role from a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveBasicRoleYieldsEventOk() throws Exception {
        final GroupImpl role = (GroupImpl) m_roleManager.addRole(new GroupImpl("foo"));
        role.addMember(RoleRepository.USER_ANYONE);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.removeMember(RoleRepository.USER_ANYONE);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemoveExistingRoleOk() {
        UserImpl role = new UserImpl("foo");
        assertSame(role, m_roleManager.addRole(role));
        
        assertTrue(m_roleManager.removeRole(role));
        assertEquals(0, m_roleManager.getRoles(null).size());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemoveNonExistingRoleOk() {
        UserImpl role1 = new UserImpl("foo");
        UserImpl role2 = new UserImpl("bar");
        assertSame(role1, m_roleManager.addRole(role1));

        assertFalse(m_roleManager.removeRole(role2));
        assertEquals(1, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests that removing a null {@link RoleChangeListener} instance does not work.
     */
    public void testRemoveNullRoleChangeListenerFail() throws Exception {
        try {
            m_roleManager.removeRoleChangeListener(null);
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException e) {
            // Ok; expected...
        }
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.RoleRepository#removeRole(Role)}.
     */
    public void testRemovePredefinedRoleFails() {
        m_roleManager.addRole(new UserImpl("foo"));

        Role role = RoleRepository.USER_ANYONE;
        assertFalse(m_roleManager.removeRole(role));

        assertEquals(1, m_roleManager.getRoles(null).size());
    }

    /**
     * Tests whether removing a role from a group causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveRequiredRoleYieldsEventOk() throws Exception {
        final GroupImpl role = (GroupImpl) m_roleManager.addRole(new GroupImpl("foo"));
        role.addRequiredMember(RoleRepository.USER_ANYONE);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.removeMember(RoleRepository.USER_ANYONE);
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing a valid {@link RoleChangeListener} instance works.
     */
    public void testRemoveRoleChangeListenerOk() throws Exception {
        // Should succeed...
        m_roleManager.removeRoleChangeListener(new RoleChangeListener() {
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
        final Role role = m_roleManager.addRole(new UserImpl("john.doe"));
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
     * Tests that adding a role that does not inherit from RoleImpl does not work and yields an exception.
     */
    public void testRemoveRoleWithInvalidRoleFail() {
        try {
            m_roleManager.removeRole(new Role() {
                public String getName() {
                    return "A User";
                }
                public Dictionary getProperties() {
                    return new Properties();
                }
                public int getType() {
                    return Role.USER;
                }
            });
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that removing a null-role does not work and yields an exception.
     */
    public void testRemoveRoleWithNullRoleFail() {
        try {
            m_roleManager.removeRole(null);
            
            fail("Exception expected!");
        } catch (IllegalArgumentException e) {
            // Ok; expected
        }
    }

    /**
     * Tests whether removing a credential from a user causes an event to be emitted to the {@link RoleRepository}.
     */
    public void testRemoveUserCredentialYieldsEventOk() throws Exception {
        final UserImpl role = (UserImpl) m_roleManager.addRole(new UserImpl("john.doe"));
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

        m_roleManager = new RoleRepository(new MemoryRoleRepositoryStore());
        m_roleManager.addRoleChangeListener(new RoleChangeListener() {
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
        
        m_roleManager.start();
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
