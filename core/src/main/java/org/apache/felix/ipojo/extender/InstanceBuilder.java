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

import org.osgi.framework.BundleContext;

/**
 * Support class for fluent instance declaration building.
 * This class can be used to specify instance details like naming, component's type, component's version.
 * The {@link #context(org.osgi.framework.BundleContext)} method can be used to override the default
 * {@link org.osgi.framework.BundleContext} used for declaration service registration.
 *
 * @since 1.11.2
 */
public interface InstanceBuilder {

    /**
     * Specify the instance name. Using {@literal null} will result in an anonymous instance.
     * @param name instance name or {@literal null}
     * @return this builder
     */
    InstanceBuilder name(String name);

    /**
     * Specify the component's type of this instance.
     * @param type type of the instance (cannot be {@literal null}).
     * @return this builder
     */
    InstanceBuilder type(String type);

    /**
     * Specify the component's type of this instance.
     * @param type type of the instance (cannot be {@literal null}).
     * @return this builder
     */
    InstanceBuilder type(Class<?> type);

    /**
     * Specify the component's version of this instance. If {@literal null} is used,
     * the factory without any version attribute will be used.
     * @param version component's version (can be {@literal null}).
     * @return this builder
     */
    InstanceBuilder version(String version);

    /**
     * Override the default BundleContext used for declaration service registration.
     * Use this with <bold>caution</bold>, for example, if the bundle does not import the
     * <code>{@link org.apache.felix.ipojo.extender}</code> package, declarations may not be
     * linked and activated properly.
     * @param context new context to be used.
     * @return this builder
     */
    InstanceBuilder context(BundleContext context);

    /**
     * Access the dedicated builder for configuration (properties).
     * Notice that when this method is used, the called must use {@link ConfigurationBuilder#build()}
     * to build the instance declaration configured with the right set of properties. Any attempt
     * to use {@link #build()} will result in a new declaration with no properties associated.
     *
     * Good usage (produced declaration has the associated properties):
     * <pre>
     *     DeclarationHandle handle = builder.configure()
     *                                            .property("hello", "world")
     *                                            .build();
     * </pre>
     *
     * Bad usage (produced declaration does not have the associated properties):
     * <pre>
     *     builder.configure()
     *                .property("hello", "world");
     *
     *     DeclarationHandle handle = builder.build();
     * </pre>
     *
     * @return the instance configuration builder
     */
    ConfigurationBuilder configure();

    /**
     * Build the declaration handle (never contains any configuration).
     * Notice that the declaration is not yet published (no automatic activation).
     * The client has to do it through {@link DeclarationHandle#publish()}
     * @return the handle to the declaration
     */
    DeclarationHandle build();
}
