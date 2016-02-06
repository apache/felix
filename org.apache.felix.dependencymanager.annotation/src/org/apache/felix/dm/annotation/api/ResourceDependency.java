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
 * Annotates a method of field as a Resource Dependency. A resource dependency allows you to 
 * depend on a resource. Resources are an abstraction that is introduced by the dependency manager, represented as a URL. 
 * They can be implemented to serve resources embedded in bundles, somewhere on a file system or in 
 * an http content repository server, or database. <p> A resource is a URL and you can use a filter condition based on 
 * protocol, host, port, and path. 
 * 
 * <h3>Usage Examples</h3>
 * Here, the "VideoPlayer" component plays any provided MKV video resources
 * <blockquote>
 * <pre>
 * 
 * &#64;Component
 * public class VideoPlayer {
 *     &#64;ResourceDependency(required=false, filter="(path=/videos/*.mkv)")
 *     void playResource(URL video) { ... }
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
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ResourceDependency
{
    /**
     * Returns the callback method to be invoked when the service is available. This attribute is only meaningful when 
     * the annotation is applied on a class field.
     * @return the add callback
     */
    String added() default "";

    /**
     * Returns the callback method to be invoked when the service properties have changed.
     * @return the change callback
     */
    String changed() default "";

    /**
     * Returns the callback method to invoke when the service is lost.
     * @return the remove callback
     */
    String removed() default "";

    /**
     * Returns whether the Service dependency is required or not.
     * @return the required flag
     */
    boolean required() default true;
    
    /**
     * Returns the Service dependency OSGi filter.
     * @return the filter
     */
    String filter() default "";

    /**
     * Specifies if the resource URL properties must be propagated. If set to true, then the URL properties 
     * ("protocol"/"host"/"port"/"path") will be propagated to the service properties of the component which 
     * is using this dependency. 
     * @return the propagate flag
     */
    boolean propagate() default false;
    
    /**
     * The name used when dynamically configuring this dependency from the init method.
     * Specifying this attribute allows to dynamically configure the dependency 
     * <code>filter</code> and <code>required</code> flag from the Service's init method.
     * All unnamed dependencies will be injected before the init() method; so from the init() method, you can
     * then pick up whatever information needed from already injected (unnamed) dependencies, and configure dynamically
     * your named dependencies, which will then be calculated once the init() method returns.
     * 
     * <p> See {@link Init} annotation for an example usage of a dependency dynamically configured from the init method.
     * @return the dependency name
     */
    String name() default "";   
}
