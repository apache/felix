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


import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.cometd.CometdService;
import org.cometd.bayeux.server.BayeuxServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class Activator
    implements BundleActivator, ServiceTrackerCustomizer
{
    private BundleContext context;
    private ServiceTracker cometdServiceTracker;
    private ServiceTracker httpServiceTracker;
    private ServiceRegistration reg;
    
    public void start(BundleContext context)
        throws Exception
    {
        this.context = context;
        this.cometdServiceTracker = new ServiceTracker(this.context, CometdService.class.getName(), this);
        this.cometdServiceTracker.open();
        this.httpServiceTracker = new ServiceTracker(this.context, HttpService.class.getName(), new HTTPServiceCustomizer());
        this.httpServiceTracker.open();
    }

    public void stop(BundleContext context)
        throws Exception
    {
        if (this.cometdServiceTracker != null) {
            this.cometdServiceTracker.close();
        }
        if (this.httpServiceTracker != null) {
        	this.httpServiceTracker.close();
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
            BayeuxServer bayeuxServer = cometdService.getBayeuxServer();
            if (bayeuxServer != null) {
                Hashtable<String, String> props = new Hashtable<String, String>();
                props.put("alias", "/system/time");
                // whiteboard servlet registration
                this.reg = context.registerService(Servlet.class.getName(), new TimeServlet(bayeuxServer), props);
                doLog("Connect a browser to http://<host>:<port>/system/time to view the time.");
            }
            else {
                doLog("Failed to get bayeux server");
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
    
    class HTTPServiceCustomizer implements ServiceTrackerCustomizer {

		public Object addingService(ServiceReference reference) {
			HttpService httpService = (HttpService) context.getService(reference);
			try {
				httpService.registerResources("/js", "/src-web", null);
			} catch (NamespaceException e) {
				e.printStackTrace();
			}
			return null;
		}

		public void modifiedService(ServiceReference reference, Object service) {
			
		}

		public void removedService(ServiceReference reference, Object service) {
			HttpService httpService = (HttpService) context.getService(reference);
			httpService.unregister("/js");
		}
    	
    }
}
