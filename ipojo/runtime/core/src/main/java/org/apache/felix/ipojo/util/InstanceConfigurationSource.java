/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;

import java.util.*;

/**
 * A context source giving access to system properties.
 */
public class InstanceConfigurationSource implements ContextSource {

    /**
     * The instance configuration.
     */
    private Dictionary<String,Object> m_configuration;

    /**
     * Listener list.
     */
    private List<ContextListener> m_listeners = new ArrayList<ContextListener>();

    public InstanceConfigurationSource(Dictionary<String, Object> configuration) {
        m_configuration = configuration;
    }

    /**
     * The instance is reconfigured.
     * Changes are computed by comparing the old and new configuration.
     * @param newConfiguration the new instance configuration
     */
    public void reconfigure(Dictionary<String, Object> newConfiguration) {
        // Copy the current configuration and update the field.
        Hashtable<String, Object> configuration = new Hashtable<String, Object>();
        Enumeration<String> enumeration = m_configuration.keys();
        while (enumeration.hasMoreElements()) {
            final String key = enumeration.nextElement();
            configuration.put(key, m_configuration.get(key));
        }
        // We now have a copy of the current configuration.
        // We must set the field to the new configuration, as updates are going to be fired.
        // so we must have reflected the updates already.
        m_configuration = newConfiguration;

        // We use the same loop to find lost and updated properties.
        enumeration = configuration.keys();
        while (enumeration.hasMoreElements()) {
            final String key = enumeration.nextElement();
            final Object newValue = newConfiguration.get(key);
            final Object oldValue = configuration.get(key);
            if (newValue == null)  {
                // If we don't have the property anymore, notify the departure.
                fireUpdate(key, null);
            } else {
                // The new configuration still has a value, is it the same ?
                if (! newValue.equals(oldValue)) {
                    fireUpdate(key, newValue);
                }
            }
        }

        // Do we have new properties ?
        enumeration = newConfiguration.keys();
        while (enumeration.hasMoreElements()) {
            final String key = enumeration.nextElement();
            if (configuration.get(key) == null) {
                Object newValue = newConfiguration.get(key);
                fireUpdate(key, newValue);
            }
        }
    }

    private void fireUpdate(String key, Object newValue) {
        for (ContextListener listener : m_listeners) {
            listener.update(this, key, newValue);
        }
    }

    public Object getProperty(String property) {
        return m_configuration.get(property);
    }

    public Dictionary getContext() {
        return m_configuration;
    }

    public void registerContextListener(ContextListener listener, String[] properties) {
        if (! m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    public void unregisterContextListener(ContextListener listener) {
        m_listeners.remove(listener);
    }
}
