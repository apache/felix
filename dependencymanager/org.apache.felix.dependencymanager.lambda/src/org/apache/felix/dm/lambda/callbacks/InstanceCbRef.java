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

import org.osgi.framework.ServiceReference;

/**
 * Represents a callback(ServiceReference) on an Object instance.
 * 
 * <p> The type of the service passed in argument to the callback is defined by the "S" generic parameter.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FunctionalInterface
public interface InstanceCbRef<S> {
    /**
     * Handles the given arguments.
     * @param ref a Service Reference
     * @param service a Service
     */
    void accept(ServiceReference<S> ref);

    default InstanceCbRef<S> andThen(InstanceCbRef<S> after) {
        Objects.requireNonNull(after);
        return (ServiceReference<S> ref) -> {
            accept(ref);
            after.accept(ref);
        };
    }
}
