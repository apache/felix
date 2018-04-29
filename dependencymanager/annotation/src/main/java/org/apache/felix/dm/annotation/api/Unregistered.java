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
 * This annotation can be used to be notified when a component is unregistered from the registry. 
 * At this point, the component has been unregistered from the OSGI registry (if it provides some services).
 * The method must not take any parameters.
 * 
 * <p>
 * <h3>Usage Examples</h3>
 * <blockquote>
 * 
 * <pre>
 * &#64;Component
 * class X implements Z {     
 *     &#64;Stop
 *     void stop(ServiceRegistration sr) {
 *        // Our service must stop because it is about to be unregistered from the registry.
 *     }
 *     
 *     &#64;Unregistered
 *     void unregistered() {
 *        // At this point, our service has been unregistered from the OSGi registry
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Unregistered
{
}
