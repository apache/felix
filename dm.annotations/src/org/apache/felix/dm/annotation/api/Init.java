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
 * Annotates a method which will be invoked when the Service is initializing.
 * All required dependencies are already injected before the annotated method is called, and 
 * optional dependencies on class fields are injected with NullObjects if the optional
 * dependencies are not currently available.<p>
 * 
 * If some dependencies are declared using a <b>named</b> &#64;{@link ServiceDependency} annotation, 
 * then the annotated method may optionally return a Map used to dynamically configure such 
 * dependencies (Please refer to &#64;{@link ServiceDependency#name()} attribute for more 
 * information about this feature).<p>
 * 
 * After the init method returns, the component is then invoked in the method annotated with
 * &#64;{@link Start}, in order to notify that the component is about to be registered into the OSGi 
 * registry (if this one provides a service). However, you can take control of when the service is registered,
 * using the &#64;{@link LifecycleController} annotation).
 * 
 * <h3>Usage Examples</h3>
 * Here, the "VideoPlayer" init method is called after the "log" dependency is injected.
 * <blockquote>
 * <pre>
 * 
 * &#64;Component
 * public class VideoPlayer {
 *     &#64;ServiceDependency
 *     LogService log;
 *     
 *     &#64;Init
 *     void init() {} // initialize our service (the "log" dependency is already injected).
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Init
{
}
