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

import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("rawtypes")
class TestReference implements ServiceReference {
	Properties props = new Properties();

	public TestReference() {
	}

	public void addProperty(String key, String value) {
		/*
		 * Property keys are case-insensitive. -> see @
		 * org.osgi.framework.ServiceReference
		 */
		props.put(key.toLowerCase(), value);
	}

	public void addProperty(String key, long value) {
		props.put(key, value);
	}

	public void addProperty(String key, int value) {
		props.put(key, value);
	}

	public void addProperty(String key, boolean value) {
		props.put(key, value);
	}

	public void addProperty(String key, String[] multiValue) {
		props.put(key, multiValue);
	}

	@Override
	public Object getProperty(String key) {
		return props.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return props.keySet().toArray(new String[] {});
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	@Override
	public int compareTo(Object reference) {
		// TODO Auto-generated method stub
		return 0;
	}

}