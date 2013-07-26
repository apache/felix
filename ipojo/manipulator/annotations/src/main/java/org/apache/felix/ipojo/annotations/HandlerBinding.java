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
 * A @{@link HandlerBinding} bind its annotated type to a given handler.
 *
 * The handler name is specified as parameter using the {@literal 'namespace:name'} format (qualified name).
 *
 * <pre>
 *     // Namespace and name will be inferred from the annotation's package name.
 *     &#64;HandlerBinding()
 *     public &#64;interface Foo {}
 *
 *     // No namespace declared, default will be used ('org.apache.felix.ipojo')
 *     &#64;HandlerBinding("foo")
 *     public &#64;interface Foo {}
 *
 *     // Namespace will be 'com.acme' and name: 'foo'
 *     &#64;HandlerBinding("com.acme:foo")
 *     public &#64;interface Foo {}
 *
 *     // Provided namespace and value (for the name) will be used
 *     &#64;HandlerBinding(namespace = "com.acme", value = "foo")
 *     public &#64;interface Foo {}
 * </pre>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface HandlerBinding {
    String DEFAULT = "#";

    /**
     * Defines the handler's namespace. Must be used in correlation with the {@literal value} attribute.
     * <pre>
     *     &#64;HandlerBinding(namespace = "com.acme", value = "foo")
     *     public &#64;interface Foo {}
     * </pre>
     */
    String namespace() default DEFAULT;

    /**
     * When used <b>without</b> the {@literal namespace} attribute, defines both the namespace + name
     * of a handler in a short notation (if no namespace can be found in the parameter - no ':' separator - fallback
     * on iPOJO's default namespace):
     *
     * <pre>
     *     &#64;HandlerBinding("com.acme:foo")
     *     public &#64;interface Foo {}
     * </pre>
     *
     * When used <b>with</b> the {@literal namespace} attribute, holds the name of the handler (without
     * its namespace part):
     *
     * <pre>
     *     &#64;HandlerBinding(namesapce = "com.acme", value = "foo")
     *     public &#64;interface Foo {}
     * </pre>
     */
    String value() default DEFAULT;
}
