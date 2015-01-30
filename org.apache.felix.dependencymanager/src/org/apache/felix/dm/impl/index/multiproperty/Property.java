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
package org.apache.felix.dm.impl.index.multiproperty;

import java.util.Set;
import java.util.TreeSet;
/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */

public class Property {
	boolean m_negate;
	boolean m_valid = true;
	String m_key;
	String m_value;
	Set<String> m_values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	
	public Property() {
	}
	
	public Property(boolean negate, String key, String value) {
		super();
		m_negate = negate;
		m_key = key.toLowerCase();
		m_values.add(value);
		m_value = value;
	}

	public void setNegate(boolean negate) {
		this.m_negate = negate;
	}
	
	public void setKey(String key) {
		this.m_key = key.toLowerCase();
	}
	
	public void addValue(String value, boolean negate) {
		if (this.m_negate != negate) {
			// multiproperty with different negations, causes invalid configuration.
			m_valid = false;
		}
		if (this.m_value == null) {
			// value has not bee set yet
			this.m_value = value;
		}
		if (value != null) {
			m_values.add(value);
		}
	}
	
	public boolean isNegate() {
		return m_negate;
	}
	
	public String getKey() {
		return m_key;
	}
	
	public String getValue() {
		return m_value;
	}
	
	public Set<String> getValues() {
		return m_values;
	}
	
	public boolean isWildcard() {
		return "*".equals(m_value);
	}
	
	public boolean isMultiValue() {
		return m_values.size() > 1;
	}

	public String toString() {
		return "Property [negate=" + m_negate + ", key=" + m_key + ", values="
				+ m_values + "]";
	}
	
	public boolean isValid() {
		return m_valid;
	}
}
