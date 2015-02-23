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
package test;

import org.apache.felix.dm.context.Event;

/** in real life, this event might contain a service reference and service instance
 * or something similar
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventImpl extends Event { // the actual event object (a Service, a Bundle, a Configuration, etc ...)
	private final int m_id;

	public EventImpl() {
		this(1);
	}
	/** By constructing events with different IDs, we can simulate different unique instances. */
	public EventImpl(int id) {
		this (id, null);
	}
	
	public EventImpl(int id, Object event) {
	    super(event);
	    m_id = id;
	}
	

	@Override
	public int hashCode() {
		return m_id;
	}

	@Override
	public boolean equals(Object obj) {
		// an instanceof check here is not "strong" enough with subclasses overriding the
		// equals: we need to be sure that a.equals(b) == b.equals(a) at all times
		if (obj != null && obj.getClass().equals(EventImpl.class)) {
			return ((EventImpl) obj).m_id == m_id;
		}
		return false;
	}
	
    @Override
    public int compareTo(Event o) {
        EventImpl a = this, b = (EventImpl) o;
        if (a.m_id < b.m_id) {
            return -1;
        } else if (a.m_id == b.m_id){
            return 0;
        } else {
            return 1;
        }
    }            
}
