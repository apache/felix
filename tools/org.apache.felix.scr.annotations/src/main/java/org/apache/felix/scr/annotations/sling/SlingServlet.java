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
package org.apache.felix.scr.annotations.sling;

import java.lang.annotation.*;

/**
 * Marks servlet classes as SCR component, and allows to configure
 * Sling's resource resolver mapping.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface SlingServlet {

    /**
     * Whether to generate a default SCR component tag. If
     * set to false, a {@link org.apache.felix.scr.annotations.Component}
     * annotation can be added manually with defined whatever configuration
     * needed.
     */
    boolean generateComponent() default true;

    /**
     * Whether to generate a default SCR service tag with
     * "interface=javax.servlet.Servlet". If set to false, a
     * {@link org.apache.felix.scr.annotations.Service} annotation can be added
     * manually with defined whatever configuration needed.
     */
    boolean generateService() default true;

    /**
     * One or more paths under which the servlet will be registered in the
     * Sling Resource tree.
     * <p>
     * This attribute is converted to values for the
     * <code>sling.servlet.paths</code> property.
     * <p>
     * Note that to be used as a servlet for Sling either this attribute or
     * the {@link #resourceTypes()} attribute or both must be set.
     */
    String[] paths() default {};

    /**
     * One or more resource types which are handled by this servlet.
     * <p>
     * This attribute is converted to values for the
     * <code>sling.servlet.resourceTypes</code> property.
     * <p>
     * Note that to be used as a servlet for Sling either this attribute or
     * the {@link #paths()} attribute or both must be set.
     */
    String[] resourceTypes() default {};

    /**
     * One ore more request URL selectors supported by the servlet. The
     * selectors must be configured as they would be specified in the URL that
     * is as a list of dot-separated strings such as <em>print.a4</em>.
     * <p>
     * This attribute is converted to values for the
     * <code>sling.servlet.selectors</code> property and is ignored if the
     * {@link #resourceTypes()} attribute is not set.
     */
    String[] selectors() default {};

    /**
     * One or more request URL extensions supported by the servlet.
     * <p>
     * This attribute is converted to values for the
     * <code>sling.servlet.extensions</code> property and is ignored if the
     * {@link #resourceTypes()} attribute is not set.
     */
    String[] extensions() default {};

    /**
     * One or more request methods supported by the servlet.
     * <p>
     * This attribute is converted to values for the
     * <code>sling.servlet.methods</code> property and is ignored if the
     * {@link #resourceTypes()} attribute is not set.
     */
    String[] methods() default {};

    /**
     * Defines the Component name also used as the PID for the Configuration
     * Admin Service. Default value: Fully qualified name of the Java class.
     * @since 1.6
     */
    String name() default "";

    /**
     * Whether Metatype Service data is generated or not. If this parameter is
     * set to true Metatype Service data is generated in the
     * <code>metatype.xml</code> file for this component. Otherwise no Metatype
     * Service data is generated for this component.
     * @since 1.6
     */
    boolean metatype() default false;

    /**
     * This is generally used as a title for the object described by the meta
     * type. This name may be localized by prepending a % sign to the name.
     * Default value: %&lt;name&gt;.name
     * @since 1.6
     */
    String label() default "";

    /**
     * This is generally used as a description for the object described by the
     * meta type. This name may be localized by prepending a % sign to the name.
     * Default value: %&lt;name&gt;.description
     * @since 1.6
     */
    String description() default "";
 }
