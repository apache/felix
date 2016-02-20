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

import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbBundleComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundle;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundleComponent;

/**
 * Builds a Dependency Manager bundle adapter. <p> The adapter created by this builder will be applied to any bundle that matches the specified 
 * bundle state mask and filter condition. For each matching bundle an adapter service will be created based on the adapter implementation class. 
 * The adapter will be registered with the specified interface and existing properties from the original bundle plus any extra properties 
 * you supply here. The bundle is injected by reflection in adapter class fields having a Bundle type, or using a callback method that you can 
 * specify.
 * 
 * You can specify reflection based (using method names), or java8 method references for callbacks.
 * 
 * <p> Example which creates a BundleAdapter service for each started bundle (the bundle is added by reflection on
 * a class field that has a "Bundle" type):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
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
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       bundleAdapter(adapt -> adapt
 *           .impl(BundleAdapterImpl.class)
 *           .provides(BundleAdapter.class)
 *           .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
 *           .add(BundleAdapterImpl::setBundle));
 *    }
 * }
 * }</pre>
 *
 * Example that creates a BundleAdapter service for each started bundle (the bundle is added using a method name):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       bundleAdapter(adapt -> adapt
 *           .impl(BundleAdapterImpl.class)
 *           .provides(BundleAdapter.class)
 *           .mask(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE)
 *           .add("setBundle"));
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
     * Sets a "add" callback name invoked on the component implementation instance(s).
     * The callback can be used as hooks whenever the dependency is added. When you specify a callback, 
     * the auto configuration feature is automatically turned off, because we're assuming you don't need it in this case.
     * 
     * @param callback the method to call when a bundle was added
     * 
     * The following method signature are supported:
     * <pre>{@code
     * callback(Bundle b)
     * callback(Component c, Bundle b)
     * }</pre>
     * 
     * @param callback the callback name
     * @return this builder.
     */
    BundleAdapterBuilder add(String callback);
    
    /**
     * Sets a "change" callback name invoked on the component implementation instance(s).
     * The callback can be used as hooks whenever the dependency is changed. When you specify a callback, 
     * the auto configuration feature is automatically turned off, because we're assuming you don't need it in this case.
     * 
     * @param callback the method to call when a bundle was changed
     * 
     * The following method signature are supported:
     * <pre>{@code
     * callback(Bundle b)
     * callback(Component c, Bundle b)
     * }</pre>
     * 
     * @param callback the callback name
     * @return this builder.
     */
     BundleAdapterBuilder change(String callback);
 
     /**
      * Sets a "remove" callback name invoked on the component implementation instance(s).
      * The callback can be used as hooks whenever the dependency is removed. When you specify a callback, 
      * the auto configuration feature is automatically turned off, because we're assuming you don't need it in this case.
      * 
      * @param callback the method to call when a bundle was removed
      * 
      * The following method signature are supported:
      * <pre>{@code
      * callback(Bundle b)
      * callback(Component c, Bundle b)
      * }</pre>
      * 
      * @param callback the callback name
      * @return this builder.
      */
     BundleAdapterBuilder remove(String callback);
     
     /**
      * Sets a callback instance to use when invoking reflection based callbacks.
      * 
      * @param callbackInstance the instance to call the reflection based callbacks on
      * @return this builder.
      * @see #add(String)
      * @see #change(String)
      * @see #remove(String)
      */
     BundleAdapterBuilder callbackInstance(Object callbackInstance);

    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is added
     * and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleAdapterBuilder add(CbBundle<T> add);
    
    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is changed
     * and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    <T> BundleAdapterBuilder change(CbBundle<T> change);
    
    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is removed
     * and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder remove(CbBundle<T> remove);
    
    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is added
     * and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleAdapterBuilder add(CbBundleComponent<T> add);    
    
    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is changed
     * and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    <T> BundleAdapterBuilder change(CbBundleComponent<T> change);    
    
    /**
     * Sets a reference to a callback method invoked on one of the component implementation classes.
     * The method reference must point to a Component implementation class method, it is called when the bundle is removed
     * and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleAdapterBuilder remove(CbBundleComponent<T> remove);    

    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle is added and takes as argument a Bundle.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleAdapterBuilder add(InstanceCbBundle add);
    
    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle has changed and takes as argument a Bundle.
     * 
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    BundleAdapterBuilder change(InstanceCbBundle change);
    
    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle is removed and takes as argument a Bundle.
     * 
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder remove(InstanceCbBundle remove);

    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle is added and takes as argument a Bundle and a Component.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleAdapterBuilder add(InstanceCbBundleComponent add);
    
    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle has changed and takes as argument a Bundle and a Component.
     * 
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    BundleAdapterBuilder change(InstanceCbBundleComponent change);
    
    /**
     * Sets a reference to a callback method invoked on a given Object instance.
     * The method reference is invoked when the bundle is removed and takes as argument a Bundle and a Component.
     * 
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleAdapterBuilder remove(InstanceCbBundleComponent remove);
}
