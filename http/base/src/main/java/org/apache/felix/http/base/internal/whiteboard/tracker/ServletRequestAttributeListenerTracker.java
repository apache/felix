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

import javax.servlet.ServletRequestAttributeListener;

import org.apache.felix.http.base.internal.runtime.ServletRequestAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.whiteboard.ServletContextHelperManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class ServletRequestAttributeListenerTracker extends WhiteboardServiceTracker<ServletRequestAttributeListener>
{
    public ServletRequestAttributeListenerTracker(final BundleContext context, final ServletContextHelperManager manager)
    {
        super(manager, context, createListenerFilterExpression(ServletRequestAttributeListener.class));
    }

    @Override
    protected WhiteboardServiceInfo<ServletRequestAttributeListener> getServiceInfo(final ServiceReference<ServletRequestAttributeListener> ref) {
        return new ServletRequestAttributeListenerInfo(ref);
    }
}
