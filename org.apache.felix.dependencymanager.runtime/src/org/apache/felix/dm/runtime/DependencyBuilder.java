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

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceDependency;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.Bundle;

/**
 * Class used to build a concrete dependency from meta data.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyBuilder
{
    public enum DependencyType
    {
        ServiceDependency,
        TemporalServiceDependency,
        ConfigurationDependency,
        BundleDependency,
        ResourceDependency
    }

    private MetaData m_metaData;

    public DependencyBuilder(MetaData dependencyMetaData)
    {
        m_metaData = dependencyMetaData;
    }

    public Dependency build(Bundle b, DependencyManager dm)
        throws Exception
    {
        Dependency dp = null;
        DependencyType type;

        try
        {
            type = DependencyType.valueOf(m_metaData.getString(Params.type));
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("no \"type\" parameter found from metaData: " + m_metaData);
        }

        switch (type)
        {
            case ServiceDependency:
                dp = createServiceDependency(b, dm);
                break;

            case ConfigurationDependency:
                dp = createConfigurationDependency(b, dm);
                break;

            case BundleDependency:
                dp = createBundleDependency(dm);
                break;

            case ResourceDependency:
                dp = createResourceDependency(dm);
                break;
            
            default:
                throw new IllegalArgumentException("Can't build service dependency: " + type);
        }
        return dp;
    }

    private Dependency createServiceDependency(Bundle b, DependencyManager dm)
        throws ClassNotFoundException
    {
        String service = m_metaData.getString(Params.service);
        Class<?> serviceClass = b.loadClass(service);
        String serviceFilter = m_metaData.getString(Params.filter, null);
        String defaultServiceImpl = m_metaData.getString(Params.defaultImpl, null);
        Class<?> defaultServiceImplClass =
            (defaultServiceImpl != null) ? b.loadClass(defaultServiceImpl) : null;
        String added = m_metaData.getString(Params.added, null);
        long timeout = m_metaData.getLong(Params.timeout, -1L);
        String changed = timeout != -1 ? null : m_metaData.getString(Params.changed, null);
        String removed = timeout != -1 ? null : m_metaData.getString(Params.removed, null);
        String autoConfigField = m_metaData.getString(Params.autoConfig, null);
        boolean required = "true".equals(m_metaData.getString(Params.required, "true"));
        boolean propagate = "true".equals(m_metaData.getString(Params.propagate, "false"));

        Dependency dp = createServiceDependency(dm, serviceClass,
            serviceFilter, defaultServiceImplClass, added, changed,
            removed, autoConfigField, timeout, required, propagate);
        return dp;
    }

    private Dependency createServiceDependency(DependencyManager dm, Class<?> serviceClass, 
        String serviceFilter, Class<?> defaultServiceImplClass, String added,
        String changed, String removed, String autoConfigField, long timeout, boolean required,
        boolean propagate)
    {
        ServiceDependency sd = timeout != -1 ? dm.createTemporalServiceDependency(timeout)
            : dm.createServiceDependency();
        sd.setService(serviceClass, serviceFilter);
        if (defaultServiceImplClass != null)
        {
            sd.setDefaultImplementation(defaultServiceImplClass);
        }
        sd.setCallbacks(added, changed, removed);
        if (autoConfigField != null)
        {
            sd.setAutoConfig(autoConfigField);
        }
        if (timeout == -1)
        {
            sd.setRequired(required);
        }
        
        sd.setPropagate(propagate);
        return sd;
    }

    private Dependency createConfigurationDependency(Bundle b, DependencyManager dm) throws Exception
    {
        String confProxyType = m_metaData.getString(Params.configType, null);
        String pid = m_metaData.getString(Params.pid);
        boolean propagate = "true".equals(m_metaData.getString(Params.propagate, "false"));
        String callback = m_metaData.getString(Params.updated, "updated");
        return createConfigurationDependency(dm, b, pid, callback, confProxyType, propagate);
    }

    private Dependency createConfigurationDependency(DependencyManager dm, Bundle b, String pid, String callback, String confProxyType, boolean propagate) 
        throws ClassNotFoundException
    {
        if (pid == null)
        {
            throw new IllegalArgumentException(
                "pid attribute not provided in ConfigurationDependency declaration");
        }
        ConfigurationDependency cd = dm.createConfigurationDependency();
        cd.setPid(pid);
        if (confProxyType != null) 
        {
            Class<?> confProxyTypeClass = b.loadClass(confProxyType); 
            cd.setCallback(callback, confProxyTypeClass);            
        }
        else
        {
            cd.setCallback(callback);            
        }
        cd.setPropagate(propagate);
        return cd;
    }

    /**
     * Creates a BundleDependency that we parsed from a component descriptor entry.
     * @param b
     * @param dm
     * @param parser
     * @return
     */
    private Dependency createBundleDependency(DependencyManager dm)
    {
        String added = m_metaData.getString(Params.added, null);
        String changed = m_metaData.getString(Params.changed, null);
        String removed = m_metaData.getString(Params.removed, null);
        boolean required = "true".equals(m_metaData.getString(Params.required, "true"));
        String filter = m_metaData.getString(Params.filter, null);
        int stateMask = m_metaData.getInt(Params.stateMask, -1);
        boolean propagate = "true".equals(m_metaData.getString(Params.propagate, "false"));

        Dependency dp = createBundleDependency(dm, added, changed, removed, required, propagate, filter,
            stateMask);
        return dp;
    }

    private Dependency createBundleDependency(DependencyManager dm, String added, String changed,
        String removed, boolean required, boolean propagate, String filter, int stateMask)
    {
        BundleDependency bd = dm.createBundleDependency();
        bd.setCallbacks(added, changed, removed);
        bd.setRequired(required);
        bd.setPropagate(propagate);
        if (filter != null)
        {
            bd.setFilter(filter);
        }
        if (stateMask != -1)
        {
            bd.setStateMask(stateMask);
        }
        return bd;
    }

    private Dependency createResourceDependency(DependencyManager dm)
    {
        String added = m_metaData.getString(Params.added, null);
        String changed = m_metaData.getString(Params.changed, null);
        String removed = m_metaData.getString(Params.removed, null);
        String filter = m_metaData.getString(Params.filter, null);
        boolean required = "true".equals(m_metaData.getString(Params.required, "true"));
        boolean propagate = "true".equals(m_metaData.getString(Params.propagate, "false"));
        String autoConfigField = m_metaData.getString(Params.autoConfig, null);

        Dependency dp = createResourceDependency(dm, added, changed, removed, required, filter, 
                                                 propagate, autoConfigField);
        return dp;
    }

    private Dependency createResourceDependency(DependencyManager dm, String added,
        String changed, String removed, boolean required, String filter, boolean propagate, 
        String autoConfigField)
    {
        ResourceDependency rd = dm.createResourceDependency();
        rd.setCallbacks(added, changed, removed);
        rd.setRequired(required);
        if (filter != null)
        {
            rd.setFilter(filter);
        }
        if (autoConfigField != null)
        {
            rd.setAutoConfig(autoConfigField);
        }
        rd.setPropagate(propagate);
        return rd;
    }
}
