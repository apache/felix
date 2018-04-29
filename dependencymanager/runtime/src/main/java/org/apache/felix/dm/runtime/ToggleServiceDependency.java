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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyActivation;
import org.apache.felix.dm.DependencyService;

/**
 * This is a custom DependencyManager Dependency, allowing to take control of when the dependency
 * is available or not. It's used in the context of the LifecycleController class, in order to 
 * activate/deactivate a Component on demand.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ToggleServiceDependency implements Dependency, DependencyActivation
{
    private final List<Object> m_services = new ArrayList<Object>();
    private volatile boolean m_isAvailable;
    private volatile boolean m_stopped;

    public ToggleServiceDependency()
    {
    }

    public ToggleServiceDependency(boolean isAvailable)
    {
        m_isAvailable = isAvailable;
    }

    public void setAvailable(boolean isAvailable)
    {
        synchronized (this)
        {
            if (m_stopped)
            {
                return;
            }
            boolean changed = m_isAvailable != isAvailable;
            m_isAvailable = isAvailable;
            
            if (! changed) {
                return;
            }
        }
        
        // invoked on every change
        if (m_isAvailable)
        {
            Object[] services = m_services.toArray();
            for (int i = 0; i < services.length; i++)
            {
                DependencyService ds = (DependencyService) services[i];
                ds.dependencyAvailable(this);
                if (!isRequired())
                {
                    invokeAdded(ds);
                }
            }
        }
        else
        {
            Object[] services = m_services.toArray();
            for (int i = 0; i < services.length; i++)
            {
                DependencyService ds = (DependencyService) services[i];
                ds.dependencyUnavailable(this);
                if (!isRequired())
                {
                    invokeRemoved(ds);
                }
            }
        }
    }

    public Dependency createCopy()
    {
        return new ToggleServiceDependency(m_isAvailable);
    }

    public Object getAutoConfigInstance()
    {
        return "" + m_isAvailable;
    }

    public String getAutoConfigName()
    {
        return null;
    }

    public Class<?> getAutoConfigType()
    {
        return String.class;
    }

    @SuppressWarnings("unchecked")
    public Dictionary getProperties()
    {
        return null;
    }

    public void invokeAdded(DependencyService service)
    {
    }

    public void invokeRemoved(DependencyService service)
    {
    }

    public void invoke(DependencyService dependencyService, String name)
    {
    }

    public boolean isAutoConfig()
    {
        return false;
    }

    public boolean isAvailable()
    {
        return m_isAvailable;
    }

    public boolean isInstanceBound()
    {
        return true;
    }

    public boolean isPropagated()
    {
        return false;
    }

    public boolean isRequired()
    {
        return true;
    }

    public void start(DependencyService service)
    {
        synchronized (this)
        {
            m_services.add(service);
            m_stopped = false;
        }
    }

    public void stop(DependencyService service)
    {
        synchronized (this)
        {
            m_services.remove(service);
            m_stopped = true;
        }
    }
}
