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
package org.apache.felix.ipojo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation declares that the component instances will provide a service.
 *
 * <pre>
 *     {@linkplain org.apache.felix.ipojo.annotations.Component @Component}
 *     {@code @Provides}
 *     public class MyComponent implements Service {
 *         // ...
 *     }
 * </pre>
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface Provides {

    /**
     * Set the provided specifications.
     * It can be used to force the exposed service to use an implementation class
     * (not an interface) as specification.
     * Default : all implemented interfaces
     *
     * <pre>
     *     {@linkplain org.apache.felix.ipojo.annotations.Component @Component}
     *     {@code @Provides(specifications = AbsComponent.class)}
     *     public class MyComponent extends AbsComponent {
     *         // ...
     *     }
     * </pre>
     */
    Class[] specifications() default { };

    /**
     * Set the service object creation strategy.
     * Multiple values are possible: {@literal SINGLETON}, {@literal SERVICE},
     * {@literal METHOD}, {@literal INSTANCE} or the strategy fully qualified class name:
     * <ul>
     *     <li>{@literal SINGLETON}: Default strategy</li>
     *     <li>{@literal SERVICE}: OSGi Service Factory style, 1 POJO instance per consumer bundle</li>
     *     <li>{@literal METHOD}: Delegates the creation to the factory-method of the component, method will be called every time the service reference is get.</li>
     *     <li>{@literal INSTANCE}: Creates one service object per requiring instance</li>
     *     <li>Any other value is interpreted as the qualified name of a {@code CreationStrategy} implementation</li>
     * </ul>
     */
    String strategy() default "SINGLETON";

    /**
     * Allows adding static properties to the service.
     * Nested properties are static service properties, so <b>must</b> contain the name,
     * value and type as they are not attached to a field (cannot discover type through
     * introspection).
     * The array contains {@link StaticServiceProperty} elements.
     * Default : No service properties

     * <pre>
     *     {@linkplain org.apache.felix.ipojo.annotations.Component @Component}
     *     {@code @Provides}(
     *         properties = {
     *             {@code @StaticServiceProperty}(name = "size", type = "int", value = "5"),
     *             {@code @StaticServiceProperty}(name = "name", type = "java.lang.String", value = "OSGi")
     *         }
     *     )
     *     public class MyComponent implements Service {
     *         // ...
     *     }
     * </pre>

     */
    StaticServiceProperty[] properties() default {};
}
