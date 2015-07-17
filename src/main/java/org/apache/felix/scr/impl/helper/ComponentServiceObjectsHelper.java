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
package org.apache.felix.scr.impl.helper;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;


/**
 * Utility class for handling references using a ComponentServiceObjects
 * to get services.
 */
public class ComponentServiceObjectsHelper
{
    private final BundleContext bundleContext;

    private final Map<ServiceReference<?>, ComponentServiceObjectsImpl> services = new HashMap<ServiceReference<?>, ComponentServiceObjectsImpl>();

    private final ConcurrentMap<ServiceReference, Object> prototypeInstances = new ConcurrentHashMap<ServiceReference, Object>();

    public ComponentServiceObjectsHelper(final BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void cleanup()
    {
        synchronized ( this )
        {
            for(final Map.Entry<ServiceReference<?>, ComponentServiceObjectsImpl> entry : services.entrySet())
            {
                entry.getValue().close();
            }
            services.clear();
        }
        prototypeInstances.clear();
    }

    public ComponentServiceObjects getServiceObjects(final ServiceReference<?> ref)
    {
        synchronized ( this )
        {
            ComponentServiceObjectsImpl cso = this.services.get(ref);
            if ( cso == null )
            {
                final ServiceObjects serviceObjects = this.bundleContext.getServiceObjects(ref);
                if ( serviceObjects != null )
                {
                    cso = new ComponentServiceObjectsImpl(serviceObjects);
                    this.services.put(ref, cso);
                }
            }
            return cso;
        }
    }

    public void closeServiceObjects(final ServiceReference<?> ref) {
        ComponentServiceObjectsImpl cso;
        synchronized ( this )
        {
            cso = this.services.remove(ref);
        }
        if ( cso != null )
        {
            cso.close();
        }
    }

    public <T> T getPrototypeRefInstance(final ServiceReference<T> ref, ServiceObjects<T> serviceObjects)
    {
    	T service = (T) prototypeInstances.get(ref);
    	if ( service == null )
    	{
    		service = serviceObjects.getService();
    		T oldService = (T)prototypeInstances.putIfAbsent(ref, service);
    		if ( oldService != null )
    		{
    			// another thread created the instance already
    			serviceObjects.ungetService(service);
    			service = oldService;
    		}
    	}
    	return service;
    }

    private static final class ComponentServiceObjectsImpl implements ComponentServiceObjects
    {
        private final List<Object> instances = new ArrayList<Object>();

        private volatile ServiceObjects serviceObjects;

        public ComponentServiceObjectsImpl(final ServiceObjects so)
        {
            this.serviceObjects = so;
        }

        /**
         * Close this instance and unget all services.
         */
        public void close()
        {
            final ServiceObjects so = this.serviceObjects;
            this.serviceObjects = null;
            if ( so != null )
            {
                synchronized ( this.instances )
                {
                    for(final Object obj : instances)
                    {
                        try
                        {
                            so.ungetService(obj);
                        }
                        catch ( final IllegalStateException ise )
                        {
                            // ignore (this happens if the bundle is not valid anymore)
                        }
                        catch ( final IllegalArgumentException iae )
                        {
                            // ignore (this happens if the service has not been returned by the service objects)
                        }
                    }
                    this.instances.clear();
                }
            }
        }

        public Object getService()
        {
            final ServiceObjects so = this.serviceObjects;
            Object service = null;
            if ( so != null )
            {
                service = so.getService();
                if ( service != null )
                {
                    synchronized ( this.instances )
                    {
                        this.instances.add(service);
                    }
                }
            }
            return service;
        }

        public void ungetService(final Object service)
        {
            final ServiceObjects so = this.serviceObjects;
            if ( so != null )
            {
                boolean remove;
                synchronized ( instances )
                {
                    remove = instances.remove(service);
                }
                if ( remove ) {
                    so.ungetService(service);
                }
            }
        }

        public ServiceReference<?> getServiceReference()
        {
            final ServiceObjects so = this.serviceObjects;
            if ( so != null )
            {
                return so.getServiceReference();
            }
            return null;
        }
    }
 }
