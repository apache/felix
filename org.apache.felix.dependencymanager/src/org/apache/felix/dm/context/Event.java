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
package org.apache.felix.dm.context;

import java.util.Dictionary;
import java.util.Hashtable;

/** 
 * An event holds all data that belongs to some external event as it comes in via
 * the 'changed' callback of a dependency.
 */
public class Event<T> implements Comparable<Event<T>> {
    protected final static Dictionary<String, Object> EMPTY_PROPERTIES = new Hashtable<>();
    private final T m_event;    // the actual event object (a Service, a Bundle, a Configuration, etc ...)
    
    public Event(T event) {
        m_event = event;
    }

    @Override
    public int hashCode() {
        return m_event.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        // an instanceof check here is not "strong" enough with subclasses overriding the
        // equals: we need to be sure that a.equals(b) == b.equals(a) at all times
        if (obj != null && obj.getClass().equals(Event.class)) {
            return (((Event<T>) obj).m_event).equals(m_event);
        }
        return false;
    }
    
    @Override
    public int compareTo(Event<T> o) {
        return 0;
    }
    
    /**
     * Release the resources this event is holding (like service reference for example).
     */
    public void close() {
    }
    
    /**
     * Returns the actual event object wrapped by this event (a Service Dependency, a Bundle for Bundle Dependency, etc...).
     */
    public T getEvent() {
        return m_event;
    }
    
    /**
     * Returns the properties of the actual event object wrapped by this event (Service Dependency properties, ...).
     */
    public Dictionary<String, Object> getProperties() {
        return EMPTY_PROPERTIES;
    }
}
