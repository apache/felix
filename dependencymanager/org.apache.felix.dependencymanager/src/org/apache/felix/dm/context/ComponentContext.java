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
package org.apache.felix.dm.context;

import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import aQute.bnd.annotation.ProviderType;

/**
 * This interface is the entry point to the Component implementation context.
 * It is used by all DependencyManager Dependency implementations.
 * 
 * @see DependencyContext interface
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@ProviderType
public interface ComponentContext extends Component {
    /**
     * Returns the Component Executor gate that can be used to ensure proper component event serialization.
     * When you schedule a task in the component executor, your task is executed safely and you do not need
     * to managed synchronization (other external events, like service dependency events) will be queued
     * until your task has been executed). 
     */
    public Executor getExecutor();    
    
    /**
     * Returns the logger which can be used by the DependencyManager Dependencies implementations.
     */
    public Logger getLogger();
    
    /**
     * Returns the Component's bundle context
     * @return the Component's bundle context
     */
    public BundleContext getBundleContext();
    
    /**
     * Returns the Compoent's bundle.
     * @return the Compoent's bundle.
     */
    public Bundle getBundle();
    
    /**
     * Sets a threadpool that the component will use when handling external events
     * @param threadPool a threadpool used to handle component events and invoke the component's lifecycle callbacks
     */
    public void setThreadPool(Executor threadPool);
    
    /**
     * Starts the component. All initial dependencies previously added to the component will be started.
     */
    public void start();
    
    /**
     * Stops the component.
     */
    public void stop();
    
    /**
     * Is this component already started ?
     * @return true if this component has been started
     */
    public boolean isActive();
    
    /**
     * Is this component available (all required dependencies are available) ?
     * @return true if this component is available (all dependencies are available), or false
     */
    public boolean isAvailable();
    
    /**
     * Notifies the Component about a dependency event.
     * An event is for example fired when:<p>
     * <ul>
     * <li> a dependency service becomes available {@link EventType#ADDED}) 
     * <li> a dependenc service has changed is changed {@link EventType#CHANGED}) 
     * <li> a dependency service has been lost {@link EventType#REMOVED}) 
     * <li> a dependency service has been swapped by another {@link EventType#SWAPPED}) 
     * </ul> 
     * @param dc the dependency
     * @param type the dependency event type
     * @param e the dependency event
     * @see EventType
     */
    public void handleEvent(DependencyContext dc, EventType type, Event ... event);
   
    /**
     * Returns the list of dependencies that has been registered on this component
     * @return the list of dependencies that has been registered on this component
     */
    public List<DependencyContext> getDependencies();
    
    /**
     * Invokes a callback method on a given set of objects. An error is logged if the callback is not found in any of the object instances.
     * @param instances the component instances
     * @param methodName the method name
     * @param signatures the method signatures (types)
     * @param parameters the method parameters
     */
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters);
    
    /**
     * Invokes a callback method on a given set of objects.
     * @param instances the component instances
     * @param methodName the method name
     * @param signatures the method signatures (types)
     * @param parameters the method parameters
     * @param logIfNotFound true if a warning message should be logged in case the callback is not found in any of the object instances.
     */
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters, boolean logIfNotFound);
    
    /**
     * Returns the component instances
     * @return the component instances
     */
    public Object[] getInstances();
    
    /**
     * Returns the component instance field that is assignable to a given class type
     * @param clazz the type of an object that has to be injected in the component instance field
     * @return the name of the component instance field that can be assigned to an object having the same type as 
     * the "clazz" parameter
     */
    public String getAutoConfigInstance(Class<?> clazz);
    
    /**
     * Indicates if an object of the given class can be injected in one field of the component
     * @param clazz the class of an object that has to be injected in one of the component fields
     * @return true if the component can be injected with an object having the specified "clazz" type.
     */
    public boolean getAutoConfig(Class<?> clazz);
    
    /**
     * Returns the highest ranked dependency service instance for a given dependency
     * @param dc the dependency 
     * @return the highest ranked dependency service instance for a given dependency
     */
    public Event getDependencyEvent(DependencyContext dc);
    
    /**
     * Returns all the available dependency services for a given dependency
     * @param dc the dependency 
     * @return all the available dependency services for a given dependency
     */
    public Set<Event> getDependencyEvents(DependencyContext dc);
    
    /**
     * Creates a configuration for a given type backed by a given dictionary.
     * This method can be used by any custom Dependency Manager dependency that
     * needs to expose some configuration through a dynamic proxy interface.
     * 
     * @param type the configuration class, cannot be <code>null</code>;
     * @param config the configuration to wrap, cannot be <code>null</code>.
     * @return an instance of the given type that wraps the given configuration.
     */
    public <T> T createConfigurationType(Class<T> type, Dictionary<?, ?> config);
}
