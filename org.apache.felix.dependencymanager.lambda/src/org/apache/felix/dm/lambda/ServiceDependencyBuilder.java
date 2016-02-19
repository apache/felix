package org.apache.felix.dm.lambda;

import java.util.Dictionary;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.ServiceReference;

/**
 * Builds a Dependency Manager Service Dependency. 
 * The Dependency is required by default, but you can
 * control the default mode (required or optional) using the "org.apache.felix.dependencymanager.lambda.dependencymode"
 * system property which can be set to either "required" or "optional" ("required" by default).
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
     * Configures this dependency as optional. By default, the dependency is required, but you can specify the default mode
     * using the "org.apache.felix.dependencymanager.lambda.dependencymode" system property.
     * @return this builder
     */
    ServiceDependencyBuilder<S> optional();

    /**
     * Configures this dependency as required.  By default, the dependency is required, but you can specify the default mode
     * using the "org.apache.felix.dependencymanager.lambda.dependencymode" system property.
	 * @return this builder
     */
    ServiceDependencyBuilder<S> required();
    
    /**
     * Configures whether this dependency is required or not.  By default, the dependency is required, but you can specify the default mode
     * using the "org.apache.felix.dependencymanager.lambda.dependencymode" system property.
     * 
     * @param required true if the dependency is required, false if not. Unlike with the original DM API, service dependencies are required by default.
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