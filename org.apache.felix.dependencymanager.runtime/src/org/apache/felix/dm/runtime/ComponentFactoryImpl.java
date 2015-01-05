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
package org.apache.felix.dm.runtime;

import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.runtime.api.ComponentException;
import org.apache.felix.dm.runtime.api.ComponentFactory;
import org.apache.felix.dm.runtime.api.ComponentInstance;
import org.osgi.framework.Bundle;

/**
 * This class implements a DM Component factory service.
 * When a <code>Component</annotation> contains a <code>factoryName</code> attribute, this class is provided
 * into the OSGi registry with a <code>org.apache.felix.dependencymanager.factory.name</code> service property. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentFactoryImpl implements ComponentFactory {
    /**
     * The list of Dependencies which are applied in the Service.
     */
    private MetaData m_srvMeta;

    /**
     * The list of Dependencies which are applied in the Service.
     */
    private List<MetaData> m_depsMeta;

    /**
     * The DependencyManager which is used to create Service instances.
     */
    private DependencyManager m_dm;

    /**
     * Flag used to check if our service is Active.
     */
    private volatile boolean m_active;

    /**
     * The bundle containing the Service annotated with the factory attribute.
     */
    private final Bundle m_bundle;

    /**
     * Sole constructor.
     * @param b the bundle containing the Service annotated with the factory attribute
     * @param srvMeta the component service metadata
     * @param depsMeta teh component dependencies metadata
     */
    public ComponentFactoryImpl(Bundle b, MetaData srvMeta, List<MetaData> depsMeta) {
        m_bundle = b;
        m_srvMeta = srvMeta;
        m_depsMeta = depsMeta;
    }

    /**
     * Our Service is starting. 
     */
    public void start(Component c) {
        m_active = true;
        m_dm = c.getDependencyManager();
    }

    /**
     * Our Service is stopping.
     */
    public void stop() {
        m_active = false;
    }

    /**
     * Create or Update a Service.
     */
    public ComponentInstance newInstance(Dictionary<String, ?> conf) {
        // Check parameter validity
        if (conf == null) {
            throw new NullPointerException("configuration parameter can't be null");
        }

        // Check if our service is running.
        checkServiceAvailable();

        try {
            ComponentInstanceImpl instance = new ComponentInstanceImpl(m_dm,m_bundle, m_srvMeta, m_depsMeta, conf);
            return instance;
            
        } catch (Throwable t) {
            Log.instance().error("ServiceFactory: could not instantiate service %s", t, m_srvMeta);
            throw new ComponentException("could not instantiate factory component", t);
        }   
    }

    /**
     * Checks if our Service is available (we are not stopped").
     */
    private void checkServiceAvailable() {
        if (!m_active) {
            throw new IllegalStateException("Service not available");
        }
    }
}
