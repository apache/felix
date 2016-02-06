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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.framework.Bundle;

/**
 * Annotates a bundle adapter service class. Bundle adapters are similar to {@link AdapterService},
 * but instead of adapting a service, they adapt a bundle with a certain set of states (STARTED|INSTALLED|...),
 * and provide a service on top of it. <p>
 * The bundle adapter will be applied to any bundle that matches the specified bundle state mask and 
 * filter conditions, which may match some of the bundle OSGi manifest headers. For each matching 
 * bundle an adapter will be created based on the adapter implementation class. The adapter will be 
 * registered with the specified interface and with service properties found from the original bundle
 * OSGi manifest headers plus any extra properties you supply here.
 * If you declare the original bundle as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> In the following example, a "VideoPlayer" Service is registered into the OSGi registry each time
 * an active bundle containing a "Video-Path" manifest header is detected:
 * 
 * <blockquote>
 * <pre>
 * &#64;BundleAdapterService(filter = "(Video-Path=*)", stateMask = Bundle.ACTIVE, propagate=true)
 * public class VideoPlayerImpl implements VideoPlayer {
 *     Bundle bundle; // Injected by reflection
 *     
 *     void play() {
 *         URL mpegFile = bundle.getEntry(bundle.getHeaders().get("Video-Path"));
 *         // play the video provided by the bundle ...
 *     }
 *     
 *     void stop() {}
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public @Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface BundleAdapterService
{
    /**
     * The interface(s) to use when registering adapters. By default, the interface(s) directly implemented
     * by the annotated class is (are) used.
     * @return the interface(s) to use when registering adapters
     */
    Class<?>[] provides() default {};
    
    /**
     * Additional properties to use with the service registration
     * @return the bundle adapter properties
     */
    Property[] properties() default {};
    
   /**
     * The filter used to match a given bundle.
     * @return the bundle adapter filter
     */
    String filter();
    
    /**
     * the bundle state mask to apply
     * @return the bundle state mask to apply
     */
    int stateMask() default Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;
    
    /**
     * Specifies if manifest headers from the bundle should be propagated to the service properties.
     * @return the propagation flag
     */
    boolean propagate() default true;
    
    /**
     * Sets the static method used to create the BundleAdapterService implementation instance.
     * @return the factory method
     */
    String factoryMethod() default "";
}
