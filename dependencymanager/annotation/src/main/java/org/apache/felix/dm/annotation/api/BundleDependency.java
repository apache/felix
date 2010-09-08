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
 * Annotates a method for a bundle dependency.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface BundleDependency
{
    /**
     * Returns the callback method to be invoked when the service have changed.
     */
    String changed() default "";

    /**
     * Returns the callback method to invoke when the service is lost.
     */
    String removed() default "";
    
    /**
     * Returns whether the dependency is required or not.
     */
    boolean required() default true;
    
    /**
     * Returns the filter dependency
     */
    String filter() default "";
     
    /**
     * Returns the bundle state mask
     */
    int stateMask() default Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;

    /**
     * Specifies if the manifest headers from the bundle should be propagated to 
     * the service properties.
     */
    boolean propagate() default false;
    
    /**
     * The name used when dynamically configuring this dependency from the init method.
     * Specifying this attribute allows to dynamically configure the dependency 
     * <code>filter</code> and <code>required</code> flag from the Service's init method.
     * All unnamed dependencies will be injected before the init() method; so from the init() method, you can
     * then pick up whatever information needed from already injected (unnamed) dependencies, and configure dynamically
     * your named dependencies, which will then be calculated once the init() method returns.
     */
    String name() default "";
}
