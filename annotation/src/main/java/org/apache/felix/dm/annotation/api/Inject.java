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
 * Inject classes in a component instance field.
 * The following injections are currently performed, depending on the type of the
 * field this annotation is applied on:
 * <dl>
 * <dt>BundleContext</dt><dd>the bundle context of the bundle</dd>
 * <dt>DependencyManager</dt><dd>the dependency manager instance</dd>
 * <dt>Component</dt><dd>the component instance of the dependency manager</dd>
 * </dl>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Inject
{
}
