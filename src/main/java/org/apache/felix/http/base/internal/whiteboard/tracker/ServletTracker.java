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

import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.whiteboard.ServletContextHelperManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class ServletTracker extends AbstractReferenceTracker<Servlet>
{
    private final ServletContextHelperManager contextManager;

    private static org.osgi.framework.Filter createFilter(final BundleContext btx)
    {
        try
        {
            return btx.createFilter(String.format("(&(objectClass=%s)(%s=*))",
                    Servlet.class.getName(),
                    HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN));
        }
        catch ( final InvalidSyntaxException ise)
        {
            // we can safely ignore it as the above filter is a constant
        }
        return null; // we never get here - and if we get an NPE which is fine
    }

    public ServletTracker(final BundleContext context, final ServletContextHelperManager manager)
    {
        super(context, createFilter(context));
        this.contextManager = manager;
    }

    @Override
    protected void added(final ServiceReference<Servlet> ref)
    {
        final ServletInfo info = new ServletInfo(ref);

        if ( info.isValid() )
        {
            this.contextManager.addServlet(info);
        }
        else
        {
            SystemLogger.debug("Ignoring Servlet service " + ref);
        }
    }

    @Override
    protected void removed(final ServiceReference<Servlet> ref)
    {
        final ServletInfo info = new ServletInfo(ref);

        if ( info.isValid() )
        {
            this.contextManager.removeServlet(info);
        }
    }
}
