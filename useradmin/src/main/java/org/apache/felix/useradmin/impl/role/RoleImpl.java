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

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.service.useradmin.Role;

/**
 * Provides a default implementation of {@link Role} that is not security-aware 
 * and does not keep track of changes to its properties.
 * <p>
 * This implementation should be wrapped in an {@link ObservableRole} when 
 * returned from the UserAdmin implementation. 
 * </p>
 */
public class RoleImpl implements Serializable, Role {

	private static final long serialVersionUID = 6403795608776837916L;
	
    private final Properties m_properties;
    private final String m_name;
    private final int m_type;

    /**
     * Creates a new {@link RoleImpl} instance of type {@link Role#ROLE} and a given name.
     * 
     * @param name the name of this role, cannot be <code>null</code> or empty.
     */
    public RoleImpl(String name) {
        this(Role.ROLE, name);
    }

    /**
     * Creates a new {@link RoleImpl} instance with a given type and name.
     * 
     * @param type the type of this role, should be {@link Role#ROLE}, {@link Role#USER} or {@link Role#GROUP};
     * @param name the name of this role, cannot be <code>null</code> or empty.
     */
    protected RoleImpl(int type, String name) {
        if (name == null || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty!");
        }
        m_type = type;
        m_name = name;
        m_properties = new Properties();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        RoleImpl other = (RoleImpl) obj;
        if (m_name == null) {
            if (other.m_name != null) {
                return false;
            }
        } else if (!m_name.equals(other.m_name)) {
            return false;
        }

        if (m_type != other.m_type) {
            return false;
        }

        return true;
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
     * {@inheritDoc}
     */
    public int getType() {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_name == null) ? 0 : m_name.hashCode());
        result = prime * result + m_type;
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Role(" + getName() + ")";
    }
}
