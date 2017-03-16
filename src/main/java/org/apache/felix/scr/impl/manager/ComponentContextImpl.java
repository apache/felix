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


import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.component.ExtComponentContext;
import org.apache.felix.scr.impl.helper.ComponentServiceObjectsHelper;
import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * Implementation for the ComponentContext interface
 *
 */
public class ComponentContextImpl<S> implements ExtComponentContext {

    private final SingleComponentManager<S> m_componentManager;

    private final EdgeInfo[] edgeInfos;

    private final ComponentInstance<S> m_componentInstance = new ComponentInstanceImpl<S>(this);

    private final Bundle m_usingBundle;

    private volatile ServiceRegistration<S> m_serviceRegistration;

    private volatile S m_implementationObject;

    private volatile boolean m_implementationAccessible;

    private final CountDownLatch accessibleLatch = new CountDownLatch(1);

    private final ComponentServiceObjectsHelper serviceObjectsHelper;

    /** Mapping of ref pairs to value bound */
    private final Map<String, Map<RefPair<?, ?>, Object>> boundValues = new HashMap<String, Map<RefPair<?,?>,Object>>();



    public ComponentContextImpl( final SingleComponentManager<S> componentManager, final Bundle usingBundle, ServiceRegistration<S> serviceRegistration )
    {
        m_componentManager = componentManager;
        m_usingBundle = usingBundle;
        m_serviceRegistration = serviceRegistration;
        edgeInfos = new EdgeInfo[componentManager.getComponentMetadata().getDependencies().size()];
        for (int i = 0; i< edgeInfos.length; i++)
        {
            edgeInfos[i] = new EdgeInfo();
        }
        this.serviceObjectsHelper = new ComponentServiceObjectsHelper(usingBundle.getBundleContext());
    }

    public void unsetServiceRegistration() {
        m_serviceRegistration = null;
    }

    public void cleanup()
    {
        this.serviceObjectsHelper.cleanup();
    }

    public ComponentServiceObjectsHelper getComponentServiceObjectsHelper()
    {
        return this.serviceObjectsHelper;
    }

    public void setImplementationObject(S implementationObject)
    {
        this.m_implementationObject = implementationObject;
    }


    public void setImplementationAccessible(boolean implementationAccessible)
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

    ServiceRegistration<S> getServiceRegistration()
    {
        return m_serviceRegistration;
    }

    protected SingleComponentManager<S> getComponentManager()
    {
        return m_componentManager;
    }

    public ComponentMetadata getComponentMetadata()
    {
    	return m_componentManager.getComponentMetadata();
    }

    @Override
    public final Dictionary<String, Object> getProperties()
    {
        // 112.12.3.5 The Dictionary is read-only and cannot be modified
        return new ReadOnlyDictionary( m_componentManager.getProperties() );
    }


    @Override
    public Object locateService( String name )
    {
        m_componentManager.obtainActivationReadLock( );
        try
        {
            DependencyManager<S, ?> dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getService(this) : null;
        }
        finally
        {
            m_componentManager.releaseActivationReadLock(  );
        }
    }


    @Override
    public Object locateService( String name, ServiceReference ref )
    {
        m_componentManager.obtainActivationReadLock(  );
        try
        {
            DependencyManager<S, ?> dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getService( this, ref ) : null;
        }
        finally
        {
            m_componentManager.releaseActivationReadLock( );
        }
    }


    @Override
    public Object[] locateServices( String name )
    {
        m_componentManager.obtainActivationReadLock(  );
        try
        {
            DependencyManager<S, ?> dm = m_componentManager.getDependencyManager( name );
            return ( dm != null ) ? dm.getServices(this) : null;
        }
        finally
        {
            m_componentManager.releaseActivationReadLock( );
        }
    }


    @Override
    public BundleContext getBundleContext()
    {
        return m_componentManager.getBundleContext();
    }


    @Override
    public Bundle getUsingBundle()
    {
        return m_usingBundle;
    }

    public SimpleLogger getLogger()
    {
        return m_componentManager;
    }

    @Override
    public ComponentInstance<S> getComponentInstance()
    {
        return m_componentInstance;
    }


    @Override
    public void enableComponent( String name )
    {
        ComponentActivator activator = m_componentManager.getActivator();
        if ( activator != null )
        {
            activator.enableComponent( name );
        }
    }


    @Override
    public void disableComponent( String name )
    {
        ComponentActivator activator = m_componentManager.getActivator();
        if ( activator != null )
        {
            activator.disableComponent( name );
        }
    }


    @Override
    public ServiceReference<S> getServiceReference()
    {
        return m_serviceRegistration == null? null: m_serviceRegistration.getReference();
    }


    //---------- Speculative MutableProperties interface ------------------------------

    @Override
    public void setServiceProperties(Dictionary<String, ?> properties)
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

    private static class ComponentInstanceImpl<S> implements ComponentInstance<S>
    {
        private final ComponentContextImpl<S> m_componentContext;

        private ComponentInstanceImpl(ComponentContextImpl<S> m_componentContext)
        {
            this.m_componentContext = m_componentContext;
        }


        @Override
        public S getInstance()
        {
            return m_componentContext.getImplementationObject(true);
        }


        @Override
        public void dispose()
        {
            m_componentContext.getComponentManager().dispose();
        }

    }

    public Map<RefPair<?, ?>, Object> getBoundValues(final String key)
    {
        Map<RefPair<?, ?>, Object> map = this.boundValues.get(key);
        if ( map == null )
        {
            map = createNewFieldHandlerMap();
            this.boundValues.put(key, map);
        }
        return map;
    }

    private Map<RefPair<?, ?>, Object> createNewFieldHandlerMap()
    {
        return new TreeMap<RefPair<?,?>, Object>(
                new Comparator<RefPair<?, ?>>()
                {

                    @Override
                    public int compare(final RefPair<?, ?> o1, final RefPair<?, ?> o2)
                    {
                        return o1.getRef().compareTo(o2.getRef());
                    }
                });
    }
}
