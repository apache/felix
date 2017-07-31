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
package org.apache.felix.http.base.internal.whiteboard.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Service tracker for all whiteboard services except servlet context helper.
 * This tracker does not get/unget the service objects itself, but just forwards the service reference
 * by creating an info data object. Each sub class creates a different
 * data object.
 */
public abstract class WhiteboardServiceTracker<T> extends ServiceTracker<T, ServiceReference<T>>
{
    /** Map containing all info objects reported from the trackers. */
    private final Map<Long, WhiteboardServiceInfo<T>> allInfos = new ConcurrentHashMap<Long, WhiteboardServiceInfo<T>>();

    private static org.osgi.framework.Filter createFilter(final BundleContext btx, final String expr)
    {
        try
        {
            return btx.createFilter(expr);
        }
        catch ( final InvalidSyntaxException ise)
        {
            // we can safely ignore it as the filter is a constant
        }
        return null; // we never get here - and if we get an NPE which is fine
    }

    /** The  manager is called for each added/removed reference. */
    private final WhiteboardManager contextManager;

    /**
     * Create a new tracker
     * @param contextManager The context manager
     * @param bundleContext The bundle context.
     * @param filterExpr The filter expression for the services to track
     */
    public WhiteboardServiceTracker(final WhiteboardManager contextManager,
            final BundleContext bundleContext, final String filterExpr)
    {
        super(bundleContext, createFilter(bundleContext, filterExpr), null);
        this.contextManager = contextManager;
    }

    @Override
    public void close() {
        super.close();
        this.allInfos.clear();
    }

    @Override
    public final ServiceReference<T> addingService(final ServiceReference<T> ref)
    {
        this.added(ref);
        return ref;
    }

    @Override
    public final void modifiedService(final ServiceReference<T> ref, final ServiceReference<T> service)
    {
        this.modified(ref);
    }

    @Override
    public final void removedService(final ServiceReference<T> ref, final ServiceReference<T> service)
    {
        this.removed(ref);
    }

    private void modified(final ServiceReference<T> ref)
    {
        removed(ref);
        added(ref);
    }

    private void added(final ServiceReference<T> ref)
    {
        final WhiteboardServiceInfo<T> info = this.getServiceInfo(ref);
        if ( this.contextManager.addWhiteboardService(info) )
        {
            this.allInfos.put((Long)ref.getProperty(Constants.SERVICE_ID), info);
        }
    }

    private void removed(final ServiceReference<T> ref)
    {
        final WhiteboardServiceInfo<T> info = this.allInfos.remove(ref.getProperty(Constants.SERVICE_ID));
        if ( info != null )
        {
            this.contextManager.removeWhiteboardService(info);
        }
    }

    /**
     * Implemented by sub classes to create the correct whiteboard service info object.
     * @param ref The service reference
     * @return A whiteboard service info
     */
    protected abstract WhiteboardServiceInfo<T> getServiceInfo(final ServiceReference<T> ref);
}
