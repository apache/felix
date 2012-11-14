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


import org.apache.felix.useradmin.impl.RoleChangeListener;
import org.apache.felix.useradmin.impl.role.ObservableDictionary.DictionaryChangeListener;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Provides an implementation of {@link Role}.
 */
public class RoleImpl implements Serializable, Role, DictionaryChangeListener {
	
    private static final long serialVersionUID = -6292833161748591485L;

	private final ObservableProperties m_properties;
    private final String m_name;
    private final int m_type;
    
    private volatile RoleChangeListener m_listener;

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
        m_properties = new ObservableProperties(null, UserAdminPermission.CHANGE_PROPERTY);
        m_properties.setDictionaryChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public final void entryAdded(Object key, Object value) {
        RoleChangeListener listener = m_listener;
        if (listener != null) {
            listener.propertyAdded(this, key, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void entryRemoved(Object key) {
        RoleChangeListener listener = m_listener;
        if (listener != null) {
            listener.propertyRemoved(this, key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void entryChanged(Object key, Object oldValue, Object newValue) {
        RoleChangeListener listener = m_listener;
        if (listener != null) {
            listener.propertyChanged(this, key, oldValue, newValue);
        }
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
     * Sets the {@link RoleChangeListener} for this role implementation.
     * 
     * @param listener the listener to set, may be <code>null</code> to stop listening.
     */
    public void setRoleChangeListener(RoleChangeListener listener) {
        m_listener = listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Role(" + getName() + ")";
    }
}
