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

import java.util.ArrayList;

import org.apache.felix.http.base.internal.AbstractActivator;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
import org.apache.felix.http.whiteboard.internal.tracker.FilterTracker;
import org.apache.felix.http.whiteboard.internal.tracker.HttpContextTracker;
import org.apache.felix.http.whiteboard.internal.tracker.ListenersTracker;
import org.apache.felix.http.whiteboard.internal.tracker.ServletTracker;
import org.osgi.util.tracker.ServiceTracker;

public final class WhiteboardActivator
    extends AbstractActivator
{
    private final ArrayList<ServiceTracker<?, ?>> trackers = new ArrayList<>();

    @Override
    protected void doStart()
        throws Exception
    {
        final ExtenderManager manager = new ExtenderManager();
        addTracker(new HttpContextTracker(getBundleContext(), manager));
        addTracker(new FilterTracker(getBundleContext(), manager));
        addTracker(new ServletTracker(getBundleContext(), manager));
        addTracker(new ListenersTracker(getBundleContext(), manager));

        SystemLogger.info("Apachde Felix Http Whiteboard Service started");
    }

    private void addTracker(ServiceTracker<?,?> tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    @Override
    protected void doStop()
        throws Exception
    {
        for (ServiceTracker<?,?> tracker : this.trackers) {
            tracker.close();
        }

        this.trackers.clear();

        SystemLogger.info("Apachde Felix Http Whiteboard Service stopped");
    }
}
