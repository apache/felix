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
 * a Dependency Manager resource adapter component.
 * 
 * The resource adapter will be applied to any resource that
 * matches the specified filter condition. For each matching resource
 * an adapter will be created based on the adapter implementation class.
 * The adapter will be registered with the specified interface and existing properties
 * from the original resource plus any extra properties you supply here.
 * It will also inherit all dependencies, and if you declare the original
 * service as a member it will be injected.
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
 *          Component resourceComponent = dm.createResourceComponent()
 *             .setResourceFilter("(path=/videos/*.mkv)")
 *             .setInterface(VideoPlayer.class, null)
 *             .setImplementation(VideoPlayerImpl.class);
 *         dm.add(resourceComponent);
 *     }
 * }
 * 
 * public interface VideoPlayer {
 *     void play();
 * }
 * 
 * public class VideoPlayerImpl implements VideoPlayer {
 *     volatile URL resource; // injected 
 *     
 *     void play() {
 *         ...
 *     }
 * }
 * } </pre></blockquote>
 * 
 * <p> When you use callbacks to get injected with the resource, the "add", "change" callbacks
 * support the following method signatures:
 * 
 * <p>
 * <pre>{@code
 * (Component, URL, Dictionary)
 * (Component, URL)
 * (Component) 
 * (URL, Dictionary)
 * (URL)
 * (Object)
 * }</pre>
 * 
 * @see DependencyManager#createBundleComponent()
 */
public interface ResourceComponent extends Component<ResourceComponent> {
    
    /**
     * Sets the resource filter used to match a given resource URL.
     * 
     * @param filter the filter condition to use with the resource
     * @return this ResourceComponent
     */
	ResourceComponent setResourceFilter(String filter);
            
    /**
     * Sets if properties from the resource should be propagated to the resource adapter service. true by default.
     * @param propagate true if if properties from the resource should be propagated to the resource adapter service.
     * true by default.
     * @return this ResourceComponent
     */
	ResourceComponent setPropagate(boolean propagate);
	
    /**
     * Sets the propagate callback to invoke in order to propagate the resource properties to the adapter service.
     * @param propagateCbInstance the object to invoke the propagate callback method on
     * @param propagateCbMethod the method name to invoke in order to propagate the resource properties to the adapter service.
     * @return this ResourceComponent
     */
	ResourceComponent setPropagate(Object propagateCbInstance, String propagateCbMethod);

	/**
	 * Sets the callbacks to invoke when injecting the resource into the adapter component.
	 * @param add the method to invoke when injected the resource into the adapter component
	 * @param change the method to invoke when the resource properties have changed
     * @return this ResourceComponent
	 */
	ResourceComponent setBundleCallbacks(String add, String change);

    /**
     * Sets the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the resource adapter itself)
     * 
     * @param callbackInstance the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the resource adapter itself)
     * @return this ResourceComponent
     */
	ResourceComponent setBundleCallbackInstance(Object callbackInstance);

    
}
