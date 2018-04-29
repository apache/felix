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

<<<<<<< HEAD
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
=======
import org.apache.felix.http.whiteboard.internal.manager.ExtenderManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public final class HttpContextTracker
    extends AbstractTracker<HttpContext>
{
    private final ExtenderManager manager;

<<<<<<< HEAD
    public HttpContextTracker(BundleContext context, ExtenderManager manager)
=======
    public HttpContextTracker(final BundleContext context, final ExtenderManager manager)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        super(context, HttpContext.class);
        this.manager = manager;
    }

<<<<<<< HEAD
    protected void added(HttpContext service, ServiceReference ref)
    {
        this.manager.add(service, ref);
    }

    protected void modified(HttpContext service, ServiceReference ref)
    {
        removed(service, ref);
        added(service, ref);
    }

    protected void removed(HttpContext service, ServiceReference ref)
    {
        this.manager.remove(service);
=======
    @Override
    protected void added(final HttpContext service, final ServiceReference<HttpContext> ref)
    {
        this.logDeprecationWarning("HttpContext", service, ref);
        this.manager.addHttpContext(service, ref);
    }

    @Override
    protected void removed(final HttpContext service, final ServiceReference<HttpContext> ref)
    {
        this.manager.removeHttpContext(ref);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
}
