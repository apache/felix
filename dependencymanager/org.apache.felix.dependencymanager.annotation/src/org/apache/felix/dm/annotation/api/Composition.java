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
 * Annotates a method returning the list of objects which are part of a Component implementation.
 * When implementing complex Components, you often need to use more than one object instances. 
 * Moreover, several of these instances might want to have dependencies injected, as well as lifecycle
 * callbacks invoked, like the methods annotated with {@link Init}, {@link Start}, {@link Stop}, 
 * {@link Destroy} annotations. In such cases you can tell the dependency manager which instances to 
 * consider, by annotating a method in your Component, returning a list of objects which are part 
 * of the implementation.
 * <p>
 * This annotation may be applied on a method which is part of class annotated with either a {@link Component},
 * {@link AspectService}, {@link AdapterService}, {@link FactoryConfigurationAdapterService} or 
 * {@link ResourceAdapterService} annotation.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here, the "MyComponent" component is composed of the Helper class, which is also injected with 
 * service dependencies. The lifecycle callbacks are also invoked in the Helper (if the Helper defines 
 * them):
 * <blockquote>
 * <pre>
 *
 * class Helper {
 *     LogService logService; // Injected
 *     void start() {} // lifecycle callback
 *     void bind(OtherService otherService) {} // injected
 * }
 * 
 * &#64;Component
 * class MyComponent {
 *     // Helper which will also be injected with our service dependencies
 *     private Helper helper = new Helper();
 *      
 *     &#64;Composition
 *     Object[] getComposition() {
 *         return new Object[] { this, helper }; 
 *     }
 *
 *     &#64;ServiceDependency
 *     private LogService logService; // Helper.logService will be also be injected, if defined.
 *     
 *     &#64;Start
 *     void start() {} // the Helper.start() method will also be called, if defined
 *     
 *     &#64;ServiceDependency
 *     void bind(OtherService otherService) {} // the Helper.bind() method will also be called, if defined     
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Composition
{
}
