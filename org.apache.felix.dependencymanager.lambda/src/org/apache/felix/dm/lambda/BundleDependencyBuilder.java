package org.apache.felix.dm.lambda;

import java.util.Dictionary;
import java.util.function.Supplier;

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbComponentBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentBundle;
import org.osgi.framework.Bundle;

/**
 * Builds a Dependency Manager Bundle Dependency. The Dependency is required by default (unlike in the original Dependency Manager API).
 * 
 * <p> Example of a Component which tracks a started bundle having a given bundle symbolic name:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *         String BSN = "org.apache.felix.dependencymanager";
 *         component(comp -> comp
 *             .impl(MyComponent.class)
 *             .withBundle(b -> b.mask(Bundle.ACTIVE).filter("(Bundle-SymbolicName=" + BSN + ")").cb(MyComponent::add, MyComponent::remove)));
 *                  
 *    }
 * }
 * } </pre>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface BundleDependencyBuilder extends DependencyBuilder<BundleDependency> {
    /**
     * Enables auto configuration for this dependency. This means the component implementation (composition) will be
     * injected with this bundle dependency automatically.
     * 
     * @param autoConfig <code>true</code> to enable auto configuration
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder autoConfig(boolean autoConfig);

    /**
     * Enables auto configuration for this dependency. This means the component implementation (composition) will be
     * injected with this bundle dependency automatically.
     * 
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder autoConfig();

    /**
     * Sets the dependency to be required. By default, the dependency is required.
     * 
     * @param required <code>true</code> if this bundle dependency is required (true by default).
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder required(boolean required);

    /**
     * Sets the dependency to be required.
     * 
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder required();

    /**
     * Sets the bundle to depend on directly.
     * 
     * @param bundle the bundle to depend on
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder bundle(Bundle bundle);

    /**
     * Sets the filter condition to depend on. Filters are matched against the full manifest of a bundle.
     * 
     * @param filter the filter condition
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder filter(String filter);

    /**
     * Sets the bundle state mask to depend on. The OSGi BundleTracker explains this mask in more detail, but
     * it is basically a mask with flags for each potential state a bundle can be in.
     * 
     * @param mask the mask to use
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder mask(int mask);

    /**
     * Sets property propagation. If set to <code>true</code> any bundle manifest properties will be added
     * to the service properties of the component that has this dependency (if it registers as a service).
     * 
     * @param propagate <code>true</code> to propagate the bundle manifest properties
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder propagate(boolean propagate);
    
    /**
     * Sets property propagation. any bundle manifest properties will be added
     * to the service properties of the component that has this dependency (if it registers as a service).
     * 
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder propagate();
    
    /**
     * Sets an Object instance and a callback method used to propagate some properties to the provided service properties.
     * The method will be invoked on the specified object instance and must have one of the following signatures:
     * <ul><li>Dictionary callback(ServiceReference, Object service) 
     * <li>Dictionary callback(ServiceReference)
     * </ul>
     * @param instance the Object instance which is used to retrieve propagated service properties 
     * @param method the method to invoke for retrieving the properties to be propagated to the service properties.
     * @return this service dependency. builder
     */
    public BundleDependencyBuilder propagate(Object instance, String method);
    
    /**
     * Sets an Object instance and a callback method used to propagate some properties to the provided service properties.
     * The method will be invoked on the specified object instance and must have one of the following signatures:
     * <ul><li>Dictionary callback(ServiceReference, Object service) 
     * <li>Dictionary callback(ServiceReference)
     * </ul>
     * @param instance the Object instance which is used to retrieve propagated service properties 
     * @return this service dependency. builder
     */
    public BundleDependencyBuilder propagate(Supplier<Dictionary<?, ?>> instance);

    /**
     * Sets some <code>callback</code> methods to invoke on the component instance(s). When a bundle state matches the bundle 
     * filter, then the bundle is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. When you specify three
     * callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for the "remove" callback.
     * 
     * @param callbacks a list of callbacks (1 param: "add", 2 params: "add"/remove", 3 params: "add"/"change"/"remove" callbacks).
     * @return this builder
     */
    BundleDependencyBuilder cb(String ... callbacks);
    
    /**
     * Sets some <code>callback instance</code> methods to invoke on a given Object instance. When a bundle state matches the bundle 
     * filter, then the bundle is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. 
     * When you specify three callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for 
     * the "remove" callback.
     * 
     * @param callbackInstance the Object instance where the callbacks are invoked on
     * @param callbacks a list of callbacks (1 param: "add", 2 params: "add/remove", 3 params: "add/change/remove" callbacks).
     * @return this builder
     */
    BundleDependencyBuilder cb(Object callbackInstance, String ... callbacks);

    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is added.
     * The method reference must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeBundle<T> add);
    
    /**
     * Sets some <code>callback</code> method references which are invoked when a bundle is added, or removed.
     * The method references must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> remove);
    
    /**
     * Sets some <code>callback</code> method references which are invoked when a bundle is added, changed or removed.
     * The method references must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> change, CbTypeBundle<T> remove);
    
    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is added.
     * The method reference must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeComponentBundle<T> add); 
    
    /**
     * Sets some <code>callback</code> method references which are invoked when a bundle is added, or removed.
     * The method references must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> remove); 
    
    /**
     * Sets some <code>callback</code> method references which are invoked when a bundle is added, changed or removed.
     * The method references must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> change, CbTypeComponentBundle<T> remove); 
 
    /**
     * Sets a <code>callback instance</code> method reference which is invoked when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbBundle add);
    
    /**
     * Sets some <code>callback instance</code> method references which are invoked when a bundle is added or removed. 
     * The method references must point to an Object instance method, and take as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbBundle add, CbBundle remove);
    
    /**
     * Sets some <code>callback instance</code> method references which are invoked when a bundle is added, changed or removed.
     * The method references must point to an Object instance method, and take as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbBundle add, CbBundle change, CbBundle remove);

    /**
     * Sets a <code>callback instance</code> method reference which is invoked when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as arguments a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbComponentBundle add);
    
    /**
     * Sets some <code>callback instance</code> method references which are invoked when a bundle is added or removed. 
     * The method references must point to an Object instance method, and take as argument a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbComponentBundle add, CbComponentBundle remove);
    
    /**
     * Sets some <code>callback instance</code> method references which are invoked when a bundle is added, changed or removed.
     * The method references must point to an Object instance method, and take as argument a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder cbi(CbComponentBundle add, CbComponentBundle change, CbComponentBundle remove);    
}
