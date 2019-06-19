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

import java.util.Dictionary;


import org.apache.felix.useradmin.impl.role.UserImpl;
import org.osgi.service.useradmin.Role;

import junit.framework.TestCase;

/**
 * Test case for {@link UserImpl}. 
 */
public class UserImplTest extends TestCase {

    /**
     * Tests that we can get the credentials for a {@link UserImpl}.
     */
    public void testGetCredentials() {
        UserImpl user = new UserImpl("foo");
        
        Dictionary dict = user.getCredentials();
        assertNotNull(dict);
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.RoleImpl#getType()}.
     */
    public void testGetType() {
        UserImpl user = new UserImpl("foo");
        
        assertEquals(Role.USER, user.getType());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.UserImpl#hasCredential(java.lang.String, java.lang.Object)}.
     */
    public void testHasExistingCredentialAlternativeValueTypeOk() {
        UserImpl user = new UserImpl("foo");
        
        Dictionary dict = user.getCredentials();
        dict.put("password", "secret");

        // Direct comparison...
        assertTrue(user.hasCredential("password", "secret"));

        // In case the given value is a byte[]...
        assertTrue(user.hasCredential("password", "secret".getBytes()));

        dict.put("password", "otherSecret".getBytes());
        
        // Direct comparison...
        assertTrue(user.hasCredential("password", "otherSecret".getBytes()));

        // In case the stored value is a byte[]...
        assertTrue(user.hasCredential("password", "otherSecret"));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.role.UserImpl#hasCredential(java.lang.String, java.lang.Object)}.
     */
    public void testHasExistingCredentialOk() {
        UserImpl user = new UserImpl("foo");
        
        Dictionary dict = user.getCredentials();
        dict.put("password", "secret");

        assertTrue(user.hasCredential("password", "secret"));
    }

    /**
     * Tests that {@link UserImpl#hashCode()} yields predictable results.
     */
    public void testHashCodeOk() {
        UserImpl user1 = new UserImpl("foo");
        UserImpl user2 = new UserImpl("foo");
        UserImpl user3 = new UserImpl("bar");
        
        assertTrue(user1.hashCode() == user2.hashCode());
        assertFalse(user1.hashCode() == user3.hashCode());
        assertFalse(user2.hashCode() == user3.hashCode());
    }
}
