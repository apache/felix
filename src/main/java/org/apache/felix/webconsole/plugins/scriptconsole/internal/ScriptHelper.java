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

package org.apache.felix.webconsole.plugins.scriptconsole.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.lang.reflect.Array;
import java.util.*;

class ScriptHelper
{
    /** The bundle context. */
    private final BundleContext bundleContext;

    /**
     * The list of references - we don't need to synchronize this as we are
     * running in one single request.
     */
    private List<ServiceReference> references;

    /** A map of found services. */
    private Map<String, Object> services;

    public ScriptHelper(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type)
    {
        ServiceType service = (this.services == null ? null
            : (ServiceType) this.services.get(type.getName()));
        if (service == null)
        {
            final ServiceReference ref = this.bundleContext.getServiceReference(type.getName());
            if (ref != null)
            {
                service = (ServiceType) this.bundleContext.getService(ref);
                if (service != null)
                {
                    if (this.services == null)
                    {
                        this.services = new HashMap<String, Object>();
                    }
                    if (this.references == null)
                    {
                        this.references = new ArrayList<ServiceReference>();
                    }
                    this.references.add(ref);
                    this.services.put(type.getName(), service);
                }
            }
        }
        return service;
    }

    public <ServiceType> ServiceType[] getServices(Class<ServiceType> serviceType,
        String filter) throws InvalidSyntaxException
    {
        final ServiceReference[] refs = this.bundleContext.getServiceReferences(
            serviceType.getName(), filter);
        ServiceType[] result = null;
        if (refs != null)
        {
            final List<ServiceType> objects = new ArrayList<ServiceType>();
            for (int i = 0; i < refs.length; i++)
            {
                @SuppressWarnings("unchecked")
                final ServiceType service = (ServiceType) this.bundleContext.getService(refs[i]);
                if (service != null)
                {
                    if (this.references == null)
                    {
                        this.references = new ArrayList<ServiceReference>();
                    }
                    this.references.add(refs[i]);
                    objects.add(service);
                }
            }
            if (objects.size() > 0)
            {
                @SuppressWarnings("unchecked")
                ServiceType[] srv = (ServiceType[]) Array.newInstance(serviceType,
                    objects.size());
                result = objects.toArray(srv);
            }
        }
        return result;
    }

    /**
     * Clean up this instance.
     */
    public void cleanup()
    {
        if (this.references != null)
        {
            final Iterator<ServiceReference> i = this.references.iterator();
            while (i.hasNext())
            {
                final ServiceReference ref = i.next();
                this.bundleContext.ungetService(ref);
            }
            this.references.clear();
        }
        if (this.services != null)
        {
            this.services.clear();
        }
    }
}
