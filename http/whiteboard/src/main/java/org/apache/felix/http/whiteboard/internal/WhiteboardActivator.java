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
package org.apache.felix.http.whiteboard.internal;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.apache.felix.http.whiteboard.internal.tracker.FilterTracker;
import org.apache.felix.http.whiteboard.internal.tracker.HttpContextTracker;
import org.apache.felix.http.whiteboard.internal.tracker.ServletTracker;
import org.apache.felix.http.whiteboard.internal.tracker.HttpServiceTracker;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
import org.apache.felix.http.whiteboard.internal.manager.HttpWhiteboardWebConsolePlugin;
import org.apache.felix.http.base.internal.AbstractActivator;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import java.util.ArrayList;
import java.util.Hashtable;

public final class WhiteboardActivator
    extends AbstractActivator
{
    private final ArrayList<ServiceTracker> trackers;
    private ExtenderManager manager;
    private ServiceRegistration httpPlugin;

    public WhiteboardActivator()
    {
        this.trackers = new ArrayList<ServiceTracker>();
    }

    protected void doStart()
        throws Exception
    {
        this.manager = new ExtenderManager();
        addTracker(new HttpContextTracker(getBundleContext(), this.manager));
        addTracker(new FilterTracker(getBundleContext(), this.manager));
        addTracker(new ServletTracker(getBundleContext(), this.manager));
        addTracker(new HttpServiceTracker(getBundleContext(), this.manager));

        HttpWhiteboardWebConsolePlugin plugin = new HttpWhiteboardWebConsolePlugin(this.manager);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", plugin.getLabel());
        props.put("felix.webconsole.title", plugin.getTitle());
        props.put("felix.webconsole.configprinter.modes", new String[]
            { "txt", "zip" });
        props.put(Constants.SERVICE_DESCRIPTION, "Felix Http Whiteboard WebConsole Plugin and Configuration Printer");
        httpPlugin = getBundleContext().registerService("javax.servlet.Servlet", plugin, props);

        SystemLogger.info("Http service whiteboard started");
    }

    private void addTracker(ServiceTracker tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    protected void doStop()
        throws Exception
    {
        this.httpPlugin.unregister();

        for (ServiceTracker tracker : this.trackers) {
            tracker.close();
        }

        this.trackers.clear();
        this.manager.unregisterAll();
    }
}
