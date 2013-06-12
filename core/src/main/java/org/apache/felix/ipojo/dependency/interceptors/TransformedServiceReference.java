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

package org.apache.felix.ipojo.dependency.interceptors;

import org.apache.felix.ipojo.Factory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.*;

/**
 * Transformed service reference is an interface letting updating the properties of a service reference.
 *
 * Transformed service reference wraps a <i>real</i> service reference and has all its properties.
 */
public interface TransformedServiceReference<S> extends ServiceReference<S> {

    /**
     * These properties are cannot be removed, added or updated.
     */
    public static final List<String> FORBIDDEN_KEYS = Arrays.asList(
            Constants.SERVICE_ID,
            Constants.SERVICE_PID,
            Factory.INSTANCE_NAME_PROPERTY
    );


    /**
     * Adds a property to the reference
     * @param name the property name
     * @param value the value (must not be null)
     * @return the current transformed service reference
     */
    public TransformedServiceReference<S> addProperty(String name, Object value);

    /**
     * Adds a property to the service reference if this property is not already set on the reference.
     * @param name the property name
     * @param value the value
     * @return the current transformed service reference
     */
    public TransformedServiceReference<S> addPropertyIfAbsent(String name, Object value);

    /**
     * Gets the current value of a property.
     * @param name the property name
     * @return the current value of the property, {@literal null} if not in the properties.
     */
    public Object get(String name);

    /**
     * Removes a property from the reference.
     * @param name the property name
     * @return the current transformed service reference
     */
    public TransformedServiceReference<S> removeProperty(String name);

    /**
     * Does the service reference contains the given property ?
     * @param name the property name
     * @return whether the current reference contains a property with the given name
     */
    public boolean contains(String name);

    /**
     * Gets the wrapped service reference
     * @return the wrapped service reference
     */
    public ServiceReference<S> getWrappedReference();
}
