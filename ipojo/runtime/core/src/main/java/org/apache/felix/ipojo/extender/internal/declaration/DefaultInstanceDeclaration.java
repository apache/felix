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

package org.apache.felix.ipojo.extender.internal.declaration;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Default implementation of the instance declaration.
 */
public class DefaultInstanceDeclaration extends AbstractDeclaration implements InstanceDeclaration {

    private static final Dictionary<String, Object> EMPTY_DICTIONARY = new Hashtable<String, Object>();

    private final String m_componentName;
    private final Dictionary<String, Object> m_configuration;
    private final String m_componentVersion;
    private final String m_instanceName;

    public DefaultInstanceDeclaration(BundleContext bundleContext, String componentName) {
        this(bundleContext, componentName, EMPTY_DICTIONARY);
    }

    public DefaultInstanceDeclaration(BundleContext bundleContext, String componentName, Dictionary<String, Object> configuration) {
        super(bundleContext, InstanceDeclaration.class);
        m_componentName = componentName;
        m_configuration = configuration;
        m_componentVersion = initComponentVersion();
        m_instanceName = initInstanceName();
    }

    private String initInstanceName() {
        String name = (String) m_configuration.get(Factory.INSTANCE_NAME_PROPERTY);
        if (name == null) {
            name = UNNAMED_INSTANCE;
        }
        return name;
    }

    private String initComponentVersion() {
        return (String) m_configuration.get(Factory.FACTORY_VERSION_PROPERTY);
    }

    public Dictionary<String, Object> getConfiguration() {
        return m_configuration;
    }

    public String getComponentName() {
        return m_componentName;
    }

    public String getComponentVersion() {
        return m_componentVersion;
    }

    public String getInstanceName() {
        return m_instanceName;
    }

    /**
     * Gets the bundle that is declaring this instance.
     *
     * @return the bundle object
     * @since 1.11.2
     */
    public Bundle getBundle() {
        return m_bundleContext.getBundle();
    }

    @Override
    protected Dictionary<String, ?> getServiceProperties() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(InstanceDeclaration.COMPONENT_NAME_PROPERTY, m_componentName);

        String version = getComponentVersion();
        if (version != null) {
            properties.put(InstanceDeclaration.COMPONENT_VERSION_PROPERTY, version);
        }

        properties.put(InstanceDeclaration.INSTANCE_NAME, m_instanceName);

        return properties;
    }

}
