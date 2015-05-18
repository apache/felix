/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.metatype.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Activator is the main starting class.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer
{

    private static final String META_TYPE_NAME = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$

    private ServiceTracker tracker;
    private BundleContext context;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public final void start(BundleContext context) throws Exception
    {
        this.context = context;
        this.tracker = new ServiceTracker(context, META_TYPE_NAME, this);
        this.tracker.open();
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public final void stop(BundleContext context) throws Exception
    {
        if (tracker != null)
        {
            tracker.close();
            tracker = null;
        }
    }

    public final void modifiedService(ServiceReference reference, Object service)
    {/* unused */
    }

    public final Object addingService(ServiceReference reference)
    {
        final MetaTypeService service = (MetaTypeService) context.getService(reference);
        return new MetatypeInventoryPrinter(context, service);
    }

    public final void removedService(ServiceReference reference, Object service)
    {
        ((MetatypeInventoryPrinter) service).unregister();
    }
}
