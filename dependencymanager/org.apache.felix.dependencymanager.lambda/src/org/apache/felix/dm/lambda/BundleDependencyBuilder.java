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
import java.util.function.Function;

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.lambda.callbacks.CbBundle;
import org.apache.felix.dm.lambda.callbacks.CbBundleComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundle;
import org.apache.felix.dm.lambda.callbacks.InstanceCbBundleComponent;
import org.osgi.framework.Bundle;

/**
 * Builds a Dependency Manager Bundle Dependency. 
 * 
 * <p> Example of a Pojo Component which tracks a started bundle having a given bundle symbolic name:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         String BSN = "org.apache.felix.dependencymanager";
 *         component(comp -> comp
 *             .impl(Pojo.class)
 *             .withBundle(b -> b.mask(Bundle.ACTIVE).filter("(Bundle-SymbolicName=" + BSN + ")").add(Pojo::add).remove(Pojo::remove)));
 *    }
 * }
 * } </pre>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface BundleDependencyBuilder extends DependencyBuilder<BundleDependency> {
    /**
     * Enables auto configuration for this dependency. This means the component implementation class fields will be
     * injected with this bundle dependency automatically.
     * 
     * @param autoConfig <code>true</code> to enable auto configuration
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder autoConfig(boolean autoConfig);

    /**
     * Enables auto configuration for this dependency. This means the component implementation class fields will be
     * injected with this bundle dependency automatically.
     * 
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder autoConfig();

    /**
     * Sets the dependency to be required.
     * @param required <code>true</code> if this bundle dependency is required.
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
     * Sets the dependency to be optional.
     * 
     * @return the bundle dependency builder
     */
    public BundleDependencyBuilder optional();

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
     * to the service properties of the component that declares this dependency (if it provides a service).
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
     * 
     * <p><ul><li>Dictionary callback(Bundle bundle)</ul>
     * 
     * @param instance the Object instance which is used to retrieve propagated service properties 
     * @param method the method to invoke for retrieving the properties to be propagated to the service properties.
     * @return this service dependency. builder
     */
    public BundleDependencyBuilder propagate(Object instance, String method);
    
    /**
     * Sets a reference to a method on an Object instance used to propagate some bundle properties to the provided service properties.
     * 
     * @param propagate a function which accepts a Bundle argument and which returns some properties that will be
     * propagated to the provided component service properties. 
     * @return this service dependency. builder
     */
    public BundleDependencyBuilder propagate(Function<Bundle, Dictionary<?, ?>> propagate);

    /**
     * Sets a "add" <code>callback</code> method to invoke on the component implementation instance(s).
     * The callback is invoked when the bundle is added, and the following signatures are supported:
     * 
     * <p><ol>
     * <li>method(Bundle)</li>
     * <li>method(Component, Bundle)</li>
     * </ol>
     * 
     * @param callback the add callback
     * @return this builder
     */
    BundleDependencyBuilder add(String callback);
    
    /**
     * Sets a "change" <code>callback</code> method to invoke on the component implementation instance(s). 
     * The callback is invoked when the bundle state has changed, and the following signatures are supported:
     * 
     * <p><ol>
     * <li>method(Bundle)</li>
     * <li>method(Component, Bundle)</li>
     * </ol>
     * 
     * @param callback the change callback
     * @return this builder
     */
    BundleDependencyBuilder change(String callback);
    
    /**
     * Sets a "remove" <code>callback</code> method to invoke on the component implementation instance(s). 
     * The callback is invoked when the bundle is removed, and the following signatures are supported:
     * <p><ol>
     * <li>method(Bundle)</li>
     * <li>method(Component, Bundle)</li>
     * </ol>
     * 
     * @param callback the remove callback
     * @return this builder
     */
    BundleDependencyBuilder remove(String callback);
    
    /**
     * Specifies a callback instance used to invoke the reflection based callbacks on it.
     * @param callbackInstance the instance to invoke the reflection based callbacks on
     * @return this builder
     * @see #add(String)
     * @see #change(String)
     * @see #remove(String)
     */
    BundleDependencyBuilder callbackInstance(Object callbackInstance);

    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is added.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleDependencyBuilder add(CbBundle<T> add);
        
    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is changed.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    <T> BundleDependencyBuilder change(CbBundle<T> change);
    
    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is removed.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder remove(CbBundle<T> remove);

    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is added.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    <T> BundleDependencyBuilder add(CbBundleComponent<T> add); 
    
    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is changed.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param change the method reference invoked when a bundle has changed.
     * @return this builder
     */
    <T> BundleDependencyBuilder change(CbBundleComponent<T> change); 
 
    /**
     * Sets a <code>callback</code> method reference which is invoked when a bundle is removed.
     * The method reference must point to a Component implementation class method, and takes as argument a Bundle and a Component.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked on.
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    <T> BundleDependencyBuilder remove(CbBundleComponent<T> remove); 
    
    /**
     * Sets a method reference on an Object instance which is invoked when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle parameter.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleDependencyBuilder add(InstanceCbBundle add);
    
    /**
     * Sets a method reference on an Object instance which is invoked when a bundle is changed. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle parameter.
     * 
     * @param change the method reference invoked when a bundle is changed.
     * @return this builder
     */
    BundleDependencyBuilder change(InstanceCbBundle change);
    
    /**
     * Sets a method reference on an Object instance which is invoked when a bundle is removed. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle parameter.
     * 
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder remove(InstanceCbBundle remove);

    /**
     * Sets a <code>callback instance</code> method reference which is invoked when a bundle is added. 
     * The method reference must point to an Object instance method, and takes as arguments a Bundle and a Component.
     * 
     * @param add the method reference invoked when a bundle is added.
     * @return this builder
     */
    BundleDependencyBuilder add(InstanceCbBundleComponent add);
    
    /**
     * Sets a <code>callback instance</code> method reference which is invoked when a bundle is changed. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle and a Component.
     * 
     * @param change the method reference invoked when a bundle is changed.
     * @return this builder
     */
    BundleDependencyBuilder change(InstanceCbBundleComponent change);
    
    /**
     * Sets a <code>callback instance</code> method reference which is invoked when a bundle is removed. 
     * The method reference must point to an Object instance method, and takes as argument a Bundle and a Component.
     * 
     * @param remove the method reference invoked when a bundle is removed.
     * @return this builder
     */
    BundleDependencyBuilder remove(InstanceCbBundleComponent remove);    
}
