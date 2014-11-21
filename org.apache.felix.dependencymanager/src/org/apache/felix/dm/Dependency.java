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
 * Generic dependency for a component. 
 * Can be added to a single component. Can be available, or not.. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Dependency {
    /**
     * Returns <code>true</code> if this a required dependency. Required dependencies
     * are dependencies that must be available before the component can be activated.
     * 
     * @return <code>true</code> if the dependency is required
     */
    public boolean isRequired();

    /**
     * Returns <code>true</code> if the dependency is available.
     * 
     * @return <code>true</code> if the dependency is available
     */
    public boolean isAvailable();

    /**
     * Returns <code>true</code> if auto configuration is enabled for this dependency.
     * Auto configuration means that a dependency is injected in the component instance
     * when it's available, and if it's unavailable, a "null object" will be inserted
     * instead.
     * 
     * @return true if auto configuration is enabled for this dependency
     */
    public boolean isAutoConfig();
    
    /**
     * Returns the name of the member in the class of the component instance
     * to inject into. If you specify this, not all members of the right
     * type will be injected, only the member whose name matches.
     * 
     * @return the name of the member in the class of the component instance to inject into
     */
    public String getAutoConfigName();
    
    /**
     * Determines if the properties associated with this dependency should be propagated to
     * the properties of the service registered by the component they belong to.
     * 
     * @see Dependency#getProperties()
     * 
     * @return <code>true</code> if the properties should be propagated
     */
    public boolean isPropagated();

    /**
     * Returns the properties associated with this dependency.
     * 
     * @see Dependency#isPropagated()
     * 
     * @return the properties
     */
    public <K,V> Dictionary<K,V> getProperties();
}
