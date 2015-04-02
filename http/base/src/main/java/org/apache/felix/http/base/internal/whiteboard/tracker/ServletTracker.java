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

import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.whiteboard.WhiteboardManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public final class ServletTracker extends WhiteboardServiceTracker<Servlet>
{
    public ServletTracker(final BundleContext context, final WhiteboardManager manager)
    {
        super(manager, context, String.format("(&(objectClass=%s)(|(%s=*)(%s=*)))",
                Servlet.class.getName(),
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE));
    }

    @Override
    protected WhiteboardServiceInfo<Servlet> getServiceInfo(final ServiceReference<Servlet> ref) {
        return new ServletInfo(ref);
    }
}
