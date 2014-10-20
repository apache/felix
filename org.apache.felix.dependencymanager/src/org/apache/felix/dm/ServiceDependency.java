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

import org.osgi.framework.ServiceReference;

/**
 * Service dependency that can track an OSGi service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceDependency extends Dependency, ComponentDependencyDeclaration {
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. When you specify callbacks, the auto configuration 
     * feature is automatically turned off, because we're assuming you don't need it in this 
     * case.
     * 
     * @param add the method to call when a service was added
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
	public ServiceDependency setCallbacks(String add, String remove);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * 
     * @param add the method to call when a service was added
     * @param change the method to call when a service was changed
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
	public ServiceDependency setCallbacks(String add, String change, String remove);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * @param add the method to call when a service was added
     * @param change the method to call when a service was changed
     * @param remove the method to call when a service was removed
     * @param swap the method to call when the service was swapped due to addition or 
     * removal of an aspect
     * @return this service dependency
     */
	public ServiceDependency setCallbacks(String add, String change, String remove, String swap);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param add the method to call when a service was added
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
	public ServiceDependency setCallbacks(Object instance, String add, String remove);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case.
     * 
     * @param instance the instance to call the callbacks on
     * @param add the method to call when a service was added
     * @param change the method to call when a service was changed
     * @param remove the method to call when a service was removed
     * @return this service dependency
     */
	public ServiceDependency setCallbacks(Object instance, String add, String change, String remove);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case.
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @param swapped the method to call when the service was swapped due to addition or 
     * removal of an aspect
     * @return this service dependency
     */    
	public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped);

    /**
     * Sets the required flag which determines if this service is required or not.
     * A ServiceDependency is false by default.
     * 
     * @param required the required flag
     * @return this service dependency
     */
	public ServiceDependency setRequired(boolean required);

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name. Dependency services will be injected
     * in the following kind of fields:<p>
     * <ul>
     * <li> a field having the same type as the dependency. If the field may be accessed by anythread, then
     * the field should be declared volatile, in order to ensure visibility when the field is auto injected concurrently.
     * 
     * <li> a field which is assignable to an <code>Iterable&#60;T&#62;</code> where T must match the dependency type. 
     * In this case, an Iterable will be injected by DependencyManager before the start callback is called.
     * The Iterable field may then be traversed to inspect the currently available dependency services. The Iterable 
     * can possibly be set to a final value so you can choose the Iterable implementation of your choice
     * (for example, a CopyOnWrite ArrayList, or a ConcurrentLinkedQueue).
     * 
     * <li> a <code>Map&#60;K,V&#62;</code> where K must match the dependency type and V must exactly equals <code>Dictionary</code>. 
     * In this case, a ConcurrentHashMap will be injected by DependencyManager before the start callback is called.
     * The Map may then be consulted to lookup current available dependency services, including the dependency service
     * properties (the map key holds the dependency service, and the map value holds the dependency service properties).
     * 
     * The Map field may be set to a final value so you can choose a Map of your choice (Typically a ConcurrentHashMap).
     * 
     * A ConcurrentHashMap is "weakly consistent", meaning that when traversing 
     * the elements, you may or may not see any concurrent updates made on the map. So, take care to traverse 
     * the map using an iterator on the map entry set, which allows to atomically lookup pairs of Dependency service/Service properties.
     * </ul> 
     * 
     * <p> Here are some example using an Iterable:
     * <blockquote>
     * 
     * <pre>
     * 
     * public class SpellChecker {
     *    // can be traversed to inspect currently available dependencies
     *    final Iterable&#60;DictionaryService&#62; dictionaries = new ConcurrentLinkedQueue<>();
     *    
     *    Or
     *    
     *    // will be injected by DM automatically and can be traversed any time to inspect all currently available dependencies.
     *    volatile Iterable&#60;DictionaryService&#62; dictionaries = null;
     * }
     * 
     * </pre>
     * </blockquote>
     * 
     * Here are some example using a Map:
     * <blockquote>
     * 
     * <pre>
     * 
     * public class SpellChecker {
     *    // can be traversed to inspect currently available dependencies
     *    final Map&#60;DictionaryService, Dictionary&#62; dictionaries = new ConcurrentLinkedQueue<>();
     *    
     *    or
     *    
     *    // will be injected by DM automatically and can be traversed to inspect currently available dependencies
     *    volatile Map&#60;DictionaryService, Dictionary&#62; dictionaries = null;
     *
     *    void iterateOnAvailableServices() {                 
     *       for (Map.Entry<MyService, Dictionary> entry : this.services.entrySet()) {
     *           MyService currentService = entry.getKey();
     *           Dictionary currentServiceProperties = entry.getValue();
     *           // ...
     *       }
     *    }
     * }
     * 
     * </pre>
     * </blockquote>
     * 
     * @param autoConfig the name of attribute to auto configure
     * @return this service dependency
     */
	public ServiceDependency setAutoConfig(boolean autoConfig);

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name.
     * 
     * @param instanceName the name of attribute to auto config
     * @return this service dependency
     * @see #setAutoConfig(boolean)
     */
	public ServiceDependency setAutoConfig(String instanceName);

    /**
     * Sets the name of the service that should be tracked. 
     * 
     * @param serviceName the name of the service
     * @return this service dependency
     */
    public ServiceDependency setService(Class<?> serviceName);

    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, or the name and a filter. In the latter case, the filter is used
     * to track the service and should only return services of the type that was specified
     * in the name. To make sure of this, the filter is actually extended internally to
     * filter on the correct name.
     * 
     * @param serviceName the name of the service
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    public ServiceDependency setService(Class<?> serviceName, String serviceFilter);
    
    /**
     * Sets the filter for the services that should be tracked. Any service object
     * matching the filter will be returned, without any additional filter on the
     * class.
     * 
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    public ServiceDependency setService(String serviceFilter);
    
    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, or the name and a reference. In the latter case, the service reference
     * is used to track the service and should only return services of the type that was 
     * specified in the name.
     * 
     * @param serviceName the name of the service
     * @param serviceReference the service reference to track
     * @return this service dependency
     */
    public ServiceDependency setService(Class<?> serviceName, ServiceReference serviceReference);
    
    /**
     * Sets the default implementation for this service dependency. You can use this to supply
     * your own implementation that will be used instead of a Null Object when the dependency is
     * not available. This is also convenient if the service dependency is not an interface
     * (which would cause the Null Object creation to fail) but a class.
     * 
     * @param implementation the instance to use or the class to instantiate if you want to lazily
     *     instantiate this implementation
     * @return this service dependency
     */
    public ServiceDependency setDefaultImplementation(Object implementation);
    
    /**
     * Sets propagation of the service dependency properties to the provided service properties. Any additional
     * service properties specified directly are merged with these.
     */
    public ServiceDependency setPropagate(boolean propagate);
    
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
    public ServiceDependency setPropagate(Object instance, String method);
    
    /**
     * Enabled debug logging for this dependency instance. The logging is prefixed with the given identifier.
     * @param debugKey a prefix log identifier
     * @return this service dependency.
     */
    public ServiceDependency setDebug(String debugKey);
}
