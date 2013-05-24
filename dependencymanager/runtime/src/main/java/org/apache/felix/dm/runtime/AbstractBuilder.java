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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Base class for all kind of DM component builders (for Component, Aspect, Adapters ...).
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractBuilder
{
    /**
     * Returns the service component type.
     */
    abstract String getType();

    /**
     * Builds the service component.
     * @param serviceMetaData the service component metadata parsed from the descriptor file.
     * @param serviceDependencies the service component dependencies metadata parsed from the descriptor file.
     */
    abstract void build(MetaData serviceMetaData, List<MetaData> serviceDependencies, Bundle b,
        DependencyManager dm)
        throws Exception;

    /**
     * Sets common Service parameters, if provided from our Component descriptor
     */
    protected void setCommonServiceParams(Component c, MetaData srvMeta)
        throws Exception
    {
        // Set auto configured component fields.
        DependencyManager dm = c.getDependencyManager();
        boolean autoConfigureComponents =
                "true".equals(dm.getBundleContext().getProperty(Activator.CONF_ENABLE_AUTOCONFIG));

        if (!autoConfigureComponents)
        {
            c.setAutoConfig(BundleContext.class, Boolean.FALSE);
            c.setAutoConfig(ServiceRegistration.class, Boolean.FALSE);
            c.setAutoConfig(DependencyManager.class, Boolean.FALSE);
            c.setAutoConfig(Component.class, Boolean.FALSE);
        }

        // See if BundleContext must be auto configured.
        String bundleContextField = srvMeta.getString(Params.bundleContextField, null);
        if (bundleContextField != null)
        {
            c.setAutoConfig(BundleContext.class, bundleContextField);
        }

        // See if DependencyManager must be auto configured.
        String dependencyManagerField = srvMeta.getString(Params.dependencyManagerField, null);
        if (dependencyManagerField != null)
        {
            c.setAutoConfig(DependencyManager.class, dependencyManagerField);
        }

        // See if Component must be auto configured.
        String componentField = srvMeta.getString(Params.componentField, null);
        if (componentField != null)
        {
            c.setAutoConfig(Component.class, componentField);
        }
        
        // Now, if the component has a @Started annotation, then add our component state listener,
        // which will callback the corresponding annotated method, once the component is started.
        String registered = srvMeta.getString(Params.registered, null);
        String unregistered = srvMeta.getString(Params.unregistered, null);

        if (registered != null || unregistered != null)
        {
            c.addStateListener(new RegistrationListener(registered, unregistered));
        }
    }
    
    /**
     * Registers all unnamed dependencies into a given service. Named dependencies are
     * handled differently, and are managed by the ServiceLifecycleHandler class.
     * @throws Exception 
     */
    protected static void addUnamedDependencies(Bundle b, DependencyManager dm, Component s, 
                                                MetaData srvMeta, List<MetaData> depsMeta) 
        throws Exception
    {
        for (MetaData dependency : depsMeta) 
        {
            String name = dependency.getString(Params.name, null);
            if (name == null) {
                DependencyBuilder depBuilder = new DependencyBuilder(dependency);
                Log.instance().info("adding dependency %s into service %s",
                                   dependency, srvMeta);
                Dependency d = depBuilder.build(b, dm, false);
                s.add(d);
            }
        }
    }
    
    static class RegistrationListener implements ComponentStateListener
    {
        private final String m_registered;
        private String m_unregistered;

        RegistrationListener(String registered, String unregistered)
        {
            m_registered = registered;
            m_unregistered = unregistered;
        }
        
        public void starting(Component c)
        {
            // No need to invoke any callback here, since it is the ServiceLifecycleHandler
            // that will invoke the method annotated with @Start
        }

        public void started(Component c)
        {
            if (m_registered != null)
            {
                // The component has registered a service: notify all component instances
                Object[] componentInstances = c.getCompositionInstances();
                for (Object instance : componentInstances)
                {
                    try
                    {
                        Class[][] signatures = new Class[][] { { ServiceRegistration.class }, {} };
                        Object[][] params = new Object[][] { { c.getServiceRegistration() }, {} };
                        InvocationUtil.invokeCallbackMethod(instance, m_registered, signatures, params);
                    }
                    catch (Throwable t)
                    {
                        Log.instance().error("Exception caught while invoking method %s on component %s",
                                             t,
                                             m_registered,
                                             instance);
                    }
                }
            }
        }

        public void stopping(Component c)
        {
            // No need to invoke any callback here, since it is the ServiceLifecycleHandler
            // that will invoke the method annotated with @Stop
        }

        public void stopped(Component c)
        {
            if (m_unregistered != null)
            {
                Object instance = c.getService();
                try
                {
                    InvocationUtil
                        .invokeCallbackMethod(instance,
                                              m_unregistered, 
                                              new Class[][]  {{}},
                                              new Object[][] {{}});
                }
                catch (Throwable t)
                {
                    Log.instance().error("Exception caught while invoking method %s on component %s", t, m_registered, instance);
                }
            }
        }
    }
}
