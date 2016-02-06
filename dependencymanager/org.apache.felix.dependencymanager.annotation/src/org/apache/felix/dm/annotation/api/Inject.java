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
 * Inject classes in a component instance field.
 * The following injections are currently performed, depending on the type of the
 * field this annotation is applied on:
 * <ul>
 * <li>BundleContext: the bundle context of the bundle
 * <li>DependencyManager: the dependency manager instance
 * <li>Component: the component instance of the dependency manager
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * <blockquote>
 * 
 * <pre>
 * &#64;Component
 * class X implements Z {
 *     &#64;Inject
 *     BundleContext bundleContext;
 *   
 *     &#64;Inject
 *     Component component;
 *     
 *     &#64;Inject
 *     DependencyManager manager;
 *   
 *     OtherService otherService;
 *   
 *     &#64;Init
 *     void init() {
 *         System.out.println("Bundle Context: " + bundleContext);
 *         System.out.println("Manager: " + manager);
 *         
 *         // Use DM API for defining an extra service dependency
 *         componnent.add(manager.createServiceDependency()
 *                               .setService(OtherService.class)
 *                               .setRequired(true)
 *                               .setInstanceBound(true));
 *     }
 *     
 *     &#64;Start
 *     void start() {
 *         System.out.println("OtherService: " + otherService);
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Inject
{
}
