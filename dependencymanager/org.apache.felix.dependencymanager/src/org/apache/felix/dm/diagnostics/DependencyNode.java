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

import org.apache.felix.dm.ComponentDependencyDeclaration;

class DependencyNode extends DependencyGraphNode {
	
	private ComponentDependencyDeclaration m_dependencyDeclaration;
	
	public DependencyNode(ComponentDependencyDeclaration dependencyDeclaration) {
		m_dependencyDeclaration = dependencyDeclaration;
	}
	
	public ComponentDependencyDeclaration getDependencyDeclaration() {
		return m_dependencyDeclaration;
	}
	
	public boolean isUnavailableRequired() {
		return m_dependencyDeclaration.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED;
	}
	
	public boolean isUnavailableOptional() {
		return m_dependencyDeclaration.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_OPTIONAL;
	}
	
	public boolean isUnavailable() {
		return m_dependencyDeclaration.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_OPTIONAL
				|| m_dependencyDeclaration.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED;
	}
	
	@Override
	public String toString() {
		return m_dependencyDeclaration.getName();
	}

}
