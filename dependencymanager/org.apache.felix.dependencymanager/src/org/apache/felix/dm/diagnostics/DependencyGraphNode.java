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

class DependencyGraphNode {
	
	public enum DependencyGraphNodeState {
		UNDISCOVERED,
		DISCOVERED,
		PROCESSED
	};
	
	private List<DependencyGraphNode> m_successors = new ArrayList<>();
	private List<DependencyGraphNode> m_predecessors = new ArrayList<>();
	private DependencyGraphNodeState m_state = DependencyGraphNodeState.UNDISCOVERED;
	
	public void addSuccessor(DependencyGraphNode successor) {
		m_successors.add(successor);	
		successor.addPredecessor(this);
	}
	
	private void addPredecessor(DependencyGraphNode predecessor) {
		m_predecessors.add(predecessor);
	}
	
	public List<DependencyGraphNode> getSuccessors() {
		return Collections.unmodifiableList(m_successors);
	}
	
	public List<DependencyGraphNode> getPredecessors() {
		return Collections.unmodifiableList(m_predecessors);
	}
	
	void setState(DependencyGraphNodeState state) {
		m_state = state;
	}
	
	boolean isDiscovered() {
		return m_state == DependencyGraphNodeState.DISCOVERED;
	}
	
	boolean isUndiscovered() {
		return m_state == DependencyGraphNodeState.UNDISCOVERED;
	}
	
	boolean isProcessed() {
		return m_state == DependencyGraphNodeState.PROCESSED;
	}
	

}
