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
package org.apache.felix.dm.diagnostics;

/**
 * This represents a missing dependency. It can have any of the four types known to the Dependency Manager (service,
 * configuration, bundle and resource) or it can be a type defined by the programmer.
 * A missing dependency is defined by its name, its type and the bundle name of the bundle for which
 * this dependency is unavailable.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class MissingDependency {
	
	private final String name;
	private final String type;
	private final String bundleName;
	
	public MissingDependency(String name, String type, String bundleName) {
		this.name = name;
		this.type = type;
		this.bundleName = bundleName;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getBundleName() {
		return bundleName;
	}
	
	@Override
	public String toString() {
		return "Missing dependency: " 
				+ "name = " + name + " "
				+ "type = " + type + " "
				+ "bundleName = " + bundleName;
	}

}
