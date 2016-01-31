package org.apache.felix.dm.lambda;

import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.lambda.callbacks.CbComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeDictionary;

/**
 * Builds a Dependency Manager Configuration Dependency.
 * By default, the updated callback is "updated", like in original DM API.
 * 
 * <p> Code example with a component that defines a Configuration Dependency. the ServiceImpl modified method
 * callback is declared using a method reference (see the "cb(ServiceImpl::modified)" code):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *         component(comp -> comp
 *           .impl(ServiceImpl.class)
 *           .withConf(conf -> conf.pid(ServiceConsumer.class).cb(ServiceImpl::modified)));  
 *    }
 * }
 * }</pre>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ConfigurationDependencyBuilder extends DependencyBuilder<ConfigurationDependency> { 
    /**
     * Sets the pid for this configuration dependency.
     * 
     * @param pid the configuration dependendency pid.
     * @return this builder
     */
    ConfigurationDependencyBuilder pid(String pid);
    
    /**
     * Sets the class which fqdn represents the pid for this configuration dependency. Usually, this class can optionally be annotated with metatypes bnd annotations.
     * 
     * @param pidClass the class which fqdn represents the pid for this configuration dependency.
     * @return this builder
     */
    ConfigurationDependencyBuilder pid(Class<?> pidClass);
    
    /**
     * Sets propagation of the configuration properties to the service properties (false by default). 
     * All public configuration properties (not starting with a dot) will be propagated to the component service properties.
     * 
     * @return this builder
     */
    ConfigurationDependencyBuilder propagate();
    
    /**
     * Sets propagation of the configuration properties to the service properties (false by default).
     * 
     * @param propagate true if all public configuration properties (not starting with a dot) must be propagated to the component service properties (false by default)
     * @return this builder
     */
    ConfigurationDependencyBuilder propagate(boolean propagate);
    
    /**
     * Configures whether or not the component instance should be instantiated at the time the updated callback is invoked. 
     * By default, when the callback is applied on an external object instance, the component is not instantiated, but in this case
     * you can force the creation of the component instances by calling this method.
     * 
     * @param needsInstance true if the component instance should be instantiated at the time the updated callback is invoked on an external object instance.
     * @return this builder
     */
    ConfigurationDependencyBuilder needsInstance(boolean needsInstance);
    
    /**
     * Sets a <code>callback</code> to call on the component instance(s) when the configuration is updated.
     * 
     * @param updateMethod the callback to call on the component instance(s) when the configuration is updated.
     * @return this builder
     */
    ConfigurationDependencyBuilder cb(String updateMethod);
    
    /**
     * Sets a <code>callback instance</code> to call on a given object instance when the configuration is updated.
     * When the updated method is invoked, the Component implementation has not yet been instantiated, unless you have called
     * the @link {@link #needsInstance(boolean)} method with "true".
     * 
     * @param callbackInstance the object instance on which the updatedMethod is invoked
     * @param updateMethod the callback to call on the callbackInstance when the configuration is updated.
     * @return this builder
     */
    ConfigurationDependencyBuilder cbi(Object callbackInstance, String updateMethod);

    /**
     * Sets a <code>callback</code> method reference used to invoke an update method. The method reference must point to a method from one of the component
     * implementation classes, and is invoked when the configuration is updated.
     *
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param callback the callback method reference which must point to a method from one of the component implementation classes. The method
     * takes as argument a Dictionary.
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder cb(CbTypeDictionary<T> callback);
    
    /**
     * Sets the <code>callback</code> method reference used to invoke an update method. The method reference must point to a method from one of the 
     * component implementation classes, and is invoked when the configuration is updated.
     *
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param callback the callback method reference used to invoke an update method on the component instance(s) when the configuration is updated.
     * The method takes as argument a Component and a Dictionary.
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder cb(CbTypeComponentDictionary<T> callback);
  
    /**
     * Sets a <code>callback instance</code> method reference used to invoke the update method. The method reference must point to an Object instance 
     * method which takes as argument a Dictionary.
     * 
     * @param updated a method reference that points to an Object instance method which takes as argument a Dictionary.
     * @return this builder
     */
    ConfigurationDependencyBuilder cbi(CbDictionary updated);   

    /**
     * Sets a <code>callback instance</code> method reference used to invoke the update method. The method reference must point to an Object instance method 
     * which takes as argument a Component and a Dictionary.
     * 
     * @param updated a method reference that points to an Object instance method which takes as argument a Component and a Dictionary.
     * @return this builder
     */
    ConfigurationDependencyBuilder cbi(CbComponentDictionary updated);   
}
