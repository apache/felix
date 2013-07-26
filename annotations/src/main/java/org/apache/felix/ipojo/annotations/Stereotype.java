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
 * In many systems, use of architectural patterns produces a set of recurring roles. A stereotype allows a
 * framework developer to identify such a role and declare some common metadata for objects with that role
 * in a central place.
 *
 * A stereotype is an annotation, annotated with {@literal @Stereotype}, that captures several other annotations.
 *
 * For example, the following stereotype defines a @PseudoSingletonComponent annotation, that will act,
 * when applied on a component, just like if @Component and @Instantiate where directly applied on the target component.
 * <pre>
 *
 *     &#64;Component
 *     &#64;Instantiate
 *     &#64;Stereotype
 *     &#64;Target(TYPE)
 *     &#64;Retention(CLASS)
 *     public &#64;interface PseudoSingletonComponent {}
 *
 * </pre>
 *
 * Usage:
 * <pre>
 *
 *     &#64;PseudoSingletonComponent
 *     public class HelloWorldComponent {
 *       // ...
 *     }
 *
 * </pre>
 *
 * Equivalent to:
 * <pre>
 *
 *     &#64;Component
 *     &#64;Instantiate
 *     public class HelloWorldComponent {
 *       // ...
 *     }
 *
 * </pre>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Stereotype {
}
