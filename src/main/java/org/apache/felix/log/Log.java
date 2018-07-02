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
package org.apache.felix.log;

import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;

/**
 * Class used to represent the log.  This class is used by the implementations
 * of both the {@link org.osgi.service.log.LogService} interface and the
 * {@link org.osgi.service.log.LogReaderService} to access the log.
 * @see org.osgi.service.log.LogService
 * @see org.osgi.service.log.LogReaderService
 */
final class Log implements BundleListener, FrameworkListener, ServiceListener
{
    /** The first log entry. */
    private LogNode m_head;
    /** The last log entry. */
    private LogNode m_tail;
    /** The log size. */
    private int m_size;
    /** The log listener thread. */
    private LogListenerThread listenerThread;
    /** The maximum size for the log. */
    private final int m_maxSize;
    /** Whether or not to store debug messages. */
    private final boolean m_storeDebug;

    /**
     * Create a new instance.
     * @param maxSize the maximum size for the log
     * @param storeDebug whether or not to store debug messages
     */
    Log(final int maxSize, final boolean storeDebug)
    {
        this.m_maxSize = maxSize;
        this.m_storeDebug = storeDebug;
    }

    /**
     * Close the log.
     */
    void close()
    {
        if (listenerThread != null)
        {
            listenerThread.shutdown();
            listenerThread = null;
        }

        m_head = null;
        m_tail = null;
        m_size = 0;
    }

    void log(
        final String name,
        final Bundle bundle,
        final ServiceReference<?> sr,
        final LogLevel level,
        final String message,
        final Throwable exception) {

        addEntry(new LogEntryImpl(name, bundle, sr, level, message, exception, getStackTraceElement()));
    }

    /**
     * Adds the entry to the log.
     * @param entry the entry to add to the log
     */
    synchronized void addEntry(final LogEntry entry)
    {
        if (m_maxSize != 0)
        {
            // add the entry to the historic log
            if (m_storeDebug || entry.getLogLevel() != LogLevel.DEBUG)
            {
                // create a new node for the entry
                LogNode node = new LogNode(entry);

                // add to the front of the linked list
                node.setNextNode(m_head);
                if (m_head != null)
                {
                    m_head.setPreviousNode(node);
                }

                // and store the node
                m_head = node;

                // bump the size of the list
                ++m_size;

                // if no tail node - add the node to the tail
                if (m_tail == null)
                {
                    m_tail = node;
                }
            }

            // ensure the historic log doesn't grow beyond a certain size
            if (m_maxSize != -1)
            {
                if (m_size > m_maxSize)
                {
                    LogNode last = m_tail.getPreviousNode();
                    last.setNextNode(null);
                    m_tail = last;
                    --m_size;
                }
            }
        }

        // notify any listeners
        if (listenerThread != null)
        {
            listenerThread.addEntry(entry);
        }
    }

    /**
     * Add a listener to the log.
     * @param listener the log listener to subscribe
     */
    synchronized void addListener(final LogListener listener)
    {
        if (listenerThread == null)
        {
            // create a new listener thread if necessary:
            // the listener thread only runs if there are any registered listeners
            listenerThread = new LogListenerThread();
            listenerThread.start();
        }
        listenerThread.addListener(listener);
    }

    /**
     * Remove a listener from the log.
     * @param listener the log listener to unsubscribe
     */
    synchronized void removeListener(final LogListener listener)
    {
        if (listenerThread != null)
        {
            listenerThread.removeListener(listener);

            // shutdown the thread if there are no listeners
            if (listenerThread.getListenerCount() == 0)
            {
                listenerThread.shutdown();
                listenerThread = null;
            }
        }
    }

    /**
     * Returns an enumeration of all the entries in the log most recent first.
     * @return an enumeration of all the entries in the log most recent first
     */
    synchronized Enumeration<LogEntry> getEntries()
    {
        return new LogNodeEnumeration(m_head, m_tail);
    }

    /** The messages returned for the framework events. */
    private static final String[] FRAMEWORK_EVENT_MESSAGES =
    {
        "FrameworkEvent STARTED",
        "FrameworkEvent ERROR",
        "FrameworkEvent PACKAGES REFRESHED",
        "FrameworkEvent STARTLEVEL CHANGED",
        "FrameworkEvent WARNING",
        "FrameworkEvent INFO"
    };

    /**
     * Called when a framework event occurs.
     * @param event the event that occured
     */
    public void frameworkEvent(final FrameworkEvent event)
    {
        int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < FRAMEWORK_EVENT_MESSAGES.length; ++i)
        {
            if (eventType >> i == 1)
            {
                message = FRAMEWORK_EVENT_MESSAGES[i];
            }
        }

        log(
            "Events.Framework",
            event.getBundle(),
            null,
            (eventType == FrameworkEvent.ERROR) ? LogLevel.ERROR : LogLevel.INFO,
            message,
            event.getThrowable());
    }

    /** The messages returned for the bundle events. */
    private static final String[] BUNDLE_EVENT_MESSAGES =
    {
        "BundleEvent INSTALLED",
        "BundleEvent STARTED",
        "BundleEvent STOPPED",
        "BundleEvent UPDATED",
        "BundleEvent UNINSTALLED",
        "BundleEvent RESOLVED",
        "BundleEvent UNRESOLVED"
    };

    /**
     * Called when a bundle event occurs.
     * @param event the event that occured
     */
    public void bundleChanged(final BundleEvent event)
    {
        int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < BUNDLE_EVENT_MESSAGES.length; ++i)
        {
            if (eventType >> i == 1)
            {
                message = BUNDLE_EVENT_MESSAGES[i];
            }
        }

        if (message != null)
        {
            log(
                "Events.Bundle",
                event.getBundle(),
                null,
                LogLevel.INFO,
                message,
                null);
        }
    }

    public static StackTraceElement getStackTraceElement() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length == 0) {
            return null;
        }
        for (int i = 1; i < elements.length; i++) {
            if (!elements[i].getClassName().startsWith("org.apache.felix.log")) {
                return elements[i];
            }
        }
        return elements[1];
    }

    /** The messages returned for the service events. */
    private static final String[] SERVICE_EVENT_MESSAGES =
    {
        "ServiceEvent REGISTERED",
        "ServiceEvent MODIFIED",
        "ServiceEvent UNREGISTERING"
    };

    /**
     * Called when a service event occurs.
     * @param event the event that occured
     */
    public void serviceChanged(final ServiceEvent event)
    {
        int eventType = event.getType();
        String message = null;

        for (int i = 0; message == null && i < SERVICE_EVENT_MESSAGES.length; ++i)
        {
            if (eventType >> i == 1)
            {
                message = SERVICE_EVENT_MESSAGES[i];
            }
        }

        log(
            "Events.Service",
            event.getServiceReference().getBundle(),
            event.getServiceReference(),
            (eventType == ServiceEvent.MODIFIED) ? LogLevel.DEBUG : LogLevel.INFO,
            message,
            null);
    }
}