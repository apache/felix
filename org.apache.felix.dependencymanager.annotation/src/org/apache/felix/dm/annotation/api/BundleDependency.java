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

import org.osgi.framework.Bundle;

/**
 * Annotates a class or method for a bundle dependency. A bundle dependency allows you to 
 * depend on a bundle in a certain set of states (INSTALLED|RESOLVED|STARTED|...), as 
 * indicated by a state mask. You can also use a filter condition that is matched against 
 * all manifest entries. When applied on a class field, optional unavailable dependencies 
 * are injected with a NullObject.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> In the following example, the "SCR" Component allows to track 
 * all bundles containing a specific "Service-Component" OSGi header, in order to load
 * and manage all Declarative Service components specified in the SCR xml documents referenced by the header:
 * 
 * <blockquote>
 * <pre>
 * &#64;Component
 * public class SCR {
 *     &#64;BundleDependency(required = false,
 *                       removed = "unloadServiceComponents", 
 *                       filter = "(Service-Component=*)"
 *                       stateMask = Bundle.ACTIVE)
 *     void loadServiceComponents(Bundle b) {
 *         String descriptorPaths = (String) b.getHeaders().get("Service-Component");
 *         // load all service component specified in the XML descriptorPaths files ...
 *     }
 *
 *     void unloadServiceComponents(Bundle b) {
 *         // unload all service component we loaded from our "loadServiceComponents" method.
 *     }
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface BundleDependency
{
    /**
     * Returns the callback method to be invoked when the service have changed.
     * @return the change callback
     */
    String changed() default "";

    /**
     * Returns the callback method to invoke when the service is lost.
     * @return the remove callback
     */
    String removed() default "";
    
    /**
     * Returns whether the dependency is required or not.
     * @return the required flag
     */
    boolean required() default true;
    
    /**
     * Returns the filter dependency
     * @return the filter
     */
    String filter() default "";
     
    /**
     * Returns the bundle state mask
     * @return the state mask
     */
    int stateMask() default Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;

    /**
     * Specifies if the manifest headers from the bundle should be propagated to 
     * the service properties.
     * @return the propagation flag
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
     * @return the dependency name used to dynamically configure the dependency from the init callback
     */
    String name() default "";
}
