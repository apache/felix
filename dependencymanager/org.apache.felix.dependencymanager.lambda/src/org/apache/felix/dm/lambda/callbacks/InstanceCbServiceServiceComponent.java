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
 * Represents a swap callback(Service, Service, Component) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbServiceServiceComponent<S> extends SerializableLambda {
    /**
     * Handles the given arguments.
     * @param c the component
     * @param old the old service
     * @param replace the new service
     */
    void accept(S old, S replace, Component c);

    default InstanceCbServiceServiceComponent<S> andThen(InstanceCbServiceServiceComponent<S> after) {
        Objects.requireNonNull(after);
        return (S old, S replace, Component c) -> {
            accept(old, replace, c);
            after.accept(old, replace, c);
        };
    }
}
