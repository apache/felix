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
package org.apache.felix.http.base.internal.whiteboard;

import java.util.ArrayList;

import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ResourceTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * TODO - move code to ServletContextHelperManager
 */
public final class ExtenderManager
{
    private final ServletContextHelperManager contextManager;

    private final ArrayList<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    public ExtenderManager(final HttpServiceImpl httpService, final BundleContext bundleContext)
    {
        this.contextManager = new ServletContextHelperManager(bundleContext, httpService);
        addTracker(new FilterTracker(bundleContext, contextManager));
        addTracker(new ServletTracker(bundleContext, this.contextManager));
        addTracker(new ResourceTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextHelperTracker(bundleContext, this.contextManager));
    }

    public void close()
    {
        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();
        this.contextManager.close();
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }
 }
