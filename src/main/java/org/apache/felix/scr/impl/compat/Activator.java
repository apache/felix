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
package org.apache.felix.scr.impl.compat;


import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.ScrInfo;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class Activator
{
    // tracker of the runtime service
    private volatile ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> runtimeTracker;

    // the service registrations
    private final Map<Long, ServiceRegistration<ScrService>> scrServiceRegMap = new ConcurrentHashMap<Long, ServiceRegistration<ScrService>>();

    // the service registrations
    private final Map<Long, ServiceRegistration<ScrInfo>> scrCommandRegMap = new ConcurrentHashMap<Long, ServiceRegistration<ScrInfo>>();

    public void start( final BundleContext context ) throws Exception
    {
        this.runtimeTracker = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(context, ServiceComponentRuntime.class,
                new ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceComponentRuntime>()
                {

                    public ServiceComponentRuntime addingService(
                            final ServiceReference<ServiceComponentRuntime> reference)
                    {
                        final ServiceComponentRuntime runtime = context.getService(reference);
                        if ( runtime != null )
                        {
                            Hashtable props = new Hashtable();
                            props.put(Constants.SERVICE_DESCRIPTION, "Apache Felix Compat ScrService");
                            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

                            final ScrService service = new ScrServiceImpl(context, runtime);
                            scrServiceRegMap.put((Long)reference.getProperty(Constants.SERVICE_ID),
                                    context.registerService(ScrService.class, service, props));

                            props = new Hashtable();
                            props.put(Constants.SERVICE_DESCRIPTION, "Apache Felix Compat SCR Info service");
                            props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
                            final ScrInfo info = new ScrCommand(context, service);
                            scrCommandRegMap.put((Long)reference.getProperty(Constants.SERVICE_ID),
                                    context.registerService(ScrInfo.class, info, props));
                        }
                        return runtime;
                    }

                    public void modifiedService(
                            final ServiceReference<ServiceComponentRuntime> reference,
                            final ServiceComponentRuntime service) {
                        // nothing to do
                    }

                    public void removedService(
                            final ServiceReference<ServiceComponentRuntime> reference,
                            final ServiceComponentRuntime service) {
                        final ServiceRegistration<ScrService> reg =  scrServiceRegMap.remove(reference.getProperty(Constants.SERVICE_ID));
                        if ( reg != null )
                        {
                            reg.unregister();
                        }
                        final ServiceRegistration<ScrInfo> reg2 =  scrCommandRegMap.remove(reference.getProperty(Constants.SERVICE_ID));
                        if ( reg2 != null )
                        {
                            reg2.unregister();
                        }
                        context.ungetService(reference);
                    }
        });
        this.runtimeTracker.open();
    }

    public void stop(final BundleContext context) throws Exception
    {
        this.runtimeTracker.close();
    }
}