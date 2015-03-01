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
package org.apache.felix.dm.tracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Actions which can be performed on a given customizer interface.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractCustomizerActionSet {

	enum Type { ADDED, MODIFIED, REMOVED }

	final List<CustomizerAction> m_actions = new ArrayList<>();

	public void addCustomizerAdded(Object item, Object related, Object object) {
		m_actions.add(new CustomizerAction(Type.ADDED, item, related, object));
	}
	
	public void addCustomizerModified(Object item, Object related, Object object) {
		m_actions.add(new CustomizerAction(Type.MODIFIED, item, related, object));
	}
	
	public void addCustomizerRemoved(Object item, Object related, Object object) {
		m_actions.add(new CustomizerAction(Type.REMOVED, item, related, object));
	}
	
	public void appendActionSet(AbstractCustomizerActionSet actionSet) {
		for (CustomizerAction action : actionSet.getActions()) {
			m_actions.add(action);
		}
	}
	
	abstract void execute();
	
	public List<CustomizerAction> getActions() {
		return m_actions;
	}
	
	@Override
	public String toString() {
		return "AbstractCustomizerActionSet [m_actions=" + m_actions + "]";
	}

	static class CustomizerAction {
		private final Type m_type;
		private final Object m_item;
		private final Object m_related;
		private final Object m_object;
		
		public CustomizerAction(Type type, Object item, Object related, Object object) {
			m_type = type;
			m_item = item;
			m_related = related;
			m_object = object;
		}
		
		public Type getType() {
			return m_type;
		}
		
		public Object getItem() {
			return m_item;
		}
		
		public Object getRelated() {
			return m_related;
		}
		
		public Object getObject() {
			return m_object;
		}

		@Override
		public String toString() {
			return "CustomizerAction [m_type=" + m_type + ", m_item=" + m_item
					+ ", m_related=" + m_related + ", m_object=" + m_object
					+ "]";
		}
	}
}
