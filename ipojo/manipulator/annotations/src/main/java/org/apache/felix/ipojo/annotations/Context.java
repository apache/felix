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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the bundle context object.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.FIELD})
public @interface Context {

    /**
     * Sets which bundle context object needs to be injected.
     */
    public Source value() default Source.COMPONENT;

    /**
     * Lists the different injectable bundle context.
     */
    public enum Source {
        /**
         * The bundle context from the bundle declaring the component. Generally,
         * it's the bundle containing the implementation class.
         */
        COMPONENT,

        /**
         * The bundle context from the bundle declaring the instance.
         */
        INSTANCE
    }

}
