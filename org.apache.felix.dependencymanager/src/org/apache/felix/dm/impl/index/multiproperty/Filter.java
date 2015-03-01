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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Filter {
	private boolean m_valid = true;
	private Map<String, Property> m_properties = new HashMap<>();
	private Set<String> m_propertyKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	
	private Filter() {
	}
	
	// Sample valid filter string (&(objectClass=OBJECTCLASS)(&(model=MODEL)(concept=CONCEPT)(role=ROLE)(!(context=*))))
	public static Filter parse(String filterString) {
		Filter filter = new Filter();
		StringTokenizer tokenizer = new StringTokenizer(filterString, "(&|=)", true);
		
		String token = null;
		String prevToken = null;
		String key = null;
		StringBuilder valueBuilder = new StringBuilder();
		boolean negate = false;

		while (tokenizer.hasMoreTokens()) {
			prevToken = token;
			token = tokenizer.nextToken();
			if (token.equals("|")) {
				// we're not into OR's
				filter.m_valid = false;
				break;
			}
			if (token.equals("!")) {
				negate = true;
			} else if (token.equals("=")) {
				key = prevToken.toLowerCase();
			} else if (key != null) {
				if (!token.equals(")")) {
					valueBuilder.append(token); // might be superseded by a &
				}
				if (token.equals(")")) {
					// set complete
					if (filter.m_properties.containsKey(key)) {
						// set current property to multivalue
						Property property = filter.m_properties.get(key);
						property.addValue(valueBuilder.toString(), negate);
					} else {
						Property property = new Property(negate, key, valueBuilder.toString());
						filter.m_properties.put(key, property);
						filter.m_propertyKeys.add(key);
					}
					negate = false;
					key = null;
					valueBuilder = new StringBuilder();
				}
			} 
		}
		return filter;
	}
	
	public boolean containsProperty(String propertyKey) {
		return m_properties.containsKey(propertyKey);
	}
	
	public Set<String> getPropertyKeys() {
		return m_properties.keySet();
	}
	
	public Property getProperty(String key) {
		return m_properties.get(key);
	}
	
	public boolean isValid() {
		if (!m_valid) {
			return m_valid;
		} else {
			// also check the properties
			Iterator<Property> propertiesIterator = m_properties.values().iterator();
			while (propertiesIterator.hasNext()) {
				Property property = propertiesIterator.next();
				if (!property.isValid()) {
					return false;
				}
			}
		}
		return true;
	}
	
	public static void main(String args[]) {
		Filter parser = Filter.parse("(&(objectClass=OBJECTCLASS)(&(a=x)(a=n)(a=y)(b=y)(c=z)))");
		System.out.println("key: " + parser.createKey());
	}

	protected String createKey() {
		StringBuilder builder = new StringBuilder();
		Iterator<String> keys = m_propertyKeys.iterator();
		
		while (keys.hasNext()) {
			String key = keys.next();
			Property prop = m_properties.get(key);
			if (!prop.isWildcard()) {
				Iterator<String> values = prop.getValues().iterator();
				while (values.hasNext()) {
					String value = values.next();
					builder.append(key);
					builder.append("=");
					builder.append(value);
					if (keys.hasNext() || values.hasNext()) {
						builder.append(";");
					}
				}
			}
		}
		// strip the final ';' in case the last key was a wildcard property
		if (builder.charAt(builder.length() - 1) == ';') {
			return builder.toString().substring(0, builder.length() - 1);
		} else {
			return builder.toString();
		}
	}
	
}
