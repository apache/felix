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
package org.apache.felix.dm;

/**
 * Component states. Any state listeners registered using @link {@link Component#add(ComponentStateListener)} method
 * are notified with the following stated whenever the component state changes.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public enum ComponentState {
    /**
     * The component is not currently started, and is inactive.
     */
	INACTIVE, 
	
	/**
	 * The component is waiting for some required dependencies.
	 */
	WAITING_FOR_REQUIRED, 
	
	/**
	 * The component has all its initial required dependencies available, but is now waiting for some extra required
	 * dependencies which have been added after the component have been started (like from the component init method for example).
	 */
	INSTANTIATED_AND_WAITING_FOR_REQUIRED, 
	
	/**
	 * The component is active, and is now tracking available optional dependencies.
	 */
	TRACKING_OPTIONAL
}
