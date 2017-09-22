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
package org.apache.felix.webconsole.plugins.ds.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.bundleinfo.BundleInfoProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Activator is the main starting class.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceComponentRuntime>
{

    private ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> tracker;
    private BundleContext context;

    private SimpleWebConsolePlugin plugin;

    private ServiceRegistration<InventoryPrinter> printerRegistration;

    private ServiceRegistration<BundleInfoProvider> infoRegistration;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void start(BundleContext context) throws Exception
    {
        this.context = context;
        this.tracker = new ServiceTracker(context, ServiceComponentRuntime.class, this);
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

    // - begin tracker
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void modifiedService(final ServiceReference<ServiceComponentRuntime> reference, final ServiceComponentRuntime service)
    {/* unused */
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public final ServiceComponentRuntime addingService(final ServiceReference<ServiceComponentRuntime> reference)
    {
        SimpleWebConsolePlugin plugin = this.plugin;
        if (plugin == null)
        {
            final ServiceComponentRuntime service = context.getService(reference);
            this.plugin = plugin = new WebConsolePlugin(service).register(context);

            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String name = "Declarative Services Components";
            props.put(InventoryPrinter.NAME, "scr"); //$NON-NLS-1$
            props.put(InventoryPrinter.TITLE, name);
            props.put(InventoryPrinter.FORMAT, new String[] {
                    Format.TEXT.toString(),
                    Format.JSON.toString()
            });
            printerRegistration = context.registerService(InventoryPrinter.class,
                new ComponentConfigurationPrinter(service, (WebConsolePlugin) plugin),
                props);

            infoRegistration = new InfoProvider(context.getBundle(), service).register(context);
        }

        return context.getService(reference);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void removedService(final ServiceReference<ServiceComponentRuntime> reference, final ServiceComponentRuntime service)
    {
        SimpleWebConsolePlugin plugin = this.plugin;

        if (tracker.getTrackingCount() == 0 && plugin != null)
        {
            // remove service
            plugin.unregister();
            this.plugin = null;
            // unregister configuration printer too
            ServiceRegistration<?> reg = printerRegistration;
            if (reg != null)
            {
                reg.unregister();
                printerRegistration = null;
            }
            // unregister info provider too
            reg = infoRegistration;
            if (reg != null)
            {
                reg.unregister();
                infoRegistration = null;
            }
        }

    }
}
