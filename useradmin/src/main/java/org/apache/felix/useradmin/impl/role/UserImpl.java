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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Provides a default implementation of {@link User} that is not security-aware 
 * and does not keep track of changes to its properties.
 * <p>
 * This implementation should be wrapped in an {@link ObservableUser} when 
 * returned from the UserAdmin implementation. 
 * </p>
 */
public class UserImpl extends RoleImpl implements User {

	private static final long serialVersionUID = 7332249440557443008L;
	
    private final Properties m_credentials;

    /**
     * Creates a new {@link UserImpl} instance with type {@link Role#USER}.
     * 
     * @param name the name of this user role, cannot be <code>null</code> or empty.
     */
    public UserImpl(String name) {
        this(Role.USER, name);
    }

    /**
     * Creates a new {@link UserImpl} instance with a given type.
     *
     * @param type the type of this role;
     * @param name the name of this role, cannot be <code>null</code> or empty.
     */
    protected UserImpl(int type, String name) {
        super(type, name);

        m_credentials = new Properties();
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getCredentials() {
        return m_credentials;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCredential(String key, Object value) {
        Object result = m_credentials.get(key);

        // Be a bit more lenient with the various results we can get...
        if (result instanceof String) {
            String s1 = (String) result;
            String s2;
            if (value instanceof byte[]) {
                s2 = new String((byte[]) value);
            } else if (value instanceof String) {
                s2 = (String) value;
            } else {
                // Not a string, nor a byte-array!
                return false;
            }
            
            return s1.equals(s2);
        } else if (result instanceof byte[]) {
            byte[] b1 = (byte[]) result;
            byte[] b2;
            if (value instanceof byte[]) {
                b2 = (byte[]) value;
            } else if (value instanceof String) {
                b2 = ((String) value).getBytes();
            } else {
                // Not a string, nor a byte-array!
                return false;
            }

            return Arrays.equals(b1, b2);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "User(" + getName() + ")";
    }
}
