/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.cometd.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.http.HttpServlet;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.cometd.CometdService;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.server.CometDServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class CometdServiceImpl
    extends HttpServlet
    implements ManagedService, ServiceTrackerCustomizer, CometdService
{
    private static final long serialVersionUID = 1L;
    private static final String PID = "org.apache.felix.http.cometd";

    private final CometdConfig config;
    private final BundleContext context;
    private ServiceRegistration configServiceReg;
    private ServiceTracker httpServiceTracker;
    private ServiceRegistration cometdServiceReg;
    private CometDServlet continuationCometdServlet;

    public CometdServiceImpl(BundleContext context)
    {
        this.context = context;
        this.config = new CometdConfig(this.context);
    }

    public void start()
        throws Exception
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, PID);
        this.configServiceReg = this.context.registerService(ManagedService.class.getName(), this, props);

        this.httpServiceTracker = new ServiceTracker(this.context, HttpService.class.getName(), this);
        this.httpServiceTracker.open();
    }

    public void stop()
        throws Exception
    {
        if (this.configServiceReg != null) {
            this.configServiceReg.unregister();
        }

        if (this.httpServiceTracker != null) {
            this.httpServiceTracker.close();
        }
    }

    @Override
    public void updated(Dictionary props)
    {
        this.config.update(props);
        if (this.httpServiceTracker != null) {
            Object service = this.httpServiceTracker.getService();
            if (service != null) {
                this.unregister((HttpService)service);
                this.register((HttpService)service);
            }
        }
    }

    @Override
    public Object addingService(ServiceReference reference)
    {
        Object service = this.context.getService(reference);
        this.register((HttpService)service);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service)
    {
        this.unregister((HttpService)service);
        this.register((HttpService)service);
    }

    @Override
    public void removedService(ServiceReference reference, Object service)
    {
        this.unregister((HttpService)service);
    }

    private void register(HttpService httpService) {
        if (this.continuationCometdServlet == null) {
            this.continuationCometdServlet = new CometDServlet();
        }
        try {
          Dictionary dictionary = new Hashtable();
          dictionary.put("requestAvailable","true");
          httpService.registerServlet(this.config.getPath(), this.continuationCometdServlet, dictionary, null);
        }
        catch (Exception e) {
          SystemLogger.error("Failed to register ContinuationCometdServlet to " + this.config.getPath(), e);
        }
        this.cometdServiceReg =
            this.context.registerService(CometdService.class.getName(), this, null);
    }

    private void unregister(HttpService httpService) {
        httpService.unregister(this.config.getPath());
        if (this.cometdServiceReg != null) {
            this.cometdServiceReg.unregister();
            this.cometdServiceReg = null;
        }
    }

    @Override
    public BayeuxServer getBayeuxServer() {
        return this.continuationCometdServlet.getBayeux();
    }
}
