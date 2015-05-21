/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.sslfilter.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HttpServiceTracker extends ServiceTracker
{
    /** Singleton filter to be registered with all http services. */
    private final SslFilter filter = new SslFilter();

    private volatile ServiceRegistration configReceiver;

    @SuppressWarnings("serial")
    public HttpServiceTracker(final BundleContext context)
    {
        super(context, ExtHttpService.class.getName(), null);
    }

    @Override
    public void open(final boolean trackAllServices)
    {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, SslFilter.PID);

        this.configReceiver = super.context.registerService(ManagedService.class.getName(), new ServiceFactory()
        {
            @Override
            public Object getService(Bundle bundle, ServiceRegistration registration)
            {
                return new ManagedService()
                {
                    @Override
                    public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException
                    {
                        configureFilters(properties);
                    }
                };
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
            {
                // Nop
            }
        }, props);

        super.open(trackAllServices);
    }

    @Override
    public void close()
    {
        super.close();

        if (this.configReceiver != null)
        {
            this.configReceiver.unregister();
            this.configReceiver = null;
        }
    }

    @Override
    public Object addingService(final ServiceReference reference)
    {
        final ExtHttpService service = (ExtHttpService) super.addingService(reference);
        if (service != null)
        {
            try
            {
                service.registerFilter(filter, ".*", new Hashtable(), 0, null);

                SystemLogger.log(LogService.LOG_DEBUG, "SSL filter registered...");
            }
            catch (ServletException e)
            {
                SystemLogger.log(LogService.LOG_WARNING, "Failed to register SSL filter!", e);
            }
        }

        return service;
    }

    @Override
    public void removedService(final ServiceReference reference, final Object service)
    {
        ((ExtHttpService) service).unregisterFilter(filter);

        SystemLogger.log(LogService.LOG_DEBUG, "SSL filter unregistered...");

        super.removedService(reference, service);
    }

    void configureFilters(@SuppressWarnings("rawtypes") final Dictionary properties) throws ConfigurationException
    {
        this.filter.configure(properties);
    }
}
