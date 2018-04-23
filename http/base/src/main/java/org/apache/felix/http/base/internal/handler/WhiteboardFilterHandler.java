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
package org.apache.felix.http.base.internal.handler;

import javax.servlet.Filter;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Filter holder for filters registered through the http whiteboard.
 */
public final class WhiteboardFilterHandler extends FilterHandler
{
    private final BundleContext bundleContext;

    public WhiteboardFilterHandler(final long contextServiceId,
            final ExtServletContext context,
            final FilterInfo filterInfo,
            final BundleContext bundleContext)
    {
        super(contextServiceId, context, filterInfo);
        this.bundleContext = bundleContext;
    }

    @Override
    public int init()
    {
        if ( this.useCount > 0 )
        {
            this.useCount++;
            return -1;
        }

        final ServiceReference<Filter> serviceReference = getFilterInfo().getServiceReference();
        final ServiceObjects<Filter> so = this.bundleContext.getServiceObjects(serviceReference);

        this.setFilter((so == null ? null : so.getService()));

        final int reason = super.init();
        if ( reason != -1 )
        {
            if ( so != null )
            {
                so.ungetService(this.getFilter());
            }
            this.setFilter(null);
        }
        return reason;
    }

    @Override
    public boolean destroy()
    {
        final Filter s = this.getFilter();
        if ( s != null )
        {
            if ( super.destroy() )
            {

                final ServiceObjects<Filter> so = this.bundleContext.getServiceObjects(getFilterInfo().getServiceReference());
                if (so != null)
                {
                    so.ungetService(s);
                }
                return true;
            }
        }
        return false;
    }
}
