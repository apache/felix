/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.useradmin.impl.role;

import java.util.Dictionary;

import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Provides an adapter for all {@link User}s, allowing its changes to be 
 * externally observed.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObservableUser extends ObservableRole implements User {
    
    private static final long serialVersionUID = 344574927109701877L;
    
    private final ObservableProperties m_credentials;
    
    /**
     * Creates a new {@link ObservableUser} instance.
     * 
     * @param user the user-role to observe for changes, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given user-role was <code>null</code>.
     */
    public ObservableUser(User user) {
        super(user);
        
        m_credentials = new ObservableProperties(UserAdminPermission.GET_CREDENTIAL, UserAdminPermission.CHANGE_CREDENTIAL, user.getCredentials());
        m_credentials.setDictionaryChangeListener(this);
    }

    public Dictionary getCredentials() {
        return m_credentials;
    }

    public boolean hasCredential(String key, Object value) {
        // Will throw a SecurityException if we're not allowed to do this!
        m_credentials.get(key);
        // We're allowed to do this; let the original implementation figure out
        // whether or not the test holds...
        return ((User) m_delegate).hasCredential(key, value);
    }
    
    public String toString()
    {
        return m_delegate.toString();
    }
}
