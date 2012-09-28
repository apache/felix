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
package org.apache.felix.useradmin.impl;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * Provides an event dispatcher for delivering {@link UserAdminEvent}s asynchronously. 
 */
public final class EventDispatcher implements Runnable {
    
    private static final String TOPIC_BASE = "org/osgi/service/useradmin/UserAdmin/";

    private final EventAdmin m_eventAdmin;
    private final UserAdminListenerList m_listenerList;
    private final BlockingQueue m_eventQueue;
    private final Thread m_backgroundThread;

    /**
     * Creates a new {@link EventDispatcher} instance, and starts a background thread to deliver all events.
     * 
     * @param eventAdmin the event admin to use, cannot be <code>null</code>;
     * @param listenerList the list with {@link UserAdminListener}s, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case one of the given parameters was <code>null</code>.
     */
    public EventDispatcher(EventAdmin eventAdmin, UserAdminListenerList listenerList) {
        if (eventAdmin == null) {
            throw new IllegalArgumentException("EventAdmin cannot be null!");
        }
        if (listenerList == null) {
            throw new IllegalArgumentException("ListenerList cannot be null!");
        }

        m_eventAdmin = eventAdmin;
        m_listenerList = listenerList;
        m_eventQueue = new LinkedBlockingQueue();

        m_backgroundThread = new Thread(this, "UserAdmin event dispatcher");
    }

    /**
     * Dispatches a given event for asynchronous delivery to all interested listeners, 
     * including those using the {@link EventAdmin} service.
     * <p>
     * This method will perform a best-effort to dispatch the event to all listeners, i.e., 
     * there is no guarantee that the listeners will actually obtain the event, nor any
     * notification is given in case delivery fails.
     * </p>
     * 
     * @param event the event to dispatch, cannot be <code>null</code>.
     * @throws IllegalStateException in case this dispatcher is already stopped.
     */
    public void dispatch(UserAdminEvent event) {
        if (!isRunning()) {
            return;
        }

        try {
            m_eventQueue.put(event);
        } catch (InterruptedException e) {
            // Restore interrupt flag...
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Starts this event dispatcher, allowing it to pick up events and deliver them.
     */
    public void start() {
        if (!isRunning()) {
            m_backgroundThread.start();
        }
    }

    /**
     * Signals this event dispatcher to stop its work and clean up all running threads.
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }

        // Add poison object to queue to let the background thread terminate...
        m_eventQueue.add(EventDispatcher.this);

        try {
            m_backgroundThread.join();
        } catch (InterruptedException e) {
            // We're already stopping; so don't bother... 
        }
    }

    /**
     * Returns whether or not the background thread is running.
     * 
     * @return <code>true</code> if the background thread is running (alive), <code>false</code> otherwise.
     */
    final boolean isRunning() {
        return m_backgroundThread.isAlive();
    }
    
    /**
     * Provides the main event loop, which waits until an event is enqueued in order 
     * to deliver it to any interested listener.
     */
    public void run() {
        try {
            while (true) {
                // Blocks until a event is dispatched...
                Object event = m_eventQueue.take();

                if (event instanceof UserAdminEvent) {
                    // Got a "normal" user admin event; lets dispatch it further...
                    deliverEventSynchronously((UserAdminEvent) event);
                } else {
                    // Got a "poison" object; this means we must stop running...
                    return;
                }
            }
        } catch (InterruptedException e) {
            // Restore interrupt flag, and terminate thread...
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Converts a given {@link UserAdminEvent} to a {@link Event} that can be
     * dispatched through the {@link EventAdmin} service.
     * 
     * @param event
     *            the event to convert, cannot be <code>null</code>.
     * @return a new {@link Event} instance containing the same set of
     *         information as the given event, never <code>null</code>.
     */
    private Event convertEvent(UserAdminEvent event) {
        String topic = getTopicName(event.getType());
        Role role = event.getRole();
        ServiceReference serviceRef = event.getServiceReference();

        Properties props = new Properties();
        props.put(EventConstants.EVENT_TOPIC, TOPIC_BASE.concat(topic));
        props.put(EventConstants.EVENT, event);
        props.put("role", role);
        props.put("role.name", role.getName());
        props.put("role.type", new Integer(role.getType()));
        if (serviceRef != null) {
            props.put(EventConstants.SERVICE, serviceRef);
            Object property;
            
            property = serviceRef.getProperty(Constants.SERVICE_ID);
            if (property != null) {
                props.put(EventConstants.SERVICE_ID, property);
            }
            property = serviceRef.getProperty(Constants.OBJECTCLASS);
            if (property != null) {
                props.put(EventConstants.SERVICE_OBJECTCLASS, property);
            }
            property = serviceRef.getProperty(Constants.SERVICE_PID);
            if (property != null) {
                props.put(EventConstants.SERVICE_PID, property);
            }
        }

        return new Event(topic, props);
    }

    /**
     * Delivers the given event synchronously to all interested listeners.
     * 
     * @param event the event to deliver, cannot be <code>null</code>.
     */
    private void deliverEventSynchronously(UserAdminEvent event) {
        // Asynchronously deliver an event to the EventAdmin service...
        m_eventAdmin.postEvent(convertEvent(event));

        // Synchronously call all UserAdminListeners to deliver the event...
        UserAdminListener[] listeners = m_listenerList.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].roleChanged(event);
        }
    }
    
    /**
     * Converts a topic name for the given event-type.
     * 
     * @param type the type of event to get the topic name for.
     * @return a topic name, never <code>null</code>.
     */
    private String getTopicName(int type) {
        switch (type) {
            case UserAdminEvent.ROLE_CREATED:
                return "ROLE_CREATED";
            case UserAdminEvent.ROLE_CHANGED:
                return "ROLE_CHANGED";
            case UserAdminEvent.ROLE_REMOVED:
                return "ROLE_REMOVED";
            default:
                return null;
        }
    }
}
