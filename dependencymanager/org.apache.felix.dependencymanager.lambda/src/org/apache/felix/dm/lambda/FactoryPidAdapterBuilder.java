package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.CbConfiguration;
import org.apache.felix.dm.lambda.callbacks.CbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionaryComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfiguration;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionary;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionaryComponent;

/**
 * Builds a Dependency Manager Factory Configuration Adapter Component. For each new Config Admin factory configuration matching the factoryPid, 
 * an adapter will be created based on the adapter implementation class. The adapter will be registered with the specified interface, 
 * and with the specified adapter service properties. Depending on the propagate parameter, every public factory configuration properties 
 * (which don't start with ".") will be propagated along with the adapter service properties.
 * 
 * This builded supports type safe configuration types. For a given factory configuration, you can specify an interface of your choice,
 * and DM will implement it using a dynamic proxy that converts interface methods to lookups in the actual factory configuration dictionary. 
 * 
 * <p> Example that defines a factory configuration adapter service for the "foo.bar" factory pid:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         factoryPidAdapter(adapter -> adapter
 *             .impl(DictionaryImpl.class)
 *             .factoryPid("foo.bar")
 *             .update(ServiceImpl::updated)
 *             .propagate()
 *             .withSvc(LogService.class, log -> log.optional()));
 *    }
 * }
 * }</pre>
 * 
 * <p> Example that defines a factory configuration adapter using a user defined configuration type
 * (the pid is by default assumed to match the fqdn of the configuration type):
 * 
 * <pre> {@code
 * 
 * public interface DictionaryConfiguration {
 *     public String getLanguage();
 *     public List<String> getWords();
 * }
 * 
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         factoryPidAdapter(adapter -> adapter
 *             .impl(DictionaryImpl.class)
 *             .factoryPid("foo.bar")
 *             .update(DictionaryConfiguration.class, ServiceImpl::updated)
 *             .propagate()
 *             .withSvc(LogService.class, log -> log.optional()));
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
     * Specifies if the public properties (not starting with a dot) should be propagated in the adapter service properties (false by default).
     * 
     * @return this builder.
     */
    FactoryPidAdapterBuilder propagate();
    
    /**
     * Specifies if the public properties (not starting with a dot) should be propagated in the adapter service properties (false by default).
     * 
     * @param propagate true if the public properties should be propagated in the adapter service properties (false by default).
     * @return this builder.
     */
    FactoryPidAdapterBuilder propagate(boolean propagate);
    
    /**
     * Specifies a callback method that will be called on the component instances when the configuration is injected.
     * 
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
    FactoryPidAdapterBuilder update(String updateMethod);
    
    /**
     * Sets a callback method to call on the component implementation class(es) when the configuration is updated. 
     * The callback is invoked with a configuration type argument.
     * 
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param updateMethod the callback to call on the component instance(s) when the configuration is updated.
     * @return this builder
     */
    FactoryPidAdapterBuilder update(Class<?> configType, String updateMethod);
    
    /**
     * Specifies a callback instance method that will be called on a given object instance when the configuration is injected.
     * 
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
    FactoryPidAdapterBuilder update(Object callbackInstance, String updateMethod);
    
    /**
     * Specifies a callback instance method that will be called on a given object instance when the configuration is injected.
     * The callback is invoked with a configuration type argument.
     * 
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callbackInstance the Object instance on which the updated callback will be invoked.
     * @param updateMethod the method to call on the given object instance when the configuration is available. The callback is invoked
     * with a configuration type argument (matching the configType you have specified.
     * @return this builder
     */
    FactoryPidAdapterBuilder update(Class<?> configType, Object callbackInstance, String updateMethod);
    
    /**
     * Specifies a method reference that will be called on one of the component classes when the configuration is injected.
     * The callback is invoked with a Dictionary argument.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param callback the method to call on one of the component classes when the configuration is available.
     * @return this builder
     */
    <T> FactoryPidAdapterBuilder update(CbDictionary<T> callback);
    
    /**
     * Specifies a method reference that will be called on one of the component classes when the configuration is injected.
     * The callback is invoked with a configuration type argument.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param <U> the configuration type accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the method to call on one of the component classes when the configuration is available.
     * @return this builder
     */
    <T, U> FactoryPidAdapterBuilder update(Class<U> configType, CbConfiguration<T, U> callback);
    
    /**
     * Specifies a method reference that will be called on one of the component classes when the configuration is injected
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param callback the reference to a method on one of the component classes. The method may takes as parameter a Dictionary and a Component.
     * @return this builder
     */
    <T> FactoryPidAdapterBuilder update(CbDictionaryComponent<T> callback);
    
    /**
     * Specifies a method reference that will be called on one of the component classes when the configuration is injected.
     * The callback is invoked with the following arguments: a configuration type, and a Component object.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param <U> the configuration type accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the reference to a method on one of the component classes. The method may takes as parameter a configuration type and a Component.
     * @return this builder
     */
    <T, U> FactoryPidAdapterBuilder update(Class<U> configType, CbConfigurationComponent<T, U> callback);

    /**
     * Specifies a method reference that will be called on a given object instance when the configuration is injected
     * 
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * a Dictionary parameter.
     * @return this builder
     */
    FactoryPidAdapterBuilder update(InstanceCbDictionary callback);

    /**
     * Specifies a method reference that will be called on a given object instance when the configuration is injected.
     * The callback is invoked with a type-safe configuration type argument.
     * 
     * @param <T> the configuration type accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * a configuration type parameter.
     * @return this builder
     */
    <T> FactoryPidAdapterBuilder update(Class<T> configType, InstanceCbConfiguration<T> callback);

    /**
     * Specifies a method reference that will be called on a given object instance when the configuration is injected.
     * 
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * Dictionary, and a Component parameter. 
     * @return this builder
     */
    FactoryPidAdapterBuilder update(InstanceCbDictionaryComponent callback);

    /**
     * Specifies a method reference that will be called on a given object instance when the configuration is injected.
     * 
     * @param <T> the configuration type accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the method to call on a given object instance when the configuration is available. The callback takes as argument a
     * configuration type, and a Component parameter. 
     * @return this builder
     */
    <T> FactoryPidAdapterBuilder update(Class<T> configType, InstanceCbConfigurationComponent<T> callback);
}
