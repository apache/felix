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

import javax.servlet.http.HttpSessionIdListener;

import org.apache.felix.http.base.internal.runtime.HttpSessionIdListenerInfo;
import org.apache.felix.http.base.internal.runtime.WhiteboardServiceInfo;
import org.apache.felix.http.base.internal.whiteboard.ServletContextHelperManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class HttpSessionIdListenerTracker extends WhiteboardServiceTracker<HttpSessionIdListener>
{
    public HttpSessionIdListenerTracker(final BundleContext context, final ServletContextHelperManager manager)
    {
        super(manager, context, createListenerFilterExpression(HttpSessionIdListener.class));
    }

    @Override
    protected WhiteboardServiceInfo<HttpSessionIdListener> getServiceInfo(final ServiceReference<HttpSessionIdListener> ref) {
        return new HttpSessionIdListenerInfo(ref);
    }
}
