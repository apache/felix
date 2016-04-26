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

/**
 * Implementation for a configuration event.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationEventImpl extends Event {
    private final String m_pid;
    
    public ConfigurationEventImpl(String pid, Dictionary<String, Object> conf) {
        super(conf);
        m_pid = pid;
    }
    
    public String getPid() {
        return m_pid;
    }
     
    @Override
    public int compareTo(Event other) {
        return m_pid.compareTo(((ConfigurationEventImpl) other).m_pid);
    }

    @Override
    public int hashCode() {
        return m_pid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
    	return m_pid.equals(((ConfigurationEventImpl) obj).m_pid);
    }

    @SuppressWarnings("unchecked")
	@Override
    public Dictionary<String, Object> getProperties() {
        return getEvent();
    }
}
