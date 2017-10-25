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
package org.apache.felix.scr.impl.inject;

import java.util.Map;

import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * This object describes a constructor for a component.
 * The name ComponentConstructor has been chosen to avoid a clash with the
 * existing Constructor class.
 */
public interface ComponentConstructor<T> {

	public class ReferencePair<S> {
		public DependencyManager<S, ?> dependencyManager; // TODO check if we need this
		public DependencyManager.OpenStatus<S, ?> openStatus;
    }

	/**
	 * Create a new instance
	 * @param componentContext The component context
	 * @param parameterMap A map of reference parameters for handling references in the
	 *                     constructor
	 * @return The instance
	 * @throws Exception If anything goes wrong, like constructor can't be found etc.
	 */
    <S> T newInstance(ComponentContextImpl<T> componentContext,
    		           Map<ReferenceMetadata, ReferencePair<S>> parameterMap)
    throws Exception;
}
