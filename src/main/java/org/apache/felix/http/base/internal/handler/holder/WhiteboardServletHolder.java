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
package org.apache.felix.http.base.internal.handler.holder;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * Servlet holder for servlets registered through the http whiteboard.
 */
public final class WhiteboardServletHolder extends ServletHolder
{
    private final BundleContext bundleContext;

    public WhiteboardServletHolder(final ServletContext context,
            final ServletInfo servletInfo,
            final BundleContext bundleContext)
    {
        super(context, servletInfo);
        this.bundleContext = bundleContext;
    }

    @Override
    public int init()
    {
        final ServiceReference<Servlet> serviceReference = getServletInfo().getServiceReference();
        final ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(serviceReference);

        this.setServlet((so == null ? null : so.getService()));

        final int reason = super.init();
        if ( reason != -1 )
        {
            so.ungetService(this.getServlet());
            this.setServlet(null);
        }
        return -reason;
    }

    @Override
    public void destroy()
    {
        final Servlet s = this.getServlet();
        if ( s != null )
        {
            super.destroy();

            final ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(getServletInfo().getServiceReference());
            if (so != null)
            {
                so.ungetService(s);
            }
        }
    }
}
