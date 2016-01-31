package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.CbComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentDictionary;
import org.apache.felix.dm.lambda.callbacks.CbTypeDictionary;

/**
 * Builds a Dependency Manager Factory Configuration Adapter Component. For each new Config Admin factory configuration matching the factoryPid, 
 * an adapter will be created based on the adapter implementation class. The adapter will be registered with the specified interface, 
 * and with the specified adapter service properties. Depending on the propagate parameter, every public factory configuration properties 
 * (which don't start with ".") will be propagated along with the adapter service properties.  
 * 
 * <p> Example that defines a factory configuration adapter service for the "foo.bar" factory pid:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *         factoryPidAdapter(adapter -> adapter
 *             .impl(DictionaryImpl.class)
 *             .factoryPid("foo.bar").cb(ServiceImpl::updated)
 *             .propagate()
 *             .withSrv(LogService.class, log -> log.optional()));
 *    }
 * }
 * }</pre>
 */
public interface FactoryPidAdapterBuilder extends ComponentBuilder<FactoryPidAdapterBuilder> {
    /**
     * Specifies the factory pid used by the adapter.
     * @param pid the factory pid.
     * @return this builder
     */
    FactoryPidAdapterBuilder factoryPid(String pid);
    
    /**
     * Specifies a class name which fqdn represents the factory pid. Usually, this class can optionally be annotated with metatypes bnd annotations.
     * @param pidClass the class that acts as the factory pid
     * @return this builder
     */
    FactoryPidAdapterBuilder factoryPid(Class<?> pidClass);
    
    /**
     * Specifies if the public properties (not starting with a dot) should be propagated in the adapter service properties (false by default).
     * @return this builder.
     */
    FactoryPidAdapterBuilder propagate();
    
    /**
     * Specifies if the public properties (not starting with a dot) should be propagated in the adapter service properties (false by default).
     * @param propagate true if the public properties should be propagated in the adapter service properties (false by default).
     * @return this builder.
     */
    FactoryPidAdapterBuilder propagate(boolean propagate);
    
    /**
     * Specifies a callback method that will be called on the component instances when the configuration is injected
     * @param updateMethod the method to call on the component instances when the configuration is available ("updated" by default).
     * The following method signatures are supported:
     * 
     * <pre> {@code
     *    method(Dictionary properties)
     *    method(Component component, Dictionary properties)
     * }</pre>
     * 
     * @return this builder
     */
    FactoryPidAdapterBuilder cb(String updateMethod);
    
    /**
     * Specifies a callback instance method that will be called on a given object instance when the configuration is injected
     * @param updateMethod the method to call on the given object instance when the configuration is available ("updated" by default).
     * The following method signatures are supported:
     * 
     * <pre> {@code
     *    method(Dictionary properties)
     *    method(Component component, Dictionary properties)
     * }</pre>
     *
     * @param callbackInstance the Object instance on which the updated callback will be invoked.
     * @return this builder
     */
    FactoryPidAdapterBuilder cb(Object callbackInstance, String updateMethod);
    
    /**
     * Specifies a callback method reference that will be called on one of the component classes when the configuration is injected.
     * 
     * @param <U> the type of the component implementation class on which the callback is invoked on.
     * @param callback the method to call on one of the component classes when the configuration is available.
     * @return this builder
     */
    <U> FactoryPidAdapterBuilder cb(CbTypeDictionary<U> callback);
    
    /**
     * Specifies a callback method reference that will be called on one of the component classes when the configuration is injected
     * 
     * @param <U> the type of the component implementation class on which the callback is invoked on.
     * @param callback the reference to a method on one of the component classes. The method may takes as parameter a Component and a Dictionary.
     * @return this builder
     */
    <U> FactoryPidAdapterBuilder cb(CbTypeComponentDictionary<U> callback);
    
    /**
     * Specifies a callback instance method reference that will be called on a given object instance when the configuration is injected
     * 
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * a Dictionary parameter.
     * @return this builder
     */
    FactoryPidAdapterBuilder cbi(CbDictionary callback);

    /**
     * Specifies a callback instance method reference that will be called on a given object instance when the configuration is injected.
     * 
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * Dictionary parameter. 
     * @return this builder
     */
    FactoryPidAdapterBuilder cbi(CbComponentDictionary callback);
}
