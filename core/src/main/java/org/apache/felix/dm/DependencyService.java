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

import org.osgi.framework.BundleContext;

/**
 * Interface to the component, so dependencies can interact with it.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface DependencyService {
    /**
     * Will be called when the dependency becomes available.
     * 
     * @param dependency the dependency
     */
    public void dependencyAvailable(Dependency dependency);
    
    /**
     * Will be called when the dependency becomes unavailable .
     * 
     * @param dependency the dependency
     */
    public void dependencyUnavailable(Dependency dependency);
    
    /**
     * Will be called when the dependency changes.
     * 
     * @param dependency the dependency
     */
    public void dependencyChanged(Dependency dependency);

    /**
     * Returns the component instance.
     * 
     * @return component instance
     */
    public Object getService(); // is also defined on the Service interface
    
    /**
     * Initializes the component. Instantiates it and injects the default injectables such
     * as {@link BundleContext} and {@link DependencyManager}.
     */
    public void initService(); // was an implementation method TODO we use it in ConfDepImpl but should not (probably)
    
    /**
     * Returns <code>true</code> if this component is registered. In other words, all
     * its required dependencies are available.
     * 
     * @return <code>true</code> if the component is registered
     */
    public boolean isRegistered(); // impl method
    
    /**
     * Returns a list of all instances that are part of the composition for this component.
     * 
     * @return an array of instances
     */
    public Object[] getCompositionInstances(); // impl method
    
    /**
     * Returns <code>true</code> if this component is instantiated.
     * 
     * @return <code>true</code> if this component is instantiated
     */
    public boolean isInstantiated();
    
    /**
     * Can be called by the dependency whenever it wants to invoke callback methods.
     */
    public void invokeCallbackMethod(Object[] instances, String methodName, Class[][] signatures, Object[][] parameters);

    /**
     * Returns the component interface.
     * 
     * @return the component interface
     */
    public Component getServiceInterface();
    
    /**
     * Injects the dependency into autoconfig class field (if any) of the dependency service.
     * @param dependency
     */
    public void autoConfig(final Dependency dependency);

    /**
     * Propagates the dependency properties to the ones provided by the dependency service.
     * No effet if the dependency is not configured with the setPropagate method.
     * 
     * @param dependency the dependency whose properties will be propagated to the service properties of this dependency service.
     * 
     * @see ServiceDependency#setPropagate(boolean)
     * @see ServiceDependency#setPropagate(String)
     */
    public void propagate(final Dependency dependency);
}
