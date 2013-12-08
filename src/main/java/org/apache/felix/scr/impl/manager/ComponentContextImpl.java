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
package org.apache.felix.scr.impl.manager;


import java.util.Arrays;
import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.component.ExtComponentContext;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * Implementation for the ComponentContext interface
 *
 */
public class ComponentContextImpl<S> implements ExtComponentContext {

    private final SingleComponentManager<S> m_componentManager;
    
    private final EdgeInfo[] edgeInfos;
    
    private final ComponentInstance m_componentInstance = new ComponentInstanceImpl(this);
    
    private final Bundle m_usingBundle;
    
    private final S m_implementationObject;
    
    private volatile boolean m_implementationAccessible;
    
    private final CountDownLatch accessibleLatch = new CountDownLatch(1);

    ComponentContextImpl( SingleComponentManager<S> componentManager, Bundle usingBundle, S implementationObject )
    {
        m_componentManager = componentManager;
        m_usingBundle = usingBundle;
        m_implementationObject = implementationObject;
        edgeInfos = new EdgeInfo[componentManager.getComponentMetadata().getDependencies().size()];
        for (int i = 0; i< edgeInfos.length; i++)
        {
            edgeInfos[i] = new EdgeInfo();
        }
    }
    
    void setImplementationAccessible(boolean implementationAccessible)
    {
        this.m_implementationAccessible = implementationAccessible;
        if (implementationAccessible)
        {
            accessibleLatch.countDown();
        }
    }
    
    EdgeInfo getEdgeInfo(DependencyManager<S, ?> dm)
    {
        int index = dm.getIndex();
        return edgeInfos[index];
    }

    protected SingleComponentManager<S> getComponentManager()
    {
        return m_componentManager;
    }

    public final Dictionary<String, Object> getProperties()
    {
        // 112.12.3.5 The Dictionary is read-only and cannot be modified
        Dictionary<String, Object> ctxProperties = m_componentManager.getProperties();
        return new ReadOnlyDictionary<String, Object>( ctxProperties );
    }


    public Object locateService( String name )
    {
        m_componentManager.reconfigureReadLock();
        try
        {
            DependencyManager<S, ?> dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getService() : null;
        }
        finally
        {
            m_componentManager.reconfigureReadUnlock();
        }
    }


    public Object locateService( String name, ServiceReference ref )
    {
        m_componentManager.reconfigureReadLock();
        try
        {
            DependencyManager<S, ?> dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getService( ref ) : null;
        }
        finally
        {
            m_componentManager.reconfigureReadUnlock();
        }
    }


    public Object[] locateServices( String name )
    {
        m_componentManager.reconfigureReadLock();
        try
        {
            DependencyManager dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getServices() : null;
        }
        finally
        {
            m_componentManager.reconfigureReadUnlock();
        }
    }


    public BundleContext getBundleContext()
    {
        return m_componentManager.getBundleContext();
    }


    public Bundle getUsingBundle()
    {
        return m_usingBundle;
    }


    public ComponentInstance getComponentInstance()
    {
        return m_componentInstance;
    }


    public void enableComponent( String name )
    {
        BundleComponentActivator activator = m_componentManager.getActivator();
        if ( activator != null )
        {
            activator.enableComponent( name );
        }
    }


    public void disableComponent( String name )
    {
        BundleComponentActivator activator = m_componentManager.getActivator();
        if ( activator != null )
        {
            activator.disableComponent( name );
        }
    }


    public ServiceReference<S> getServiceReference()
    {
        return m_componentManager.getServiceReference();
    }


    //---------- Speculative MutableProperties interface ------------------------------

    public void setServiceProperties(Dictionary properties)
    {
        getComponentManager().setServiceProperties(properties );
    }
    
    //---------- ComponentInstance interface support ------------------------------

    S getImplementationObject( boolean requireAccessible )
    {
        if ( !requireAccessible || m_implementationAccessible )
        {
            return m_implementationObject;
        }
        try
        {
            if (accessibleLatch.await( m_componentManager.getLockTimeout(), TimeUnit.MILLISECONDS ) && m_implementationAccessible)
            {
                return m_implementationObject;
            }
        }
        catch ( InterruptedException e )
        {
            try
            {
                if (accessibleLatch.await( m_componentManager.getLockTimeout(), TimeUnit.MILLISECONDS ) && m_implementationAccessible)
                {
                    return m_implementationObject;
                }
            }
            catch ( InterruptedException e1 )
            {
                m_componentManager.log( LogService.LOG_INFO, "Interrupted twice waiting for implementation object to become accessible", e1 );
            }
            Thread.currentThread().interrupt();
            return null;
        }
        return null;
    }
    
    private static class ComponentInstanceImpl implements ComponentInstance
    {
        private final ComponentContextImpl m_componentContext;

        private ComponentInstanceImpl(ComponentContextImpl m_componentContext)
        {
            this.m_componentContext = m_componentContext;
        }


        public Object getInstance()
        {
            return m_componentContext.getImplementationObject(true);
        }


        public void dispose()
        {
            m_componentContext.getComponentManager().dispose();
        }

    }
}
