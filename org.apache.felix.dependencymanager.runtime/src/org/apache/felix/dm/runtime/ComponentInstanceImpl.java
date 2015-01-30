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

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.runtime.api.ComponentInstance;
import org.osgi.framework.Bundle;

/**
 * Implementation for our DM Runtime ComponentInstance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentInstanceImpl implements ComponentInstance {
    /**
     * The list of Dependencies which are applied in the Service.
     */
    private final MetaData m_srvMeta;

    /**
     * The list of Dependencies which are applied in the Service.
     */
    private final List<MetaData> m_depsMeta;

    /**
     * The DependencyManager which is used to create Service instances.
     */
    private final DependencyManager m_dm;

    /**
     * The bundle containing the Service annotated with the factory attribute.
     */
    private final Bundle m_bundle;
    
    /**
     * The component 
     */
    private final Object m_impl;
    
    /**
     * The DM Component used to define the component
     */
    private final Component m_component;

    public ComponentInstanceImpl(DependencyManager dm, Bundle b, MetaData srvMeta, List<MetaData> depsMeta, Dictionary<String, ?> conf) throws Exception
    {
        m_bundle = b;
        m_dm = dm;
        m_srvMeta = srvMeta;
        m_depsMeta = depsMeta;
        m_component = m_dm.createComponent();
        
        Class<?> implClass = m_bundle.loadClass(m_srvMeta.getString(Params.impl));
        Object impl = conf.get(ComponentBuilder.FACTORY_INSTANCE);
        if (impl == null) {
            String factoryMethod = m_srvMeta.getString(Params.factoryMethod, null);
            if (factoryMethod == null) {
                impl = implClass.newInstance();
            } else {
                Method m = implClass.getDeclaredMethod(factoryMethod);
                m.setAccessible(true);
                impl = m.invoke(null);
            }
        }
        m_impl = impl;

        // Invoke "configure" callback
        String configure = m_srvMeta.getString(Params.factoryConfigure, null);

        if (configure != null) {
            invokeConfigure(impl, configure, conf);
        }

        // Create Service
        m_component.setImplementation(impl);
        String[] provides = m_srvMeta.getStrings(Params.provides, null);
        if (provides != null) {
            // Merge service properties with the configuration provided by the factory.
            Dictionary<String, ?> serviceProperties = m_srvMeta.getDictionary(Params.properties, null);
            serviceProperties = mergeSettings(serviceProperties, conf);
            m_component.setInterface(provides, serviceProperties);
        }

        m_component.setComposition(m_srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(m_component, m_bundle, m_dm, m_srvMeta, m_depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        m_component.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");

        // Adds dependencies (except named dependencies, which are managed by the lifecycle handler).
        for (MetaData dependency : m_depsMeta) {
            String name = dependency.getString(Params.name, null);
            if (name == null) {
                DependencyBuilder depBuilder = new DependencyBuilder(dependency);
                Log.instance().info("ServiceLifecycleHandler.init: adding dependency %s into service %s", dependency,
                    m_srvMeta);
                Dependency d = depBuilder.build(m_bundle, m_dm);
                m_component.add(d);
            }
        }

        // Register the Service instance, and keep track of it.
        Log.instance().info("ServiceFactory: created service %s", m_srvMeta);
        m_dm.add(m_component);
    }

    @Override
    public void dispose() {
        m_dm.remove(m_component);
    }

    @Override
    public void update(Dictionary<String, ?> conf) {
        // Reconfigure an already existing Service.
        String configure = m_srvMeta.getString(Params.factoryConfigure, null);
        if (configure != null) {
            Log.instance().info("ServiceFactory: updating service %s", m_impl);
            invokeConfigure(m_impl, configure, conf);
        }

        // Update service properties
        String[] provides = m_srvMeta.getStrings(Params.provides, null);
        if (provides != null) {
            Dictionary<String, ?> serviceProperties = m_srvMeta.getDictionary(Params.properties, null);
            serviceProperties = mergeSettings(serviceProperties, conf);
            m_component.setServiceProperties(serviceProperties);
        }
    }

    /**
     * Invokes the configure callback method on the service instance implemenatation.
     * @param impl
     * @param configure
     * @param config
     */
    private void invokeConfigure(Object impl, String configure, Dictionary<String, ?> config) {
        try {
            InvocationUtil.invokeCallbackMethod(impl, configure, new Class[][] { { Dictionary.class } },
                new Object[][] { { config } });
        }

        catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException("Could not invoke method " + configure + " on object " + impl, t);
            }
        }
    }

    /**
     * Merge factory configuration settings with the service properties. The private factory configuration 
     * settings are ignored. A factory configuration property is private if its name starts with a dot (".").
     * 
     * @param serviceProperties
     * @param factoryConfiguration
     * @return
     */
    private Dictionary<String, Object> mergeSettings(Dictionary<String, ?> serviceProperties,
        Dictionary<String, ?> factoryConfiguration)
    {
        Dictionary<String, Object> props = new Hashtable<>();

        if (serviceProperties != null) {
            Enumeration<String> keys = serviceProperties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Object val = serviceProperties.get(key);
                props.put(key, val);
            }
        }

        Enumeration<String> keys = factoryConfiguration.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (!key.toString().startsWith(".")) {
                // public properties are propagated
                Object val = factoryConfiguration.get(key);
                props.put(key, val);
            }
        }
        return props;
    }
}
