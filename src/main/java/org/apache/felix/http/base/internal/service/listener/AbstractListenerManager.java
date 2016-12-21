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
package org.apache.felix.http.base.internal.service.listener;

import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.handler.WhiteboardListenerHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.EventListenerRegistry;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.service.HttpServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implementation of the Apache Felix proprietary whiteboard for listeners
 *
 * @deprecated
 */
@Deprecated
public class AbstractListenerManager extends ServiceTracker<EventListener, ServiceReference<EventListener>>
{

    /** Map containing all info objects reported from the trackers. */
    private final Map<Long, ListenerInfo> allInfos = new ConcurrentHashMap<Long, ListenerInfo>();

    private final Object lock = new Object();

    private final EventListenerRegistry registry;

    private static org.osgi.framework.Filter createFilter(final BundleContext btx)
    {
        try
        {
            return btx.createFilter(String.format("(&" +
                                                   "(!(%s=*))" +
                                                   "(|(objectClass=%s)(objectClass=%s)(objectClass=%s)(objectClass=%s)(objectClass=%s))" +
                                                   ")",
                    HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER,
                    HttpSessionAttributeListener.class.getName(),
                    HttpSessionListener.class.getName(),
                    ServletContextAttributeListener.class.getName(),
                    ServletRequestAttributeListener.class.getName(),
                    ServletRequestListener.class.getName()));
        }
        catch ( final InvalidSyntaxException ise)
        {
            // we can safely ignore it as the above filter is a constant
        }
        return null; // we never get here - and if, we get an NPE which is fine
    }

    public AbstractListenerManager(final BundleContext context,
            final EventListenerRegistry registry)
    {
        super(context, createFilter(context), null);
        this.registry = registry;
    }

    @Override
    public ServiceReference<EventListener> addingService(final ServiceReference<EventListener> reference)
    {
        SystemLogger.warning("Deprecation warning: " +
            "Listener registered through Apache Felix whiteboard service: " + reference +
            ". Please change your code to the OSGi Http Whiteboard Service.", null);

        final ListenerInfo info = new ListenerInfo(reference, true);
        this.allInfos.put((Long)reference.getProperty(Constants.SERVICE_ID), info);

        final ListenerHandler listenerHandler = new WhiteboardListenerHandler(
                HttpServiceFactory.HTTP_SERVICE_CONTEXT_SERVICE_ID,
                null, // context is only relevant for ServletContextListener which is not supported here
                info,
                this.context);
        synchronized (lock)
        {
            registry.addListeners(listenerHandler);
        }

        return reference;
    }

    @Override
    public void modifiedService(final ServiceReference<EventListener> reference, final ServiceReference<EventListener> service)
    {
        synchronized ( lock )
        {
            this.removedService(reference, service);
            this.addingService(reference);
        }
    }

    @Override
    public void removedService(final ServiceReference<EventListener> reference, final ServiceReference<EventListener> service)
    {
        final ListenerInfo info = this.allInfos.remove(reference.getProperty(Constants.SERVICE_ID));
        if ( info != null ) {
            synchronized (lock)
            {
                registry.removeListeners(info);
            }
        }
    }
}
