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
package org.apache.felix.dm.lambda.callbacks;

import java.util.Objects;

import org.apache.felix.dm.Component;

/**
 * Represents a callback(Component) that is invoked on a Component implementation class. 
 * The type of the component implementation class on which the callback is invoked on is represented by the T generic parameter.
 * The component callback accepts in argument a Component parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface CbComponent<T> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param instance the Component implementation class instance on which the callback is invoked on. 
     * @param component the callback parameter
     */
    void accept(T instance, Component component);

    default CbComponent<T> andThen(CbComponent<? super T> after) {
        Objects.requireNonNull(after);
        return (T instance, Component component) -> {
            accept(instance, component);
            after.accept(instance, component);
        };
    }
}
