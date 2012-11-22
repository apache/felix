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

import java.io.Serializable;
import java.util.Dictionary;

import org.apache.felix.useradmin.impl.RoleChangeListener;
import org.apache.felix.useradmin.impl.role.ObservableDictionary.DictionaryChangeListener;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Provides an adapter for all {@link Role}s, allowing its changes to be 
 * externally observed.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObservableRole implements Serializable, Role, DictionaryChangeListener {

    private static final long serialVersionUID = -3706363718282775516L;

    protected final Role m_delegate;

    private final ObservableProperties m_properties;

    private volatile RoleChangeListener m_listener;

    /**
     * Creates a new {@link ObservableRole} instance.
     * 
     * @param role the role to observe for changes, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given role was <code>null</code>.
     */
    public ObservableRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }

        m_delegate = role;
        m_properties = new ObservableProperties(null, UserAdminPermission.CHANGE_PROPERTY, m_delegate.getProperties());
        m_properties.setDictionaryChangeListener(this);
    }
    
    /**
     * Wraps the given role as an (subclass of) {@link ObservableRole}.
     * <p>
     * If the given role is already an instance of {@link ObservableRole}, this
     * method simply returns the given role.
     * </p>
     * 
     * @param role the role to wrap, can be <code>null</code>.
     * @return an {@link ObservableRole} instance wrapping the given role, or 
     *         <code>null</code> if the given role was <code>null</code>.
     */
    public static ObservableRole wrap(Role role) {
        if (role == null) {
            return null;
        }
        if (role instanceof ObservableRole) {
            return (ObservableRole) role;
        }
        int type = role.getType();
        switch (type) {
            case Role.GROUP:
                return new ObservableGroup((Group) role);
                
            case Role.USER:
                return new ObservableUser((User) role);
            
            default:
                return new ObservableRole(role);
        }        
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
    public final void entryChanged(Object key, Object oldValue, Object newValue) {
        RoleChangeListener listener = m_listener;
        if (listener != null) {
            listener.propertyChanged(this, key, oldValue, newValue);
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

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }
        
        ObservableRole other = (ObservableRole) object;
        if (m_delegate == null) {
            if (other.m_delegate != null) {
                return false;
            }
        }
        else if (!m_delegate.equals(other.m_delegate)) {
            return false;
        }
        
        return true;
    }

    public String getName() {
        return m_delegate.getName();
    }

    public Dictionary getProperties() {
        return m_properties;
    }

    public int getType() {
        return m_delegate.getType();
    }

    public int hashCode() {
        return 31 + ((m_delegate == null) ? 0 : m_delegate.hashCode());
    }

    /**
     * Sets the {@link RoleChangeListener} for this role implementation.
     * 
     * @param listener the listener to set, may be <code>null</code> to stop listening.
     */
    public void setRoleChangeListener(RoleChangeListener listener) {
        m_listener = listener;
    }
    
    public String toString() {
        return m_delegate.toString();
    }
}
