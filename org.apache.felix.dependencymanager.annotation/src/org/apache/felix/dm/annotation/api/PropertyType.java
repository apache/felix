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
 * When defining component service properties, one way to achieve this is to apply the {@link Property} annotation on your component class name.
 * Now, you can also define your own component property type interfaces and apply them directly on your components (instead of the 
 * {@link Property} annotation). The PropertyType annotation is closely similar to standard OSGi r7 declarative service @ComponentPropertyType 
 * (which is also supported when dependency manager annotation API).
 * 
 * <h3>Usage Examples</h3>
 * 
 * Let’s assume your write an OSGi r7 jax rs servlet context which needs the two following service properties: 
 * 
 * <p><ul>
 * <li> osgi.http.whiteboard.context.name
 * <li> osgi.http.whiteboard.context.path
 * </ul>
 * 
 * <p> Then you can first define your own annotation (but you could also reuse the default annotations provided by the jaxrs whiteboard r7 api):
 * (notice that in the annotation, you can define default service property values):
 * 
 * <blockquote>
 * <pre>
 * &#64;PropertyType
 * &#64;interface ServletContext {
 *     String osgi_http_whiteboard_context_name() default AppServletContext.NAME;
 *     String osgi_http_whiteboard_context_path();
 * }
 * </pre>
 * </blockquote>
 * 
 * In the above, the underscore is mapped to ".".
 * Then you can apply the above annotation on top of your component like this:
 * 
 * <blockquote>
 * <pre>
 * &#64;Component
 * &#64;ServletContext(osgi_http_whiteboard_context_path="/game")
 * public class AppServletContext extends ServletContextHelper {
 * }
 * </pre>
 * </blockquote>
 * 
 * You can also use configuration admin service in order to override the default service properties:
 * 
 * <blockquote>
 * <pre>
 * &#64;Component
 * &#64;ServletContext(osgi_http_whiteboard_context_path="/game")
 * public class AppServletContext extends ServletContextHelper {
 *     &#64;ConfigurationDependency(propagate=true, pid="my.pid")
 *     void updated(ServletContext cnf) {
 *        // if some properties are not present in the configuration, then the ones used in the annotation will be used.
 *        // The configuration admin properties, if defined, will override the default configurations defined in the annotations
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * You can also define multiple property type annotations, and possibly single valued annotation. In this case, you can use
 * the standard R7 PREFIX_ constants in order to specify the property prefix, and the property name will be derived from the
 * single valued annotation (using camel case convention):
 * 
 * <blockquote>
 * <pre>
 * &#64;PropertyType
 * &#64;interface ContextName { // will map to "osgi.http.whiteboard.context.name" property name
 *     String PREFIX="osgi.http.whiteboard.";
 *     String value();
 * }
 * 
 * &#64;PropertyType
 * &#64;interface ContextPath { // will map to "osgi.http.whiteboard.context.path" property name
 *     String PREFIX="osgi.http.whiteboard.";
 *     String value();
 * }
 * 
 * &#64;Component
 * &#64;ContextName(AppServletContext.NAME)
 * &#64;ContextPath("/game")
 * public class AppServletContext extends ServletContextHelper {
 * }
 * </pre>
 * </blockquote>
 * 
 * Same example as above, but also using configuration admin service in order to override default service properties: Here, as in OSGi r7 declarative service,
 * you can define a callback method which accepts as arguments all (or some of) the defined property types:
 * 
 * <blockquote>
 * <pre>
 * &#64;Component
 * &#64;ContextName(AppServletContext.NAME)
 * &#64;ContextPath("/game")
 * public class AppServletContext extends ServletContextHelper {
 *     &#64;ConfigurationDependency(propagate=true, pid="my.pid")
 *     void updated(ContextName ctxName, ContextPath ctxPath) {
 *        // if some properties are not present in the configuration, then the ones used in the annotation will be used.
 *        // The configuration admin properties, if defined, will override the default configurations defined in the annotations
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * The following is the same example as above, but this time the configuration callback can also define a Dictionary in the first argument
 * (in case you want to also get the raw configuration dictionary:
 * 
 * <blockquote>
 * <pre>
 * &#64;Component
 * &#64;ContextName(AppServletContext.NAME)
 * &#64;ContextPath("/game")
 * public class AppServletContext extends ServletContextHelper {
 *     &#64;ConfigurationDependency(propagate=true, pid="my.pid")
 *     void updated(Dictionary&lt;String, Object&gt; rawConfig, ContextName ctxName) {
 *        // if some properties are not present in the configuration, then the ones used in the annotation will be used.
 *        // The configuration admin properties, if defined, will override the default configurations defined in the annotations
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface PropertyType {
	// meta-annotation
}
