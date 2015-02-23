/*
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
package org.apache.felix.dm.impl;

import java.net.URL;
import java.util.Dictionary;

import org.apache.felix.dm.context.Event;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceEventImpl extends Event {
    final Dictionary<Object, Object> m_resourceProperties;
    
    @SuppressWarnings("unchecked")
    public ResourceEventImpl(URL resource, Dictionary<?, ?> resourceProperties) {
        super(resource);
        m_resourceProperties = (Dictionary<Object, Object>) resourceProperties;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <K,V> Dictionary<K,V> getProperties() {
        return (Dictionary<K, V>) ((Dictionary<K,V>) m_resourceProperties == null ? EMPTY_PROPERTIES : m_resourceProperties);
    }

    public URL getResource() {
        return getEvent();
    }
        
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResourceEventImpl) {
            ResourceEventImpl r1 = this;
            ResourceEventImpl r2 = (ResourceEventImpl) obj;
            boolean match = r1.getResource().equals(r2.getResource());
            if (match) {
                Dictionary<?,?> d1 = getProperties();
                Dictionary<?,?> d2 = r2.getProperties();
                
                if (d1 == null) {
                	return d2 == null ? match : false;
                }
                else {
                	return d1.equals(d2);
                }
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getResource().hashCode();
        result = prime * result + ((getProperties() == null) ? 0 : getProperties().hashCode());
        return result;
    }

    @Override
    public int compareTo(Event that) {
        if (this.equals(that)) {
            return 0;
        }
        
        // Sort by resource name.
        return getResource().toString().compareTo(((ResourceEventImpl) that).getResource().toString());
    }
}
