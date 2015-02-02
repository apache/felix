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

import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.whiteboard.ServletContextHelperManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Service tracker that does not get/unget the service objects itself, but
 * just forwards the service reference.
 */
public abstract class AbstractReferenceTracker<T> extends ServiceTracker<T, ServiceReference<T>>
{
    private final ServletContextHelperManager contextManager;

    public AbstractReferenceTracker(final ServletContextHelperManager contextManager,
            final BundleContext context, final Filter filter)
    {
        super(context, filter, null);
        this.contextManager = contextManager;
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

    protected void modified(final ServiceReference<T> ref)
    {
        removed(ref);
        added(ref);
    }

    protected void added(final ServiceReference<T> ref)
    {
        final WhiteboardServiceInfo<T> info = this.getServiceInfo(ref);
        this.contextManager.addWhiteboardService(info);
    }

    protected void removed(final ServiceReference<T> ref)
    {
        final WhiteboardServiceInfo<T> info = this.getServiceInfo(ref);
        this.contextManager.removeWhiteboardService(info);
    }

    protected abstract WhiteboardServiceInfo<T> getServiceInfo(final ServiceReference<T> ref);
}
