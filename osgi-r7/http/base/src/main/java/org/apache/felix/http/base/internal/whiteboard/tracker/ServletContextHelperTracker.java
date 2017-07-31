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

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks all {@link ServletContextHelper} services.
 */
public final class ServletContextHelperTracker extends ServiceTracker<ServletContextHelper, ServiceReference<ServletContextHelper>>
{
    private final WhiteboardManager contextManager;

    /** Map containing all info objects reported from the trackers. */
    private final Map<Long, ServletContextHelperInfo> allInfos = new ConcurrentHashMap<Long, ServletContextHelperInfo>();

    private static org.osgi.framework.Filter createFilter(final BundleContext btx)
    {
        try
        {
            return btx.createFilter(String.format("(objectClass=%s)",
                    ServletContextHelper.class.getName()));
        }
        catch ( final InvalidSyntaxException ise)
        {
            // we can safely ignore it as the above filter is a constant
        }
        return null; // we never get here - and if we get an NPE which is fine
    }

    public ServletContextHelperTracker(@Nonnull final BundleContext context, @Nonnull final WhiteboardManager manager)
    {
        super(context, createFilter(context), null);
        this.contextManager = manager;
    }

    @Override
    public void close() {
        super.close();
        this.allInfos.clear();
    }

    @Override
    public final ServiceReference<ServletContextHelper> addingService(@Nonnull final ServiceReference<ServletContextHelper> ref)
    {
        this.added(ref);
        return ref;
    }

    @Override
    public final void modifiedService(@Nonnull final ServiceReference<ServletContextHelper> ref, @Nonnull final ServiceReference<ServletContextHelper> service)
    {
        this.removed(ref);
        this.added(ref);
    }

    @Override
    public final void removedService(@Nonnull final ServiceReference<ServletContextHelper> ref, @Nonnull final ServiceReference<ServletContextHelper> service)
    {
        this.removed(ref);
    }

    private void added(@Nonnull final ServiceReference<ServletContextHelper> ref)
    {
        final ServletContextHelperInfo info = new ServletContextHelperInfo(ref);
        if ( this.contextManager.addContextHelper(info) )
        {
            this.allInfos.put((Long)ref.getProperty(Constants.SERVICE_ID), info);
        }
    }

    private void removed(@Nonnull final ServiceReference<ServletContextHelper> ref)
    {
        final ServletContextHelperInfo info = this.allInfos.get(ref.getProperty(Constants.SERVICE_ID));
        if ( info != null )
        {
            this.contextManager.removeContextHelper(info);
        }
    }
}
