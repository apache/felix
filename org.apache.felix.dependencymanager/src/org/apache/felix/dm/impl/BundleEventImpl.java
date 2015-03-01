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

import java.util.Dictionary;

import org.apache.felix.dm.context.Event;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleEventImpl extends Event {
    final BundleEvent m_event;
    
    public BundleEventImpl(Bundle bundle, BundleEvent event) {
        super(bundle);
        m_event = event;
    }
        
    public Bundle getBundle() {
        return getEvent();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Dictionary<String, Object> getProperties() {
        return getBundle().getHeaders();
    }
    
    public BundleEvent getBundleEvent() {
        return m_event;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BundleEventImpl) {
            return getBundle().getBundleId() == ((BundleEventImpl) obj).getBundle().getBundleId();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return getBundle().hashCode();
    }

    @Override
    public int compareTo(Event b) {
        return Long.compare(getBundle().getBundleId(), ((BundleEventImpl) b).getBundle().getBundleId());
    }
}
