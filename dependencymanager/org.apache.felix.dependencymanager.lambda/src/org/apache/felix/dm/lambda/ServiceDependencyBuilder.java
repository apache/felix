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
package org.apache.felix.dm.lambda;

import java.util.Dictionary;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.ServiceReference;

/**
 * Builds a Dependency Manager Service Dependency. 
 * <p> Sample code:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       component(comp -> comp
 *          .impl(Pojo.class)
 *          .withSvc(ConfigurationAdmin.class, LogService.class) // varargs of optional (possibly NullObjects) dependencies injected in compatible class fields
 *          .withSvc(true, Coordinator.class, LogService.class) // varargs of required dependencies injected in compatible class fields
 *          .withSvc(ConfigurationAdmin.class, "(vendor=apache)") // service with a filter, injected in compatible class fields
 *          .withSvc(ConfigurationAdmin.class, "(vendor=apache)", true) // required service with a filter, injected in compatible class fields
 *          .withSvc(ConfigurationAdmin.class, "(vendor=apache)", true, "field") // required service with a filter, injected in a given class field name
 *          .withSvc(HttpService.class, svc -> svc.required().add(Pojo::setHttpService)) // required dependency injected using a method reference
 *          .withSvc(Tracked.class, svc -> svc.optional().add(Pojo::addTracked)) // optional dependency, injected using method ref, after the start() callback
 *    }
 * }}</pre>
 * 
 * @param <S> the type of the service dependency
 */
public interface ServiceDependencyBuilder<S> extends DependencyBuilder<ServiceDependency>, ServiceCallbacksBuilder<S, ServiceDependencyBuilder<S>> {
    /**
     * Configures the service dependency filter
     * @param filter the service filter
	 * @return this builder
     */
    ServiceDependencyBuilder<S> filter(String filter);
    
    /**
     * Configures this dependency with the given ServiceReference.
     * @param ref the service reference
	 * @return this builder
     */
    ServiceDependencyBuilder<S> ref(ServiceReference<S> ref);
    
    /**
     * Configures this dependency as optional.
     * @return this builder
     */
    ServiceDependencyBuilder<S> optional();

    /**
     * Configures this dependency as required.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> required();
    
    /**
     * Configures whether this dependency is required or not.
     * 
     * @param required true if the dependency is required, false if not.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> required(boolean required);
    
    /**
     * Configures debug mode
     * @param label the label used by debug messages
	 * @return this builder
     */
    ServiceDependencyBuilder<S> debug(String label);
    
    /**
     * Propagates the dependency properties to the component service properties.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> propagate();
  
    /**
     * Configures whether the dependency properties must be propagated or not to the component service properties.
     * 
     * @param propagate true if the service dependency properties should be propagated to the properties provided by the component using this dependency.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> propagate(boolean propagate);
    
    /**
     * Configures a method that can is called in order to get propagated service properties.
     * 
     * @param instance an object instance
     * @param method the method name to call on the object instance. This method returns the propagated service properties.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> propagate(Object instance, String method);
    
    /**
     * Specifies a function that is called to get the propagated service properties for this service dependency. 
     * @param propagate a function that is called to get the propagated service properties for this service dependency. 
     * @return this builder
     */
    ServiceDependencyBuilder<S> propagate(Function<ServiceReference<S>, Dictionary<String, Object>> propagate);

    /**
     * Specifies a function that is called to get the propagated service properties for this service dependency. 
     * @param propagate a function that is called to get the propagated service properties for this service dependency. 
     * @return this builder
     */
    ServiceDependencyBuilder<S> propagate(BiFunction<ServiceReference<S>, S, Dictionary<String, Object>> propagate);
    
    /**
     * Sets the default implementation if the service is not available.
     * @param defaultImpl the implementation used by default when the service is not available.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> defImpl(Object defaultImpl);
    
    /**
     * Sets a timeout for this dependency. A timed dependency blocks the invoker thread if the required dependency is currently unavailable, until it comes up again.
     * @param timeout the timeout to wait in milliseconds when the service disappears. If the timeout expires, an IllegalStateException is thrown
     * when the missing service is invoked.
     * 
     * @return this builder
     */
    ServiceDependencyBuilder<S> timeout(long timeout);
    
    /**
     * Injects this dependency in all fields matching the dependency type.
     * @return this builder
     */
    ServiceDependencyBuilder<S> autoConfig();
    
    /**
     * Configures whether or not the dependency can be injected in all fields matching the dependency type. 
     * @param autoConfig true if the dependency can be injected in all fields matching the dependency type
     * @return this builder
     */
    ServiceDependencyBuilder<S> autoConfig(boolean autoConfig);
    
    /**
     * Injects this dependency on the field with the given name
     * @param field the field name where the dependency must be injected
     * @return this builder
     */
    ServiceDependencyBuilder<S> autoConfig(String field);                
}
