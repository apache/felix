/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender;

/**
 * Support class for fluent instance declaration building.
 * This class can be used to provide values for instance configuration.
 * Any object can be used as configuration values (List and Maps are accepted for example).
 *
 * @since 1.11.2
 */
public interface ConfigurationBuilder {

    /**
     * Provide a property value.
     * @param name property name
     * @param value property value
     * @return this builder
     */
    ConfigurationBuilder property(String name, Object value);

    /**
     * Remove a property from the configuration.
     * This does not affect already created declarations (from this builder).
     * @param name property name
     * @return this builder
     */
    ConfigurationBuilder remove(String name);

    /**
     * Remove all properties from the configuration.
     * This does not affect already created declarations (from this builder).
     * @return this builder
     */
    ConfigurationBuilder clear();

    /**
     * Build the declaration handle (contains the instance configuration).
     * Notice that the declaration is not yet published (no automatic activation).
     * The client has to do it through {@link DeclarationHandle#publish()}
     * @return the handle to the configured declaration
     */
    DeclarationHandle build();
}
