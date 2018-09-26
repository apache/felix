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

/**
 * Interface used to configure the various parameters needed when defining 
 * a Dependency Manager bundle adapter component.
 * 
 * Bundle Adapters, like {@link AdapterComponent}, are used to "extend" 
 * existing bundles, and can publish an adapter services based on the existing bundle. 
 * An example would be implementing a video player which adapters a resource bundle having 
 * some specific headers. 
 * <p>When you create a bundle adapter component, it will be applied 
 * to any bundle that matches the specified bundle state mask as well as the specified ldap filter
 * used to match the bundle manifest headers. The bundle adapter will be registered 
 * with the specified bundle manifest headers as service properties, plus any extra 
 * properties you suppl. If you declare a bundle field in your bundle adapter class, 
 * it will be injected it will be injected with the original bundle.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a VideoPlayer adapter component which plays a video found from 
 * a bundle having a Video-Path manifest header.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         Component bundleComponent = createBundleComponent()
 *             .setFilter(Bundle.ACTIVE, "(Video-Path=*)")
 *             .setInterface(VideoPlayer.class.getName(), null)
 *             .setImplementation(VideoPlayerImpl.class);
 *         dm.add(bundleComponent);
 *     }
 * }
 * 
 * public interface VideoPlayer {
 *     void play();
 * }
 * 
 * public class VideoPlayerImpl implements VideoPlayer {
 *     volatile Bundle bundle; // injected
 *     String path;
 *     
 *     void start() {
 *        path = bundle.getHeaders().get("Video-Path");
 *     }
 *     
 *     void play() {
 *         ...
 *     }
 * }
 * } </pre></blockquote>
 * 
 * <p> When you use callbacks to get injected with the bundle, the "add", "change", "remove" callbacks
 * support the following method signatures:
 * 
 * <pre>{@code
 * (Bundle)
 * (Object)
 * (COmponent, Bundle)
 * }</pre>
 * 
 * @see DependencyManager#createBundleComponent()
 */
public interface BundleComponent extends Component<BundleComponent> {
    
    /**
     * Sets the bundle state mask and bundle manifest headers filter.
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @return this BundleComponent
     */
	BundleComponent setBundleFilter(int bundleStateMask, String bundleFilter);
            
    /**
     * Sets the callbacks to invoke when injecting the bundle into the adapter component.
     * 
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @return this BundleComponent
     */
	BundleComponent setBundleCallbacks(String add, String change, String remove);
    
    /**
     * Sets the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * 
     * @param callbackInstance the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @return this BundleComponent
     */
	BundleComponent setBundleCallbackInstance(Object callbackInstance);

    /**
     * Sets if the bundle manifest headers should be propagated to the bundle component adapter service consumer (true by default)
     * @param propagate true if the bundle manifest headers should be propagated to the adapter service consumers
     * @return this BundleComponent
     */
	BundleComponent setPropagate(boolean propagate);
    
}
