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
package org.apache.felix.http.proxy;

import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;

public final class ProxyServlet
    extends AbstractProxyServlet
{
    private static final long serialVersionUID = -5740821969955405599L;

    protected BundleContext getBundleContext()
        throws ServletException
    {
        Object context = getServletContext().getAttribute(BundleContext.class.getName());
        if (context instanceof BundleContext)
        {
            return (BundleContext)context;
        }

        throw new ServletException("Bundle context attribute [" + BundleContext.class.getName() +
                "] not set in servlet context");
    }
}
