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
	 * The component is waiting for some required dependencies defined in the Activator. 
	 * At this point the component has not yet been called in its init callback.
	 */
	WAITING_FOR_REQUIRED, 
	
	/**
	 * The component is instantiated and waiting for dynamic required dependencies defined in init callback.
	 */
	INSTANTIATED_AND_WAITING_FOR_REQUIRED, 
	
    /**
     * The component is starting. At this point, all required dependencies have been injected (including 
     * dynamic dependencies added from the init method), but the start callback has not yet been called and 
     * the service has not been registered yet.
     */
    STARTING,
        
	/**
	 * The component has been called in its started callback. At this point, the component has not yet been registered
	 * in the service registry.
	 */
	STARTED,

	/**
	 * The component is started. At this point, the component:<p>
	 * <ul>
	 * <li> has been called in its start callback 
	 * <li> the optional dependency callbacks have been invoked (if some optional dependencies are available)
	 * <li> and the service has been registered
	 * </ul>
	 */
	TRACKING_OPTIONAL,
	
    /**
     * the component is stopping. At this point, the service is still registered.
     */
    STOPPING,
    
    /**
     * the component is stopped. At this point, the service has been unregistered.
     */
    STOPPED
}
