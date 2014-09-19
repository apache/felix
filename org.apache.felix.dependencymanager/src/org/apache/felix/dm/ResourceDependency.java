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

import java.net.URL;

/**
 * A resource dependency is a dependency on a resource. A resource in this context is an object that is
 * identified by a URL. Resources should somehow be provided by an external component, the resource
 * provider. These dependencies then react on them becoming available or not. Use cases for such dependencies
 * are resources that are embedded in bundles, in a workspace or some remote or local repository, etc.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ResourceDependency extends Dependency, ComponentDependencyDeclaration, ResourceHandler {
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. When you specify callbacks, the auto configuration 
     * feature is automatically turned off, because we're assuming you don't need it in this 
     * case.
     * 
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public ResourceDependency setCallbacks(String added, String removed);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * 
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
     public ResourceDependency setCallbacks(String added, String changed, String removed);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
     public ResourceDependency setCallbacks(Object instance, String added, String removed);
    
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
     public ResourceDependency setCallbacks(Object instance, String added, String changed, String removed);
        
    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in any attributes in the service implementation that
     * are of the same type as this dependency. Default is on.
     * 
     * @param autoConfig the value of auto config
     * @return this service dependency
     */
     public ResourceDependency setAutoConfig(boolean autoConfig);
    
    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name.
     * 
     * @param instanceName the name of attribute to auto config
     * @return this service dependency
     */
     public ResourceDependency setAutoConfig(String instanceName);

     /**
      * Sets the resource for this dependency.
      * 
      * @param resource the URL of the resource
      */
     public ResourceDependency setResource(URL resource);

     /**
      * Determines if this is a required dependency or not.
      * 
      * @param required <code>true</code> if the dependency is required
      */
     public ResourceDependency setRequired(boolean required);

     /**
      * Sets the filter condition for this resource dependency.
      * 
      * @param resourceFilter the filter condition
      */
     public ResourceDependency setFilter(String resourceFilter);

     /** @see ResourceDependency#setPropagate(Object, String) */
     public ResourceDependency setPropagate(boolean propagate);

     /**
      * Sets an Object instance and a callback method used to propagate some properties to the provided service properties.
      * The method will be invoked on the specified object instance and must have one of the following signatures:<p>
      * <ul><li>Dictionary callback(ServiceReference, Object service) 
      * <li>Dictionary callback(ServiceReference)
      * </ul>
      * @param instance the Object instance which is used to retrieve propagated service properties 
      * @param method the method to invoke for retrieving the properties to be propagated to the service properties.
      * @return this service dependency.
      */
     public ResourceDependency setPropagate(Object instance, String method);
}
