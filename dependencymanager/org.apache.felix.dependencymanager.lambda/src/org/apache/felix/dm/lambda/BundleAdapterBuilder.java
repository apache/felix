package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbComponentBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeBundle;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentBundle;

/**
 * Builds a Dependency Manager bundle adapter. The adapter created by this builder will be applied to any bundle that matches the specified 
 * bundle state mask and filter condition. For each matching bundle an adapter service will be created based on the adapter implementation class. 
 * The adapter will be registered with the specified interface and existing properties from the original bundle plus any extra properties 
 * you supply here. The bundle is injected by reflection in adapter class fields having a Bundle type, or using a callback method that you can 
 * specify.
 * 
 * <p> Example which creates a BundleAdapter service for each started bundle (the bundle is added by reflection on
 * a class field that has a "Bundle" type):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *       bundleAdapter(adapt -> adapt
 *           .impl(BundleAdapterImpl.class)
 *           .provides(BundleAdapter.class)
 *           .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE));
 *    }
 * }
 * } </pre>
 * 
 * Example that creates a BundleAdapter service for each started bundle (the bundle is added using a method reference):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *       bundleAdapter(adapt -> adapt
 *           .impl(BundleAdapterImpl.class)
 *           .provides(BundleAdapter.class)
 *           .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
 *           .cb(BundleAdapterImpl::setBundle));
 *    }
 * }
 * }</pre>
 *
 * Example that creates a BundleAdapter service for each started bundle (the bundle is added using a method name):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void activate() throws Exception { 
 *       bundleAdapter(adapt -> adapt
 *           .impl(BundleAdapterImpl.class)
 *           .provides(BundleAdapter.class)
 *           .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
 *           .cb("setBundle"));
 *    }
 * }
 * }</pre>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface BundleAdapterBuilder extends ComponentBuilder<BundleAdapterBuilder> {
    /**
     * Sets the bundle state mask to depend on. The OSGi BundleTracker explains this mask in more detail, but
     * it is basically a mask with flags for each potential state a bundle can be in.
     * 
     * @param mask the mask to use
     * @return this builder
     */
    BundleAdapterBuilder mask(int mask);
    
    /**
     * Sets the filter condition to depend on. Filters are matched against the full manifest of a bundle.
     * 
     * @param filter the filter condition
     * @return this builder
     */
    BundleAdapterBuilder filter(String filter);

    /**
     * Sets property propagation. If set to <code>true</code> any bundle manifest properties will be added
     * to the service properties of the component that has this dependency (if it registers as a service).
     * 
     * @param propagate <code>true</code> to propagate the bundle manifest properties
     * @return this builder
     */
    BundleAdapterBuilder propagate(boolean propagate);
    
    /**
     * Enables property propagation. Any bundle manifest properties will be added
     * to the service properties of the component that has this dependency (if it registers as a service).
     * 
     * @return this builder
     */
    BundleAdapterBuilder propagate();
    
    /**
     * Sets some <code>callbacks</code> invoked on the component implementation instances. When a bundle state matches the bundle 
     * adapter filter, then the bundle is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. When you specify three
     * callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for the "remove" callback.
     * 
     * @param callbacks a list of callbacks (1 param : "add", 2 params : "add"/remove", 3 params : "add"/"change"/"remove").
     * @return this builder
     */
    BundleAdapterBuilder cb(String ... callbacks);
    
    /**
     * Sets some <code>callback instance</code> methods invoked on a given Object instance. When a bundle state matches the bundle 
     * adapter filter, then the bundle is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. 
     * When you specify three callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for 
     * the "remove" callback.
     * 
     * @param callbackInstance the Object instance where the callbacks are invoked on
     * @param callbacks a list of callbacks (1 param : "add", 2 params : "add"/remove", 3 params : "add"/"change"/"remove").
     * @return this builder
     */
    BundleAdapterBuilder cbi(Object callbackInstance, String ... callbacks);

    /**
     * Sets a <code>callback</code> invoked on a component implementation instance when a bundle is added.
     * The method reference must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeBundle<T> add);
    
    /**
     * Sets some <code>callbacks</code> invoked on a component implementation instance when a bundle is added/removed.
     * The method references must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> remove);
    
    /**
     * Sets some <code>callbacks</code> invoked on a component implementation instance when a bundle is added, changed or removed.
     * The method references must point to a Component implementation class method, and take as argument a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeBundle<T> add, CbTypeBundle<T> change, CbTypeBundle<T> remove);
    
    /**
     * Sets a <code>callback</code> invoked on a component implementation instance when a bundle is added.
     * The method reference must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add);    
    
    /**
     * Sets some <code>callbacks</code> invoked on a component implementation instance when a bundle is added, or removed.
     * The method references must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> remove);   
    
    /**
     * Sets some <code>callbacks</code> invoked on a component implementation instance when a bundle is added, changed or removed.
     * The method references must point to a Component implementation class method, and take as argument a Component and a Bundle.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder cb(CbTypeComponentBundle<T> add, CbTypeComponentBundle<T> change, CbTypeComponentBundle<T> remove);         

    /**
     * Sets a <code>callback instance</code> invoked on a given Object instance when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbBundle add);
    
    /**
     * Sets some <code>callback instance</code> invoked on a given Object instance when a bundle is added or removed. 
     * The method references must point to an Object instance method, and take as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbBundle add, CbBundle remove);
    
    /**
     * Sets some <code>callback instance</code> invoked on a given Object instance when a bundle is added, changed or removed.
     * The method references must point to an Object instance method, and take as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbBundle add, CbBundle change, CbBundle remove);

    /**
     * Sets a <code>callback instance</code> invoked on a given Object instance when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as arguments a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbComponentBundle add);
    
    /**
     * Sets some <code>callback instance</code> invoked on a given Object instance when a bundle is added or removed. 
     * The method references must point to an Object instance method, and take as argument a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbComponentBundle add, CbComponentBundle remove);
    
    /**
     * Sets some <code>callback instance</code> invoked on a given Object instance when a bundle is added, changed or removed.
     * The method references must point to an Object instance method, and take as argument a Component and a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @param change the method reference invoked when a bundle has changed.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder cbi(CbComponentBundle add, CbComponentBundle change, CbComponentBundle remove);
}
