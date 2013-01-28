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
package org.apache.felix.status.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.felix.status.PrinterMode;
import org.apache.felix.status.StatusPrinter;
import org.apache.felix.status.StatusPrinterHandler;
import org.apache.felix.status.StatusPrinterManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The manager keeps track of all status printers and maintains them
 * based on their name. If more than one printer with the same name
 * is registered, the one with highest service ranking is used.
 */
public class StatusPrinterManagerImpl implements StatusPrinterManager,
    ServiceTrackerCustomizer {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Bundle Context .*/
    private final BundleContext bundleContext;

    /** Service tracker for status printers. */
    private final ServiceTracker cfgPrinterTracker;

    /** All adapters mapped by their name. */
    private final Map<String, List<StatusPrinterAdapter>> allAdapters = new HashMap<String, List<StatusPrinterAdapter>>();

    /** Used adapters. */
    private final Set<StatusPrinterAdapter> usedAdapters = new ConcurrentSkipListSet<StatusPrinterAdapter>();

    /** Registration for the web console. */
    private final ServiceRegistration pluginRegistration;

    /**
     * Create the status printer
     * @param btx Bundle Context
     * @throws InvalidSyntaxException Should only happen if we have an error in the code
     */
    public StatusPrinterManagerImpl(final BundleContext btx) throws InvalidSyntaxException {
        this.bundleContext = btx;
        this.cfgPrinterTracker = new ServiceTracker( this.bundleContext,
                this.bundleContext.createFilter("(&(" + StatusPrinter.CONFIG_PRINTER_MODES + "=*)"
                        + "(" + StatusPrinter.CONFIG_NAME + "=*)"
                        + "(" + StatusPrinter.CONFIG_TITLE + "=*))"),
                this );
        this.cfgPrinterTracker.open();

        this.pluginRegistration = DefaultWebConsolePlugin.register(btx, this);
    }

    /**
     * Dispose this service
     */
    public void dispose() {
        if ( this.pluginRegistration != null ) {
            this.pluginRegistration.unregister();
        }
        this.cfgPrinterTracker.close();
        synchronized ( this.allAdapters ) {
            this.allAdapters.clear();
        }
        this.usedAdapters.clear();
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(final ServiceReference reference) {
        final Object obj = this.bundleContext.getService(reference);
        if ( obj != null ) {
            this.addService(reference, obj);
        }
        return obj;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(final ServiceReference reference, final Object service) {
        this.removeService(reference);
        this.addService(reference, service);
    }

    private void addService(final ServiceReference reference, final Object obj) {
        final StatusPrinterDescription desc = new StatusPrinterDescription(reference);

        boolean valid = true;
        if ( desc.getModes() == null ) {
            logger.info("Ignoring status printer - printer modes configuration is missing: {}", reference);
            valid = false;
        }
        if ( desc.getName() == null ) {
            logger.info("Ignoring status printer - name configuration is missing: {}", reference);
            valid = false;
        }
        if ( desc.getTitle() == null ) {
            logger.info("Ignoring status printer - title configuration is missing: {}", reference);
            valid = false;
        }
        if ( valid ) {
            final StatusPrinterAdapter adapter = StatusPrinterAdapter.createAdapter(desc, obj);
            if ( adapter == null ) {
                logger.info("Ignoring status printer - printer method is missing: {}", reference);
            } else {
                this.addAdapter(adapter);
            }
        }
    }

    private void addAdapter(final StatusPrinterAdapter adapter) {
        StatusPrinterAdapter removeAdapter = null;
        StatusPrinterAdapter addAdapter = null;

        final String key = adapter.getName();
        synchronized ( this.allAdapters ) {
            List<StatusPrinterAdapter> list = this.allAdapters.get(key);
            final StatusPrinterAdapter first;
            if ( list == null ) {
                list = new LinkedList<StatusPrinterAdapter>();
                this.allAdapters.put(key, list);
                first = null;
            } else {
                first = list.get(0);
            }
            list.add(adapter);
            Collections.sort(list, StatusPrinterAdapter.RANKING_COMPARATOR);
            if ( first != null ) {
                if ( first != list.get(0) ) {
                    // update
                    removeAdapter = first;
                    addAdapter = adapter;
                }
            } else {
                // add
                addAdapter = adapter;
            }
        }
        if ( removeAdapter != null ) {
            final Iterator<StatusPrinterAdapter> i = this.usedAdapters.iterator();
            while ( i.hasNext() ) {
                if ( i.next() == removeAdapter ) {
                    i.remove();
                    break;
                }
            }
            removeAdapter.unregisterConsole();
        }
        if ( addAdapter != null ) {
            this.usedAdapters.add(addAdapter);
            addAdapter.registerConsole(this.bundleContext, this);
        }
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(final ServiceReference reference, final Object service) {
        this.removeService(reference);
        this.bundleContext.ungetService(reference);
    }

    private void removeService(final ServiceReference reference) {
        synchronized ( this.allAdapters ) {
            final Iterator<Map.Entry<String, List<StatusPrinterAdapter>>> i = this.allAdapters.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, List<StatusPrinterAdapter>> entry = i.next();
                final Iterator<StatusPrinterAdapter> iter = entry.getValue().iterator();
                boolean removed = false;
                while ( iter.hasNext() ) {
                    final StatusPrinterAdapter adapter = iter.next();
                    if ( adapter.getDescription().getServiceReference().compareTo(reference) == 0 ) {
                        iter.remove();
                        removed = true;
                        break;
                    }
                }
                if ( removed ) {
                    if ( entry.getValue().size() == 0 ) {
                        i.remove();
                    }
                    break;
                }
            }
        }
        final Iterator<StatusPrinterAdapter> iter = this.usedAdapters.iterator();
        while ( iter.hasNext() ) {
            final StatusPrinterAdapter adapter = iter.next();
            if ( adapter.getDescription().getServiceReference().compareTo(reference) == 0 ) {
                iter.remove();
                adapter.unregisterConsole();
                break;
            }
        }
    }

    /**
     * @see org.apache.felix.status.StatusPrinterManager#getAllHandlers()
     */
    public StatusPrinterHandler[] getAllHandlers() {
        return this.usedAdapters.toArray(new StatusPrinterHandler[this.usedAdapters.size()]);
    }

    /**
     * @see org.apache.felix.status.StatusPrinterManager#getHandlers(org.apache.felix.status.PrinterMode)
     */
    public StatusPrinterHandler[] getHandlers(final PrinterMode mode) {
        final List<StatusPrinterHandler> result = new ArrayList<StatusPrinterHandler>();
        for(final StatusPrinterAdapter printer : this.usedAdapters) {
            if ( printer.supports(mode) ) {
                result.add(printer);
            }
        }
        return result.toArray(new StatusPrinterHandler[result.size()]);
    }

    /**
     * @see org.apache.felix.status.StatusPrinterManager#getHandler(java.lang.String)
     */
    public StatusPrinterHandler getHandler(final String name) {
        for(final StatusPrinterAdapter printer : this.usedAdapters) {
            if ( name.equals(printer.getName()) ) {
                return printer;
            }
        }
        return null;
    }
}
