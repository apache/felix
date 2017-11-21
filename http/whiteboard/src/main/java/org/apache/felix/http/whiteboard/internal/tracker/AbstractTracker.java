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
package org.apache.felix.http.whiteboard.internal.tracker;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractTracker<T>
    extends ServiceTracker<T, T>
{
    public AbstractTracker(final BundleContext context, final Filter filter)
    {
        super(context, filter, null);
    }

    public AbstractTracker(final BundleContext context, final Class<T> clazz)
    {
        super(context, clazz.getName(), null);
    }

    protected void logDeprecationWarning(final String id, final T service, final ServiceReference<T> ref)
    {
        SystemLogger.warning("Deprecation warning: " +
                    id + " registered through Apache Felix whiteboard service: " + getInfo(service, ref) +
                    ". Please change your code to use the OSGi Http Whiteboard Service.", null);
    }

    private String getInfo(final T service, final ServiceReference<T> ref)
    {
        return "Service " + ref.getProperty(Constants.SERVICE_PID) + " from bundle "
                + ref.getBundle().getBundleId()
                + (ref.getBundle().getSymbolicName() != null ? ref.getBundle().getSymbolicName() + ":" + ref.getBundle().getVersion() : "")
                + " class " + service.getClass();
    }

    @Override
    public final T addingService(final ServiceReference<T> ref)
    {
        T service = super.addingService(ref);
        added(service, ref);
        return service;
    }

    @Override
    public final void modifiedService(final ServiceReference<T> ref, T service)
    {
        removed(service, ref);
        added(service, ref);
    }

    @Override
    public final void removedService(final ServiceReference<T> ref, T service)
    {
        super.removedService(ref, service);
        removed(service, ref);
    }

    protected abstract void added(T service, ServiceReference<T> ref);

    protected abstract void removed(T service, ServiceReference<T> ref);
}
