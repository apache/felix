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
package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.SerializableLambda;

/**
 * Lambda allowing to define fluent service properties. Property names are deduces from the lambda parameter name.
 * 
 * <p> Example of a component which provides fluent properties {"foo"="bar"; "foo2"=Integer(123)}:
 * 
 * <pre>{@code
 * public class Activator extends DependencyManagerActivator {
 *   public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       component(comp -> comp.impl(MyComponentImpl.class).provides(MyService.class, foo->"bar", foo2 -> 123));
 *   }
 * } 
 * }</pre>
 * 
 * <b>Caution: Fluent properties requires the usage of the "-parameter" javac option.</b>
 * 
 * Under eclipse, you can enable this option using:
 * 
 * <pre>{@code
 * Windows -> Preference -> Compiler -> Classfile Generation -> Store information about method parameters.
 * }</pre>
 */
@FunctionalInterface
public interface FluentProperty extends SerializableLambda {
    /**
     * Represents a fluent property
     * 
     * @param name the property name. The parameter used by the lambda will be intropsected and will be used as the actual property name.
     * @return the property value
     */
    public Object apply(String name);
}
