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

package org.apache.felix.useradmin.impl.role;

import java.security.Permission;
import java.util.Enumeration;


import org.apache.felix.useradmin.impl.role.ObservableDictionary;
import org.osgi.service.useradmin.UserAdminPermission;

import junit.framework.TestCase;

/**
 * Security-related test cases for {@link ObservableDictionary}.
 */
public class ObservableDictionarySecurityTest extends TestCase {

    private static final String GET_ACTION = UserAdminPermission.GET_CREDENTIAL;
    private static final String CHANGE_ACTION = UserAdminPermission.CHANGE_CREDENTIAL;
    
    private TestSecurityManager m_securityManager;
    private ObservableDictionary m_dict;

    /**
     * Tests that no special permissions are needed to access the {@link ObservableDictionary#size()} method.
     */
    public void testSize() throws SecurityException {
        assertEquals(0, m_dict.size());
    }

    /**
     * Tests that no special permissions are needed to access the {@link ObservableDictionary#isEmpty()} method.
     */
    public void testIsEmpty() throws SecurityException {
        assertTrue(m_dict.isEmpty());
    }

    /**
     * Tests that no special permissions are needed to access the {@link ObservableDictionary#elements()} method.
     */
    public void testElements() throws SecurityException {
        Enumeration elements = m_dict.elements();
        assertNotNull(elements);
        assertFalse(elements.hasMoreElements());
    }

    /**
     * Tests that with permission, the {@link ObservableDictionary#get(Object)} method can be accessed.
     */
    public void testGetObjectWithPermissionsOk() throws SecurityException {
        assertNull(m_dict.get("permFoo"));
    }

    /**
     * Tests that without permission, the {@link ObservableDictionary#get(Object)} method can not be accessed.
     */
    public void testGetObjectWithoutPermissionsFail() throws SecurityException {
        try {
            assertNull(m_dict.get("bar"));
            
            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that no special permissions are needed to access the {@link ObservableDictionary#keys()} method.
     */
    public void testKeys() throws SecurityException {
        Enumeration keys = m_dict.keys();
        assertNotNull(keys);
        assertFalse(keys.hasMoreElements());
    }

    /**
     * Tests that with permission, the {@link ObservableDictionary#put(Object, Object)} method can be accessed.
     */
    public void testPutObjectWithPermissionsOk() throws SecurityException {
        assertNull(m_dict.put("permKey", "value"));
    }

    /**
     * Tests that without permission, the {@link ObservableDictionary#put(Object, Object)} method can not be accessed.
     */
    public void testPutObjectWithoutPermissionsFail() throws SecurityException {
        try {
            assertNull(m_dict.put("key", "value"));

            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected
        }
    }

    /**
     * Tests that with permission, the {@link ObservableDictionary#remove(Object)} method can be accessed.
     */
    public void testRemoveObjectWithPermissionsOk() throws SecurityException {
        assertNull(m_dict.remove("permKey"));
    }

    /**
     * Tests that without permission, the {@link ObservableDictionary#remove(Object)} method can not be accessed.
     */
    public void testRemoveObjectWithoutPermissionsFail() throws SecurityException {
        try {
            assertNull(m_dict.remove("key"));
            
            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_securityManager = new TestSecurityManager();
        System.setSecurityManager(m_securityManager);
        
        m_dict = new ObservableDictionary(GET_ACTION, CHANGE_ACTION);
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        
        System.setSecurityManager(null);
    }
    
    /**
     * Provides a test security manager.
     */
    static final class TestSecurityManager extends SecurityManager {

        public void checkPermission(Permission perm) {
            // Cannot use instanceof as it requires a special permission as well...
            if ("UserAdminPermission".equals(perm.getClass().getSimpleName())) {
                String name = perm.getName();
                if ((name != null) && !name.startsWith("perm")) {
                    throw new SecurityException("Not allowed!");
                }
            }
            // Do not check for other permissions...
        }

        public void checkPermission(Permission perm, Object context) {
            // Do not check for other permissions...
        }
    }
}
