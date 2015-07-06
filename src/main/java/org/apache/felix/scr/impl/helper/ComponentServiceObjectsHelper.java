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

    private final Map<ServiceReference, ServiceObjects> serviceObjectsMap = new HashMap<ServiceReference, ServiceObjects>();

    private final Map<ServiceObjects, List<Object>> services = new HashMap<ServiceObjects, List<Object>>();

    private final ConcurrentMap<ServiceReference, Object> prototypeInstances = new ConcurrentHashMap<ServiceReference, Object>();

    public ComponentServiceObjectsHelper(final BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void cleanup()
    {
        synchronized ( this )
        {
            for(final Map.Entry<ServiceObjects, List<Object>> entry : services.entrySet())
            {
                for(final Object service : entry.getValue())
                {
                    entry.getKey().ungetService(service);
                }
            }
            services.clear();
            serviceObjectsMap.clear();
        }
        prototypeInstances.clear();
    }

    public ComponentServiceObjects getServiceObjects(final ServiceReference<?> ref)
    {
        synchronized ( this )
        {
            ServiceObjects<?> so = this.serviceObjectsMap.get(ref);
            if ( so == null )
            {
                so = this.bundleContext.getServiceObjects(ref);
                if ( so != null )
                {
                    this.serviceObjectsMap.put(ref, so);
                }
            }

            if ( so != null )
            {
                List<Object> services = this.services.get(so);
                if ( services == null )
                {
                    services = new ArrayList<Object>();
                    this.services.put(so, services);
                }
                final ServiceObjects serviceObjects = so;
                final List<Object> serviceList = services;

                return new ComponentServiceObjects() 
                {

                    public Object getService() 
                    {
                        final Object service = serviceObjects.getService();
                        if ( service != null )
                        {
                            synchronized ( serviceList )
                            {
                                serviceList.add(service);
                            }
                        }
                        return service;
                    }

                    public void ungetService(final Object service) 
                    {
                        boolean remove;
                        synchronized ( serviceList )
                        {
                            remove = serviceList.remove(service);
                        }
                        if ( remove ) {
                            serviceObjects.ungetService(service);
                        }
                    }

                    public ServiceReference<?> getServiceReference() 
                    {
                        return ref;
                    }
                };
            }
        }
        return null;
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
   
 }
