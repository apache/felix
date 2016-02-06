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

/**
 * Annotates a class as a Resource adapter service. Resource adapters are things that 
 * adapt a resource instead of a service, and provide an adapter service on top of this resource.
 * Resources are an abstraction that is introduced by the dependency manager, represented as a URL. 
 * They can be implemented to serve resources embedded in bundles, somewhere on a file system or in 
 * an http content repository server, or database.<p>
 * The adapter will be applied to any resource that matches the specified filter condition, which can
 * match some part of the resource URL (with "path", "protocol", "port", or "host" filters). 
 * For each matching resource an adapter will be created based on the adapter implementation class.
 * The adapter will be registered with the specified interface and with any extra service properties 
 * you supply here. Moreover, the following service properties will be propagated from the resource URL:
 * 
 * <ul><li> "host": this property exposes the host part of the resource URL
 * <li>"path": the resource URL path
 * <li>"protocol": the resource URL protocol
 * <li>"port": the resource URL port
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * Here, the "VideoPlayer" service provides a video service on top of any movie resources, with service
 * properties "host"/"port"/"protocol"/"path" extracted from the resource URL:
 * <blockquote>
 * <pre>
 * 
 * &#64;ResourceAdapterService(filter = "(&#38;(path=/videos/*.mkv)(host=localhost))", propagate = true)
 * public class VideoPlayerImpl implements VideoPlayer {
 *     // Injected by reflection
 *     URL resource;
 *     
 *     void play() {} // play video referenced by this.resource     
 *     void stop() {} // stop playing the video
 *     void transcode() {} // ...
 * }
 * </pre>
 * </blockquote>
 * 
 * And here is an example of a VideoProvider, which provides some videos using a web URL.
 * Notice that Resource providers need to depend on the DependencyManager API:
 * 
 * <blockquote>
 * <pre>
 * import java.net.MalformedURLException;
 * import java.net.URL;
 * import java.util.HashMap;
 * import java.util.Map;
 * 
 * import org.apache.felix.dm.ResourceHandler;
 * import org.apache.felix.dm.ResourceUtil;
 * import org.apache.felix.dm.annotation.api.Component;
 * import org.apache.felix.dm.annotation.api.Init;
 * import org.apache.felix.dm.annotation.api.ServiceDependency;
 * import org.osgi.framework.BundleContext;
 * import org.osgi.framework.Filter;
 * import org.osgi.framework.InvalidSyntaxException;
 * 
 * &#64;Component
 * public class VideoProvider
 * {
 *     // Injected by reflection
 *     private volatile BundleContext context;
 *     // List of known resource handlers
 *     private Map&#60;ResourceHandler, Filter&#62; m_handlers = new HashMap&#60;ResourceHandler, Filter&#62;();
 *     // List of known video resources
 *     private URL[] m_videos;
 * 
 *     &#64;Init
 *     void init() throws MalformedURLException
 *     {
 *        m_videos = new URL[] {
 *                new URL("http://localhost:8080/videos/video1.mkv"),
 *                new URL("http://localhost:8080/videos/video2.mkv"),
 *         };
 *     }
 * 
 *     // Track resource handlers
 *     &#64;ServiceDependency(required = false)
 *     public void add(Map&#60;String, String&#62; serviceProperties, ResourceHandler handler) throws InvalidSyntaxException
 *     {
 *         String filterString = serviceProperties.get("filter");
 *         filterString = (filterString != null) ? filterString : "(path=*)";
 *         Filter filter = context.createFilter(filterString);
 *         synchronized (this)
 *         {
 *             m_handlers.put(handler, filter);
 *         }
 *         for (URL video : m_videos)
 *         {
 *             if (filter.match(ResourceUtil.createProperties(video)))
 *             {
 *                 handler.added(video);
 *             }
 *         }
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ResourceAdapterService
{
    /**
     * The interface(s) to use when registering adapters
     * @return the provided interfaces
     */
    Class<?>[] provides() default {};

    /**
     * Additional properties to use with the adapter service registration
     * @return the properties
     */
    Property[] properties() default {};

   /**
     * The filter condition to use with the resource.
     * @return the filter
     */
    String filter();

    /**
     * <code>true</code> if properties from the resource should be propagated to the service properties.
     * @return the propagate flag
     */
    boolean propagate() default false;
    
    /**
     * The callback method to be invoked when the Resource has changed.
     * @return the changed callback
     */
    String changed() default "";

    /**
     * Sets the static method used to create the AdapterService implementation instance.
     * @return the factory method
     */
    String factoryMethod() default "";
}
