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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.runtime.ServletContextHelperInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

public final class WhiteboardServletHandler extends ServletHandler
{
    private final BundleContext bundleContext;

    private Servlet servlet;

    public WhiteboardServletHandler(ServletContextHelperInfo contextInfo,
            ExtServletContext context,
            ServletInfo servletInfo,
            BundleContext bundleContext)
    {
        super(contextInfo.getServiceId(), context, checkIsResource(servletInfo, false));
        this.bundleContext = bundleContext;
    }

    @Override
    public Servlet getServlet()
    {
        return servlet;
    }

    @Override
    protected Object getSubject()
    {
        return getServlet();
    }

    @Override
    public void init() throws ServletException
    {
        if (servlet != null)
        {
            return;
        }

        ServiceReference<Servlet> serviceReference = getServletInfo().getServiceReference();
        ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(serviceReference);

        servlet = so.getService();

        if (servlet == null)
        {
            // TODO throw Exception - service ungettable ?
            return;
        }

        try {
            servlet.init(new ServletConfigImpl(getName(), getContext(), getInitParams()));
        } catch (ServletException e) {
            so.ungetService(servlet);
            throw e;
        }
    }

    @Override
    public void destroy()
    {
        if (servlet == null)
        {
            return;
        }

        servlet.destroy();

        ServiceObjects<Servlet> so = this.bundleContext.getServiceObjects(getServletInfo().getServiceReference());
        // TODO check if this is needed
        if (so != null)
        {
            so.ungetService(servlet);
        }
        servlet = null;
    }
}
