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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Provides a stub group that has "weak" references (based on their names) to its basic/required members.
 */
final class StubGroupImpl implements Group {
    
    private final String m_name;
    private final List m_basicMembers;
    private final List m_requiredMembers;
    private final Dictionary m_credentials;
    private final Dictionary m_properties;

    /**
     * Creates a new {@link StubGroupImpl} instance.
     */
    public StubGroupImpl(String name) {
        m_name = name;
        m_properties = new Properties();
        m_credentials = new Properties();
        m_basicMembers = new ArrayList();
        m_requiredMembers = new ArrayList();
    }

    /**
     * {@inheritDoc}
     */
    public boolean addMember(Role role) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean addMember(String roleName) {
        return m_basicMembers.add(roleName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addRequiredMember(Role role) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean addRequiredMember(String roleName) {
        return m_requiredMembers.add(roleName);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StubGroupImpl other = (StubGroupImpl) obj;
        if (m_name == null) {
            if (other.m_name != null) {
                return false;
            }
        } else if (!m_name.equals(other.m_name)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getCredentials() {
        return m_credentials;
    }

    /**
     * Returns the names of all basic members.
     * 
     * @return a list with basic member names (as String!), never <code>null</code>.
     */
    public List getMemberNames() {
        return m_basicMembers;
    }

    /**
     * {@inheritDoc}
     */
    public Role[] getMembers() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getProperties() {
        return m_properties;
    }

    /**
     * Returns the names of all required members.
     * 
     * @return a list with required member names (as String!), never <code>null</code>.
     */
    public List getRequiredMemberNames() {
        return m_requiredMembers;
    }

    /**
     * {@inheritDoc}
     */
    public Role[] getRequiredMembers() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return Role.GROUP;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCredential(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeMember(Role role) {
        throw new UnsupportedOperationException();
    }
}
