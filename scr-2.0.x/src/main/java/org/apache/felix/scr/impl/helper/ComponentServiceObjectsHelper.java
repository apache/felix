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
import java.util.Collection;
import java.util.List;
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

    private final ConcurrentMap<ServiceReference<?>, ComponentServiceObjectsImpl> services = new ConcurrentHashMap<ServiceReference<?>, ComponentServiceObjectsImpl>();

    private final List<ComponentServiceObjectsImpl> closedServices = new ArrayList<ComponentServiceObjectsImpl>();

    private final ConcurrentMap<ServiceReference, Object> prototypeInstances = new ConcurrentHashMap<ServiceReference, Object>();

    public ComponentServiceObjectsHelper(final BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    public void cleanup()
    {
    	Collection<ComponentServiceObjectsImpl> csos = services.values();
        services.clear();
        for(final ComponentServiceObjectsImpl cso : csos)
        {
        	cso.deactivate();
        }
        synchronized ( this.closedServices )
        {
        	csos = new ArrayList<ComponentServiceObjectsImpl>(this.closedServices);
        	this.closedServices.clear();
        }
        for(final ComponentServiceObjectsImpl cso : csos)
        {
        	cso.deactivate();
        }
        prototypeInstances.clear();
    }

    public ComponentServiceObjects getServiceObjects(final ServiceReference<?> ref)
    {
        ComponentServiceObjectsImpl cso = this.services.get(ref);
        if ( cso == null )
        {
            final ServiceObjects serviceObjects = this.bundleContext.getServiceObjects(ref);
            if ( serviceObjects != null )
            {
                cso = new ComponentServiceObjectsImpl(serviceObjects);
                final ComponentServiceObjectsImpl oldCSO = this.services.putIfAbsent(ref, cso);
                if ( oldCSO != null )
                {
                	cso = oldCSO;
                }
            }
        }
        return cso;
    }

    public void closeServiceObjects(final ServiceReference<?> ref) {
        ComponentServiceObjectsImpl cso = this.services.remove(ref);
        if ( cso != null )
        {
        	synchronized ( closedServices )
        	{
        		closedServices.add(cso);
        	}
            cso.close();
        }
        prototypeInstances.remove(ref);
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

        private volatile boolean deactivated = false;

        public ComponentServiceObjectsImpl(final ServiceObjects so)
        {
            this.serviceObjects = so;
        }

        public void deactivate()
        {
        	this.deactivated = true;
        	close();
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
            	final List<Object> localInstances = new ArrayList<Object>();
                synchronized ( this.instances )
                {
                	localInstances.addAll(this.instances);
                	this.instances.clear();
                }
                for(final Object obj : localInstances)
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
            }
        }

        public Object getService()
        {
        	if ( this.deactivated )
        	{
        		throw new IllegalStateException();
        	}
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
        	if ( this.deactivated )
        	{
        		throw new IllegalStateException();
        	}
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

		@Override
		public String toString() {
			return "ComponentServiceObjectsImpl [instances=" + instances + ", serviceObjects=" + serviceObjects
					+ ", deactivated=" + deactivated + ", hashCode=" + this.hashCode() + "]";
		}

    }
 }
