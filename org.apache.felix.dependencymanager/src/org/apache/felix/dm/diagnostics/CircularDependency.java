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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.dm.ComponentDeclaration;

public class CircularDependency {
	
	private List<ComponentDeclaration> m_components = new ArrayList<>();
	
	void addComponent(ComponentDeclaration component) {
		m_components.add(component);
	}
	
	public List<ComponentDeclaration> getComponents() {
		return Collections.unmodifiableList(m_components);
	}
	
	@Override
	public String toString() {
		String result = "";
		for(ComponentDeclaration c : m_components) {
			result += " -> " + c.getName();
		}
		return result;
	}

}
