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
package org.apache.felix.http.samples.cometd;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Hashtable;
import org.apache.felix.http.api.CometdService;
import org.cometd.Bayeux;

public final class Activator
    implements BundleActivator, ServiceTrackerCustomizer
{
    private BundleContext context;
    private ServiceTracker cometdServiceTracker;
    private ServiceRegistration reg;
    
    public void start(BundleContext context)
        throws Exception
    {
        this.context = context;
        this.cometdServiceTracker = new ServiceTracker(this.context, CometdService.class.getName(), this);
        this.cometdServiceTracker.open();
    }

    public void stop(BundleContext context)
        throws Exception
    {
        if (this.cometdServiceTracker != null) {
            this.cometdServiceTracker.close();
        }
        this.unregister();
    }

    public Object addingService(ServiceReference reference)
    {
        Object service = this.context.getService(reference);
        this.register((CometdService)service);
        return service;
    }

    public void modifiedService(ServiceReference reference, Object service)
    {
        this.unregister();
        this.register((CometdService)service);
    }

    public void removedService(ServiceReference reference, Object service)
    {
        this.unregister();
    }

    private void register(CometdService cometdService) {
        try {
            Bayeux bayeux = cometdService.getBayeux();
            if (bayeux != null) {
                Hashtable<String, String> props = new Hashtable<String, String>();
                props.put("alias", "/system/time");
                this.reg = context.registerService(Servlet.class.getName(), new TimeServlet(bayeux), props);
                doLog("Connect a browser to http://<host>:<port>/system/time to view the time.");
            }
            else {
                doLog("Failed to get bayeux");
            }
        }
        catch (ServletException e) {
            doLog("Failed to register TimeServlet at /system/time: " + e.getMessage());
        }
    }

    private void unregister() {
        if (this.reg != null) {
            this.reg.unregister();
            this.reg = null;
        }
    }

    private void doLog(String message)
    {
        System.out.println("## Activator:   " + message);
    }
}
