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
 * A @{@link Ignore} annotation mark its annotated type to be ignored by the iPOJO manipulator.
 * This is handy when the annotation could be recognized as a "custom annotation handler" (contains {@literal ".ipojo."}
 * or {@literal ".handler."} in its package name) but in fact is not a handler annotation at all.
 *
 * <pre>
 *     package com.acme.ipojo.handler;
 *     &#64;Ignore
 *     public &#64;interface Foo {}
 * </pre>
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Ignore {}
