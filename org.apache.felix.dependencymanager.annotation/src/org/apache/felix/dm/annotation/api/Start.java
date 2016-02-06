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
 * Annotates a method which will be invoked when the component is started.
 * The annotated method is invoked juste before registering the service into the OSGi registry 
 * (if the service provides an interface). Notice that the start method may optionally return 
 * a Map which will be propagated to the provided service properties.<p>
 * Service activation/deactivation can be programatically controlled using {@link LifecycleController}.
 *      
 * <h3>Usage Examples</h3>
 * <blockquote>
 * 
 * <pre>
 * &#64;Component(properties={&#64;Property(name="foo", value="bar")})
 * class X implements Z {
 *     &#64;ServiceDependency
 *     OtherService m_dependency;
 *   
 *     &#64;Start
 *     Map start() {
 *         // Our Z Service is ready (all required dependencies have been satisfied), and is about to be 
 *         // registered into the OSGi registry. We return here an optional Map containing some extra-properties
 *         // which will be appended to the properties supplied in the Component annotation.
 *         return new HashMap() {{
 *            put("foo2", "bar2");
 *            put(Constants.SERVICE_RANKING, Integer.valueOf(10));
 *         }};
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Start
{
}
