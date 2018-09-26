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

import java.util.Dictionary;

/**
 * Base interface for a Dependency Manager component.
 */
public interface AbstractComponent {

    /**
     * Returns the instance that make up this component. If the component has a composition of instances,
     * then the first instance of the composition is returned. Null is returned if the component has not 
     * even been instantiated.
     * 
     * @return the component instances
     */
	public <U> U getInstance();
	
    /**
     * Returns the composition instances that make up this component, or just the
     * component instance if it does not have a composition, or an empty array if
     * the component has not even been instantiated.
     * 
     * @return the component instances
     */
	public Object[] getInstances();
	
    /**
     * Returns the component service properties.
     * The returned dictionary is either empty if no service properties were defined for this component,
     * or copy of the existing service properties associated with this component.
     * 
     * @return a copy of the service properties associated to this component or an empty dictionary 
     *         if no service properties were defined for this component.
     */
	public <K,V> Dictionary<K,V> getServiceProperties();
	
    /**
     * Returns the dependency manager associated with this component.
     * @return the dependency manager associated with this component.
     */
	public DependencyManager getDependencyManager();

	/**
	 * Returns the component description (dependencies, service provided, etc ...).
	 * @return the component description (dependencies, service provided, etc ...).
	 */
	public ComponentDeclaration getComponentDeclaration();
	
}
