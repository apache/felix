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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Provides an observable {@link Dictionary} implementation that emits change 
 * events for the put and remove operations aside checking for security 
 * permissions for all accessor methods.
 */
class ObservableDictionary extends Dictionary implements Serializable {

    private static final long serialVersionUID = 9223154895541178975L;

    /**
     * Provides a listener for changes to a {@link ObservableDictionary}.
     */
    static interface DictionaryChangeListener {

        /**
         * Called when a new entry is added.
         * 
         * @param key the key of the entry;
         * @param value the value associated to the key.
         */
        void entryAdded(Object key, Object value);
        
        /**
         * Called when an entry is changed.
         * 
         * @param key the key of the entry;
         * @param oldValue the old value associated to the key;
         * @param newValue the new value associated to the key.
         */
        void entryChanged(Object key, Object oldValue, Object newValue);
        
        /**
         * Called when an entry is removed.
         * 
         * @param key the key of the entry.
         */
        void entryRemoved(Object key);
    }

    /**
     * Provides a wrapper to convert an {@link Iterator} to an {@link Enumeration} implementation.
     */
    static final class IteratorEnumeration implements Enumeration {
        
        private final Iterator m_iterator;

        /**
         * Creates a new {@link IteratorEnumeration}.
         * 
         * @param iterator the {@link Iterator} to convert to a {@link Enumeration}, cannot be <code>null</code>.
         */
        public IteratorEnumeration(Iterator iterator) {
            m_iterator = iterator;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasMoreElements() {
            return m_iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Object nextElement() {
            return m_iterator.next();
        }
    }
    
    /**
     * Converts a given {@link Dictionary} implementation to a {@link Map} implementation.
     * 
     * @param dictionary the dictionary to convert, cannot be <code>null</code>.
     * @return a {@link Map} instance with all the same key-value pairs as the given dictionary, never <code>null</code>.
     */
    private static ConcurrentMap convertToMap(Dictionary dictionary) {
        ConcurrentMap result = new ConcurrentHashMap();
        if (dictionary instanceof Map) {
            result.putAll((Map) dictionary);
        } else {
            Enumeration keyEnum = dictionary.keys();
            while (keyEnum.hasMoreElements()) {
                Object key = keyEnum.nextElement();
                result.put(key, dictionary.get(key));
            }
        }
        return result;
    }
    
    private final ConcurrentMap m_properties;
    private final String m_getAction;
    private final String m_changeAction;

    private transient volatile DictionaryChangeListener m_listener;

    /**
     * Creates a new, empty, {@link ObservableDictionary} instance.
     */
    public ObservableDictionary(String getAction, String changeAction) {
        m_getAction = getAction;
        m_changeAction = changeAction;
        m_properties = new ConcurrentHashMap();
    }

    /**
     * Creates a new {@link ObservableDictionary} instance with the given dictionary as defaults.
     * 
     * @param dictionary the defaults to set for this properties, cannot be <code>null</code>.
     */
    public ObservableDictionary(String getAction, String changeAction, Dictionary dictionary) {
        if (dictionary == null) {
            throw new IllegalArgumentException("Dictionary cannot be null!");
        }
        m_getAction = getAction;
        m_changeAction = changeAction;
        m_properties = convertToMap(dictionary);
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration elements() {
        Collection values = m_properties.values();
        return new IteratorEnumeration(values.iterator());
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || (getClass() != object.getClass())) {
            return false;
        }

        ObservableDictionary other = (ObservableDictionary) object;
        if (m_properties == null) {
            if (other.m_properties != null) {
                return false;
            }
        } else if (!m_properties.equals(other.m_properties)) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }

        if (m_getAction != null) {
            checkPermissions(getAsPermissionKey(key), m_getAction);
        }

        return m_properties.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + ((m_properties == null) ? 0 : m_properties.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return m_properties.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration keys() {
        Collection keys = m_properties.keySet();
        return new IteratorEnumeration(keys.iterator());
    }

    /**
     * {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null!");
        }

        if (m_changeAction != null) {
            checkPermissions(getAsPermissionKey(key), m_changeAction);
        }

        Object oldValue = m_properties.put(key, value);
        
        final DictionaryChangeListener listener = m_listener;
        if (listener != null) {
            if (oldValue == null) {
                listener.entryAdded(key, value);
            } else {
                listener.entryChanged(key, oldValue, value);
            }
        }

        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public Object remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }

        if (m_changeAction != null) {
            checkPermissions(getAsPermissionKey(key), m_changeAction);
        }

        Object oldValue = m_properties.remove(key);
        final DictionaryChangeListener listener = m_listener;
        if (listener != null) {
            listener.entryRemoved(key);
        }
        
        return oldValue;
    }

    /**
     * Sets a new {@link DictionaryChangeListener} to observe changes to this dictionary.
     * 
     * @param listener the listener to add, can be <code>null</code> to stop listening for changes.
     */
    public void setDictionaryChangeListener(DictionaryChangeListener listener) {
        m_listener = listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public int size() {
        return m_properties.size();
    }

    /**
     * @param key
     * @return
     */
    protected String getAsPermissionKey(Object key) {
        String k = UserAdminPermission.ADMIN;
        if (key instanceof String) {
            k = (String) key;
        }
        return k;
    }

    /**
     * Verifies whether the caller has the right permissions to get or change the given key.
     * 
     * @param key the name of the property that is to be accessed or changed, cannot be <code>null</code>;
     * @param action the action name to perform, cannot be <code>null</code>.
     * @throws SecurityException in case the caller has not the right permissions to perform the action.
     */
    private void checkPermissions(String key, String action) throws SecurityException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new UserAdminPermission(key, action));
        }
    }
}
