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
 * Annotates an Adapater Service. The adapter will be applied to any service that
 * matches the implemented interface and filter. The adapter will be registered 
 * with the specified interface and existing properties from the original service 
 * plus any extra properties you supply here. It will also inherit all dependencies, 
 * and if you declare the original service as a member it will be injected.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AdapterService
{
    /**
     * Sets the adapter service interface(s). By default, the directly implemented interface(s) is (are) used.
     */
    Class<?>[] provides() default {};

    /**
     * Sets some additional properties to use with the adapter service registration. By default, 
     * the adapter will inherit all adaptee service properties.
     */
    Property[] properties() default {};

    /**
     * Sets the adaptee service interface this adapter is applying to.
     */
    Class<?> adapteeService();
    
    /**
     * Sets the filter condition to use with the adapted service interface.
     */
    String adapteeFilter() default "";
    
    /**
     * Sets the static method used to create the adapter service implementation instance.
     * By default, the default constructor of the annotated class is used.
     */
    String factoryMethod() default "";
}
