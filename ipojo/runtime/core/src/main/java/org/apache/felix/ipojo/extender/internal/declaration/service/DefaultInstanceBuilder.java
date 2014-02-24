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

import static java.lang.String.format;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.extender.ConfigurationBuilder;
import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultInstanceDeclaration;
import org.osgi.framework.BundleContext;

/**
 * User: guillaume
 * Date: 13/02/2014
 * Time: 09:36
 */
public class DefaultInstanceBuilder implements InstanceBuilder {

    private BundleContext context;
    private String name;
    private String type;
    private String version;

    public DefaultInstanceBuilder(final BundleContext context, final String type) {
        this.context(context);
        this.type(type);
    }

    public InstanceBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public InstanceBuilder type(final String type) {
        if (type == null) {
            throw new IllegalArgumentException(format("'type' parameter cannot be null (instance must be of a given type)"));
        }
        this.type = type;
        return this;
    }

    public InstanceBuilder type(final Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException(format("'type' parameter cannot be null (instance must be of a given type)"));
        }
        return type(type.getName());
    }

    public InstanceBuilder version(final String version) {
        this.version = version;
        return this;
    }

    public InstanceBuilder context(final BundleContext context) {
        if (context == null) {
            throw new IllegalArgumentException(format("'context' parameter cannot be null"));
        }
        this.context = context;
        return this;
    }

    public ConfigurationBuilder configure() {
        return new DefaultConfigurationBuilder(this);
    }

    /**
     * Only called through ConfigurationBuilder to apply the created configuration to this instance
     * @param configuration
     */
    public DeclarationHandle build(Dictionary<String, Object> configuration) {

        // Prepare the instance configuration
        if (configuration == null) {
            configuration = new Hashtable<String, Object>();
        }

        if (name != null) {
            configuration.put(Factory.INSTANCE_NAME_PROPERTY, name);
        }

        if (version != null) {
            configuration.put(Factory.FACTORY_VERSION_PROPERTY, version);
        }

        return new DefaultInstanceDeclaration(context, type, configuration);

    }

    public DeclarationHandle build() {
        return build(new Hashtable<String, Object>());
    }
}
