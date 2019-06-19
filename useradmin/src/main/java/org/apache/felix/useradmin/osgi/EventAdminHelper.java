/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Wraps a {@link ServiceTracker} as an {@link EventAdmin} allowing it to be 
 * used as a "normal" service without having to worry about the actual presence 
 * of the {@link EventAdmin} service itself. 
 */
final class EventAdminHelper extends ServiceTracker implements EventAdmin {

    /**
     * Creates a new {@link EventAdminHelper} instance.
     * 
     * @param context the bundle context to use, cannot be <code>null</code>.
     */
    public EventAdminHelper(BundleContext context) {
        super(context, EventAdmin.class.getName(), null /* customizer */);
    }

    /**
     * {@inheritDoc}
     */
    public void postEvent(Event event) {
        EventAdmin eventAdmin = getEventAdmin();
        if (eventAdmin != null) {
            eventAdmin.postEvent(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void sendEvent(Event event) {
        EventAdmin eventAdmin = getEventAdmin();
        if (eventAdmin != null) {
            eventAdmin.sendEvent(event);
        }
    }

    /**
     * Returns the event admin service, if available.
     * 
     * @return the current event admin service, or <code>null</code> if it is not present.
     */
    private EventAdmin getEventAdmin() {
        return (EventAdmin) getService();
    }
}
