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

package org.apache.felix.ipojo.extender.internal.declaration.service;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.extender.ConfigurationBuilder;
import org.apache.felix.ipojo.extender.DeclarationHandle;

/**
 * Declares a configuration and build the immutable {@link org.apache.felix.ipojo.extender.DeclarationHandle}
 * containing that configuration.
 */
public class DefaultConfigurationBuilder implements ConfigurationBuilder {

    private final DefaultInstanceBuilder builder;
    private Map<String, Object> configuration = new Hashtable<String, Object>();

    public DefaultConfigurationBuilder(final DefaultInstanceBuilder builder) {
        this.builder = builder;
    }

    public ConfigurationBuilder property(final String name, final Object value) {
        configuration.put(name, value);
        return this;
    }

    public ConfigurationBuilder remove(final String name) {
        configuration.remove(name);
        return this;
    }

    public ConfigurationBuilder clear() {
        configuration.clear();
        return this;
    }

    public DeclarationHandle build() {
        // Make a fresh dictionary instance, needed for immutable instance declarations
        return builder.build(new Hashtable<String, Object>(configuration));
    }
}
