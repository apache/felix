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
package org.apache.felix.inventory.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.impl.webconsole.ConsoleConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The manager keeps track of all inventory printers and maintains them
 * based on their name. If more than one printer with the same name
 * is registered, the one with highest service ranking is used.
 */
public class InventoryPrinterManagerImpl implements ServiceTrackerCustomizer
{

    /** Bundle Context . */
    private final BundleContext bundleContext;

    /** Service tracker for Inventory printers. */
    private final ServiceTracker cfgPrinterTracker;

    /**
     * All adapters mapped by their name. Type of the map: String,
     * List<InventoryPrinterAdapter>
     */
    private final Map allAdapters = new HashMap();

    /** Used adapters. Type of the set: InventoryPrinterAdapter */
    private final Set usedAdapters = new TreeSet();

    /** Registration for the web console. */
    private final ServiceRegistration pluginRegistration;

    /**
     * Create the inventory printer manager
     *
     * @param btx Bundle Context
     * @throws InvalidSyntaxException Should only happen if we have an error in
     *             the code
     */
    public InventoryPrinterManagerImpl(final BundleContext btx) throws InvalidSyntaxException
    {
        this.bundleContext = btx;
        this.cfgPrinterTracker = new ServiceTracker(this.bundleContext, InventoryPrinter.SERVICE, this);
        this.cfgPrinterTracker.open();

        final Dictionary props = new Hashtable();
        props.put(ConsoleConstants.PLUGIN_LABEL, ConsoleConstants.NAME);
        props.put(ConsoleConstants.PLUGIN_TITLE, ConsoleConstants.TITLE);
        props.put(ConsoleConstants.PLUGIN_CATEGORY, ConsoleConstants.WEB_CONSOLE_CATEGORY);
        this.pluginRegistration = btx.registerService(ConsoleConstants.INTERFACE_SERVLET, new ServiceFactory()
        {
            public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service)
            {
                // nothing to do
            }

            public Object getService(final Bundle bundle, final ServiceRegistration registration)
            {
                return new DefaultWebConsolePlugin(InventoryPrinterManagerImpl.this);
            }
        }, props);
    }

    /**
     * Dispose this service
     */
    public void dispose()
    {
        if (this.pluginRegistration != null)
        {
            this.pluginRegistration.unregister();
        }
        this.cfgPrinterTracker.close();
        synchronized (this.allAdapters)
        {
            this.allAdapters.clear();
        }
        synchronized (this.usedAdapters)
        {
            this.usedAdapters.clear();
        }
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(final ServiceReference reference)
    {
        final Object obj = this.bundleContext.getService(reference);
        if (obj != null)
        {
            this.addService(reference, (InventoryPrinter) obj);
        }

        return obj;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public void modifiedService(final ServiceReference reference, final Object service)
    {
        this.removeService(reference);
        this.addService(reference, (InventoryPrinter) service);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public void removedService(final ServiceReference reference, final Object service)
    {
        this.removeService(reference);
        this.bundleContext.ungetService(reference);
    }

    private void addService(final ServiceReference reference, final InventoryPrinter obj)
    {
        final InventoryPrinterDescription desc = new InventoryPrinterDescription(reference);
        final InventoryPrinterAdapter adapter = new InventoryPrinterAdapter(desc, obj);

        InventoryPrinterAdapter removeAdapter = null;
        InventoryPrinterAdapter addAdapter = null;

        final String key = adapter.getName();
        synchronized (this.allAdapters)
        {
            List list = (List) this.allAdapters.get(key);
            final InventoryPrinterAdapter first;
            if (list == null)
            {
                list = new LinkedList();
                this.allAdapters.put(key, list);
                first = null;
            }
            else
            {
                first = (InventoryPrinterAdapter) list.get(0);
            }
            list.add(adapter);
            Collections.sort(list, InventoryPrinterAdapter.RANKING_COMPARATOR);
            if (first != null)
            {
                if (first != list.get(0))
                {
                    // update
                    removeAdapter = first;
                    addAdapter = adapter;
                }
            }
            else
            {
                // add
                addAdapter = adapter;
            }
        }
        if (removeAdapter != null)
        {
            removeAdapter.unregisterConsole();
            synchronized (this.usedAdapters)
            {
                this.usedAdapters.remove(removeAdapter);
            }
        }
        if (addAdapter != null)
        {
            synchronized (this.usedAdapters)
            {
                this.usedAdapters.add(addAdapter);
            }
            addAdapter.registerConsole(this.bundleContext, this);
        }
    }

    private void removeService(final ServiceReference reference)
    {
        synchronized (this.allAdapters)
        {
            final Iterator i = this.allAdapters.entrySet().iterator();
            while (i.hasNext())
            {
                final Map.Entry entry = (Entry) i.next();
                final Iterator iter = ((List) entry.getValue()).iterator();
                boolean removed = false;
                while (iter.hasNext())
                {
                    final InventoryPrinterAdapter adapter = (InventoryPrinterAdapter) iter.next();
                    if (adapter.getDescription().getServiceReference().equals(reference))
                    {
                        iter.remove();
                        removed = true;
                        break;
                    }
                }
                if (removed)
                {
                    if (((List) entry.getValue()).size() == 0)
                    {
                        i.remove();
                    }
                    break;
                }
            }
        }

        InventoryPrinterAdapter adapterToUnregister = null;
        synchronized (this.usedAdapters)
        {
            final Iterator iter = this.usedAdapters.iterator();
            while (iter.hasNext())
            {
                final InventoryPrinterAdapter adapter = (InventoryPrinterAdapter) iter.next();
                if (adapter.getDescription().getServiceReference().equals(reference))
                {
                    iter.remove();
                    adapterToUnregister = adapter;
                    break;
                }
            }
        }
        if (adapterToUnregister != null)
        {
            adapterToUnregister.unregisterConsole();
        }
    }

    /**
     * Get all handlers supporting the format or all handlers if {@code format}
     * is {@code null}.
     *
     * @param format The {@link Format} the returned handlers are expected to
     *            support. If this parameter is {@code null} all handlers are
     *            returned regardless of {@link Format} supported by the
     *            handlers.
     *
     * @return A list of handlers - might be empty.
     */
    public InventoryPrinterHandler[] getHandlers(final Format format)
    {
        final List result = new ArrayList();
        synchronized (this.usedAdapters)
        {
            final Iterator i = this.usedAdapters.iterator();
            while (i.hasNext())
            {
                final InventoryPrinterAdapter printer = (InventoryPrinterAdapter) i.next();
                if (format == null || printer.supports(format))
                {
                    result.add(printer);
                }
            }
        }
        return (InventoryPrinterHandler[]) result.toArray(new InventoryPrinterHandler[result.size()]);
    }

    /**
     * Return a handler for the unique name.
     *
     * @return The corresponding handler or <code>null</code>.
     */
    public InventoryPrinterHandler getHandler(final String name)
    {
        synchronized (this.usedAdapters)
        {
            final Iterator i = this.usedAdapters.iterator();
            while (i.hasNext())
            {
                final InventoryPrinterAdapter printer = (InventoryPrinterAdapter) i.next();
                if (name.equals(printer.getName()))
                {
                    return printer;
                }
            }
        }
        return null;
    }
}
