/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This is the main starting class of the bundle.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer
{

    private ServiceTracker pkgAdminTracker;

    private BundleContext context;
    private SimpleWebConsolePlugin plugin;
    private ServiceRegistration printerReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception
    {
        this.context = context;
        this.pkgAdminTracker = new ServiceTracker(context,
            "org.osgi.service.packageadmin.PackageAdmin", this); //$NON-NLS-1$
        this.pkgAdminTracker.open();

        // register configuration printer
        final Dictionary/*<String, Object>*/props = new Hashtable/*<String, Object>*/();
        props.put(InventoryPrinter.NAME, "duplicate_exports"); //$NON-NLS-1$
        props.put(InventoryPrinter.TITLE, "Duplicate Exports"); //$NON-NLS-1$
        props.put(InventoryPrinter.FORMAT, new String[] { Format.TEXT.toString() });

        printerReg = context.registerService(
            InventoryPrinter.class.getName(),
            new WebConsolePrinter(context, pkgAdminTracker), props);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception
    {
        if (printerReg != null)
        {
            printerReg.unregister();
            printerReg = null;
        }

        if (this.pkgAdminTracker != null)
        {
            this.pkgAdminTracker.close();
            this.pkgAdminTracker = null;
        }

        this.context = null;
    }

    // - begin tracker
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void modifiedService(ServiceReference reference, Object service)
    {/* unused */
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public final Object addingService(ServiceReference reference)
    {
        SimpleWebConsolePlugin plugin = this.plugin;
        Object ret = null;
        if (plugin == null)
        {
            ret = context.getService(reference);
            this.plugin = plugin = new WebConsolePlugin(context, ret).register(context);
        }

        return ret;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void removedService(ServiceReference reference, Object service)
    {
        SimpleWebConsolePlugin plugin = this.plugin;

        if (pkgAdminTracker.size() <= 1 && plugin != null)
        {
            plugin.unregister();
            this.plugin = null;
        }

    }

}
