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


/**
 * This annotation declares a static service property.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public @interface StaticServiceProperty {

    /**
     * Set the property name.
     * This name is mandatory for static properties.
     */
    String name();

    /**
     * Set the property value.
     * Default : empty
     */
    String value() default "";

    /**
     * Is the property mandatory?
     * Default: false
     */
    boolean mandatory() default false;

    /**
     * Set the type.
     * This value is required only for static properties.
     */
    String type();
}
