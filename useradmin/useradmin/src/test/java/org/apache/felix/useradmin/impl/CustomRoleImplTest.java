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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Tests whether using custom roles in the backend work as expected and yield the proper events.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CustomRoleImplTest extends TestCase
{
    static class MyGroupImpl extends MyUserImpl implements Group {
        private final Map m_members;
        private final Map m_requiredMembers;
        
        public MyGroupImpl(String name) {
            super(name, GROUP);
            m_members = new HashMap();
            m_requiredMembers = new HashMap();
        }

        public boolean addMember(Role role) {
            String name = role.getName();
            if (m_requiredMembers.containsKey(name) || m_members.containsKey(name)) {
                return false;
            }
            return m_members.put(name, role) == null;
        }

        public boolean addRequiredMember(Role role) {
            String name = role.getName();
            if (m_requiredMembers.containsKey(name) || m_members.containsKey(name)) {
                return false;
            }
            return m_requiredMembers.put(name, role) == null;
        }

        public Role[] getMembers() {
            Role[] result = new Role[m_members.size()];
            return (Role[]) m_members.values().toArray(result);
        }

        public Role[] getRequiredMembers() {
            Role[] result = new Role[m_requiredMembers.size()];
            return (Role[]) m_requiredMembers.values().toArray(result);
        }

        public boolean removeMember(Role role) {
            String name = role.getName();
            if (m_requiredMembers.remove(name) != null) {
                return true;
            } else if (m_members.remove(name) != null) {
                return true;
            }
            return false;
        }
    }
    
    static class MyRoleImpl implements Role {
        private final String m_name;
        private final int m_type;
        private final Hashtable m_props;
        
        public MyRoleImpl(String name, int type) {
            m_name = name;
            m_type = type;
            m_props = new Hashtable();
        }

        public String getName() {
            return m_name;
        }

        public Dictionary getProperties() {
            return m_props;
        }

        public int getType() {
            return m_type;
        }
    }
    
    static class MyRoleRepositoryStore implements RoleRepositoryStore {
        private final ConcurrentMap m_entries = new ConcurrentHashMap();

        public Role addRole(String roleName, int type) throws IOException {
            if (roleName == null) {
                throw new IllegalArgumentException("Name cannot be null!");
            }

            Role role = createRole(roleName, type);

            Object result = m_entries.putIfAbsent(roleName, role);
            return (result == null) ? role : null;
        }

        public Role getRoleByName(String roleName) throws Exception {
            if (roleName == null) {
                throw new IllegalArgumentException("Role name cannot be null!");
            }
            return (Role) m_entries.get(roleName);
        }

        public Role[] getRoles(String filterValue) throws Exception {
            Collection roles = m_entries.values();

            Filter filter = null;
            if (filterValue != null) {
                filter = FrameworkUtil.createFilter(filterValue);
            }
            
            List matchingRoles = new ArrayList();
            Iterator rolesIter = roles.iterator();
            while (rolesIter.hasNext()) {
                Role role = (Role) rolesIter.next();
                if ((filter == null) || filter.match(role.getProperties())) {
                    matchingRoles.add(role);
                }
            }

            Role[] result = new Role[matchingRoles.size()];
            return (Role[]) matchingRoles.toArray(result);
        }

        public Role removeRole(String roleName) throws Exception {
            if (roleName == null) {
                throw new IllegalArgumentException("Name cannot be null!");
            }
            Role role = getRoleByName(roleName);
            boolean result = m_entries.remove(roleName, role);
            return result ? role : null;
        }

        private Role createRole(String roleName, int type) {
            Role role;
            if (type == Role.USER) {
                role = new MyUserImpl(roleName);
            } else if (type == Role.GROUP) {
                role = new MyGroupImpl(roleName);
            } else {
                throw new IllegalArgumentException("Invalid role type!");
            }
            return role;
        }
    }
    
    static class MyUserImpl extends MyRoleImpl implements User {
        private final Hashtable m_creds;
        
        public MyUserImpl(String name) {
            this(name, USER);
        }
        
        protected MyUserImpl(String name, int type) {
            super(name, type);
            m_creds = new Hashtable();
        }

        public Dictionary getCredentials() {
            return m_creds;
        }

        public boolean hasCredential(String key, Object value) {
            if (m_creds.containsKey(key)) {
                Object v = m_creds.get(key);
                return ((v == value) || ((v != null) && v.equals(value)));
            }
            return false;
        }
    }
    
    private CountDownLatch m_latch;
    private RoleRepository m_repository;

    /**
     * Tests that adding a basic member to a group-role yields an event. 
     */
    public void testAddBasicGroupMemberYieldsEvent() throws Exception {
        final Group role = (Group) m_repository.addRole("testGroup", Role.GROUP);
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                Role anyone = m_repository.getRoleByName(Role.USER_ANYONE);
                assertTrue(role.addMember(anyone));
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding an existing role does not yield an event. 
     */
    public void testAddExistingRoleDoesNotYieldEvent() throws Exception {
        assertNotNull(m_repository.addRole("testUser", Role.USER));

        m_latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            public void run() {
                assertNull(m_repository.addRole("testUser", Role.USER));
            };
        }).start();

        assertFalse(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a new role yields an event. 
     */
    public void testAddNewRoleYieldsEvent() throws Exception {
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                assertNotNull(m_repository.addRole("testUser", Role.USER));
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that adding a required member to a group-role yields an event. 
     */
    public void testAddRequiredGroupMemberYieldsEvent() throws Exception {
        final Group role = (Group) m_repository.addRole("testGroup", Role.GROUP);
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                Role anyone = m_repository.getRoleByName(Role.USER_ANYONE);
                assertTrue(role.addRequiredMember(anyone));
            };
        }).start();

        assertTrue(m_latch.await(100, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the properties of a user-role yields an event. 
     */
    public void testAddRolePropertiesYieldsEvent() throws Exception {
        final Role role = m_repository.addRole("testUser", Role.USER);
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().put("key", "value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the credentials of a user-role yields an event. 
     */
    public void testAddUserCredentialsYieldsEvent() throws Exception {
        final User role = (User) m_repository.addRole("testUser", Role.USER);
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().put("key", "value");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the credentials of a user-role yields an event. 
     */
    public void testChangePropertiesYieldsEvent() throws Exception {
        final Role role = m_repository.addRole("testUser", Role.USER);
        role.getProperties().put("key", "value1");
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().put("key", "value2");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the credentials of a user-role yields an event. 
     */
    public void testChangeUserCredentialsYieldsEvent() throws Exception {
        final User role = (User) m_repository.addRole("testUser", Role.USER);
        role.getCredentials().put("key", "value1");
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().put("key", "value2");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing an existing role does yield an event. 
     */
    public void testRemoveExistingRoleYieldsEvent() throws Exception {
        assertNotNull(m_repository.addRole("testUser", Role.USER));

        m_latch = new CountDownLatch(1);

        new Thread(new Runnable() {
            public void run() {
                assertTrue(m_repository.removeRole("testUser"));
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing a non-existing role does not yield an event. 
     */
    public void testRemoveNonExistingRoleDoesNotYieldEvent() throws Exception {
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                assertFalse(m_repository.removeRole("testUser"));
            };
        }).start();

        assertFalse(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the properties of a user-role yields an event. 
     */
    public void testRemoveRolePropertiesYieldsEvent() throws Exception {
        final Role role = m_repository.addRole("testUser", Role.USER);
        role.getProperties().put("key", "value");
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getProperties().remove("key");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that changing the credentials of a user-role yields an event. 
     */
    public void testRemoveUserCredentialsYieldsEvent() throws Exception {
        final User role = (User) m_repository.addRole("testUser", Role.USER);
        role.getCredentials().put("key", "value");
        assertNotNull(role);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                role.getCredentials().remove("key");
            };
        }).start();

        assertTrue(m_latch.await(1, TimeUnit.SECONDS));
    }

    /**
     * Tests that removing a member from a group-role yields an event. 
     */
    public void testRemovingGroupMemberYieldsEvent() throws Exception {
        final Role anyone = m_repository.getRoleByName(Role.USER_ANYONE);
        final Group role = (Group) m_repository.addRole("testGroup", Role.GROUP);
        assertNotNull(role);
        role.addRequiredMember(anyone);
        
        m_latch = new CountDownLatch(1);
        
        new Thread(new Runnable() {
            public void run() {
                assertTrue(role.removeMember(anyone));
            };
        }).start();

        assertTrue(m_latch.await(100, TimeUnit.SECONDS));
    }
    
    /**
     * Set up for all test cases.
     */
    protected void setUp() throws Exception
    {
        m_repository = new RoleRepository(new MyRoleRepositoryStore());
        m_repository.addRoleChangeListener(new RoleChangeListener() {
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
}
