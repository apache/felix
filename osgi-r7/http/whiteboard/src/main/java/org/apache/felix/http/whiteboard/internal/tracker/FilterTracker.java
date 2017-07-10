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

import javax.servlet.Filter;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.whiteboard.HttpWhiteboardConstants;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public final class FilterTracker
    extends AbstractTracker<Filter>
{
    private final ExtenderManager manager;

    private static org.osgi.framework.Filter createFilter(final BundleContext btx)
    {
        try
        {
            return btx.createFilter(String.format("(&(objectClass=%s)(%s=*))",
                    Filter.class.getName(),
                    HttpWhiteboardConstants.PATTERN));
        }
        catch ( final InvalidSyntaxException ise)
        {
            // we can safely ignore it as the above filter is a constant
        }
        return null; // we never get here - and if we get an NPE which is fine
    }

    public FilterTracker(BundleContext context, ExtenderManager manager)
    {
        super(context, createFilter(context));
        this.manager = manager;
    }

    @Override
    protected void added(Filter service, ServiceReference ref)
    {
        SystemLogger.warning("Deprecation warning: " +
                "Filter registered through Apache Felix whiteboard service: " + ref +
                ". Please change your code to the OSGi Http Whiteboard Service.", null);

        this.manager.add(service, ref);
    }

    @Override
    protected void modified(Filter service, ServiceReference ref)
    {
        removed(service, ref);
        added(service, ref);
    }

    @Override
    protected void removed(Filter service, ServiceReference ref)
    {
        this.manager.removeFilter(ref);
    }
}
