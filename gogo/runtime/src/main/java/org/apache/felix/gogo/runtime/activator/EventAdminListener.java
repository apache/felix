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
package org.apache.felix.gogo.runtime.activator;

<<<<<<< HEAD
import java.util.Properties;

import org.apache.felix.gogo.api.CommandSessionListener;
import org.apache.felix.service.command.CommandSession;
=======
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.CommandSessionListener;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class EventAdminListener implements CommandSessionListener
{
<<<<<<< HEAD

    private BundleContext bundleContext;
    private ServiceTracker tracker;

    public EventAdminListener(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
        tracker = new ServiceTracker(bundleContext, EventAdmin.class.getName(), null);
        tracker.open();
    }

    public void beforeExecute(CommandSession session, CharSequence command) {
        EventAdmin admin = (EventAdmin) tracker.getService();
        if (admin != null) {
            Properties props = new Properties();
            props.setProperty("command", command.toString());
=======
    private ServiceTracker<EventAdmin, EventAdmin> tracker;

    public EventAdminListener(BundleContext bundleContext)
    {
        tracker = new ServiceTracker<>(bundleContext, EventAdmin.class, null);
        tracker.open();
    }

    public void beforeExecute(CommandSession session, CharSequence command)
    {
        EventAdmin admin = tracker.getService();
        if (admin != null)
        {
            Map<String, Object> props = new HashMap<>();
            props.put("command", command.toString());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            Event event = new Event("org/apache/felix/service/command/EXECUTING", props);
            admin.postEvent(event);
        }
    }

<<<<<<< HEAD
    public void afterExecute(CommandSession session, CharSequence command, Exception exception) {
    }

    public void afterExecute(CommandSession session, CharSequence command, Object result) {
=======
    public void afterExecute(CommandSession session, CharSequence command, Exception exception)
    {
    }

    public void afterExecute(CommandSession session, CharSequence command, Object result)
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
