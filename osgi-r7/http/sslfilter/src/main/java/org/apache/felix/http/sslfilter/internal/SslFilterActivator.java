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

import javax.servlet.Filter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

public class SslFilterActivator implements BundleActivator
{
    /** Singleton filter to be registered with all http services. */
    private final SslFilter filter = new SslFilter();

    private volatile ServiceRegistration configReceiver;

    private volatile ServiceRegistration filterReg;

    private LogServiceTracker logTracker;

    @Override
    public void start(final BundleContext context)
    {
        this.logTracker = new LogServiceTracker(context);
        this.logTracker.open();

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, SslFilter.PID);

        this.configReceiver = context.registerService(ManagedService.class.getName(), new ServiceFactory()
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

        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Felix HTTP SSL Filter");

        // any context
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)");
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/");

        this.filterReg = context.registerService(Filter.class.getName(), filter, properties);

        SystemLogger.log(LogService.LOG_DEBUG, "SSL filter registered...");
    }

    @Override
    public void stop(final BundleContext context)
    {
        if ( this.filterReg != null )
        {
            this.filterReg.unregister();
            this.filterReg = null;
            SystemLogger.log(LogService.LOG_DEBUG, "SSL filter unregistered...");
        }
        if (this.configReceiver != null)
        {
            this.configReceiver.unregister();
            this.configReceiver = null;
        }
        if (this.logTracker != null)
        {
            this.logTracker.close();
            this.logTracker = null;
        }
    }

    void configureFilters(@SuppressWarnings("rawtypes") final Dictionary properties) throws ConfigurationException
    {
        this.filter.configure(properties);
    }
}
