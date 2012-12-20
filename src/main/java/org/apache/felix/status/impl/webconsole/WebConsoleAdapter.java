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
package org.apache.felix.status.impl.webconsole;


import java.io.PrintWriter;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.felix.status.PrinterMode;
import org.apache.felix.status.StatusPrinter;
import org.apache.felix.status.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 */
public class WebConsoleAdapter implements ServiceTrackerCustomizer {

    private final BundleContext bundleContext;

    private final ServiceTracker cfgPrinterTracker;

    private final Map<ServiceReference, ServiceRegistration> registrations = new HashMap<ServiceReference, ServiceRegistration>();

    private final ResourceBundleManager rbManager;

    public WebConsoleAdapter(final BundleContext btx) throws InvalidSyntaxException {
        this.bundleContext = btx;
        this.rbManager = new ResourceBundleManager(btx);
        this.cfgPrinterTracker = new ServiceTracker( this.bundleContext,
                this.bundleContext.createFilter("(|(" + Constants.OBJECTCLASS + "=" + ConsoleConstants.INTERFACE_CONFIGURATION_PRINTER + ")" +
                        "(&(" + ConsoleConstants.PLUGIN_LABEL + "=*)(&("
                        + ConsoleConstants.PLUGIN_TITLE + "=*)("
                        + ConsoleConstants.CONFIG_PRINTER_MODES + "=*))))"),
                this );
        this.cfgPrinterTracker.open();
    }

    /**
     * Dispose this service
     */
    public void dispose() {
        this.cfgPrinterTracker.close();
        synchronized ( this.registrations ) {
            for(final ServiceRegistration reg : this.registrations.values()) {
                reg.unregister();
            }
            this.registrations.clear();
        }
        this.rbManager.dispose();
    }

    public void add(final ServiceReference reference, final Object service) {
        final ConfigurationPrinterAdapter cpa = ConfigurationPrinterAdapter.createAdapter(service, reference);
        if ( cpa != null && cpa.title != null ) {
            if ( cpa.title.startsWith("%") ) {
                final String key = cpa.title.substring(1);
                final ResourceBundle rb = this.rbManager.getResourceBundle(reference.getBundle());
                if ( rb == null || !rb.containsKey(key) ) {
                    cpa.title = key;
                } else {
                    cpa.title = rb.getString(key);
                }
            }
            if ( cpa.label == null ) {
                cpa.label = cpa.title;
            }
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(StatusPrinter.CONFIG_NAME, cpa.label);
            props.put(StatusPrinter.CONFIG_TITLE, cpa.title);
            props.put(StatusPrinter.CONFIG_PRINTER_MODES, cpa.getPrinterModes());

            if ( reference.getProperty(ConsoleConstants.PLUGIN_CATEGORY) != null ) {
                props.put(StatusPrinter.CONFIG_CATEGORY, reference.getProperty(ConsoleConstants.PLUGIN_CATEGORY));
            }
            final ServiceRegistration reg = this.bundleContext.registerService(StatusPrinter.class.getName(), new ZipAttachmentProvider() {

                /**
                 * @see org.apache.felix.status.StatusPrinter#print(org.apache.felix.status.PrinterMode, java.io.PrintWriter)
                 */
                public void print(final PrinterMode mode, final PrintWriter printWriter) {
                    final String m;
                    if ( mode == PrinterMode.HTML_BODY ) {
                        m = ConsoleConstants.MODE_WEB;
                    } else if ( mode == PrinterMode.TEXT ) {
                        m = ConsoleConstants.MODE_TXT;
                    } else if ( mode == PrinterMode.ZIP_FILE ) {
                        m = ConsoleConstants.MODE_ZIP;
                    } else {
                        m = null;
                    }
                    if ( m != null ) {
                        cpa.printConfiguration(printWriter, m);
                    }
                }

                /**
                 * @see org.apache.felix.status.ZipAttachmentProvider#getAttachments()
                 */
                public URL[] getAttachments() {
                    return cpa.getAttachments();
                }
            }, props);
            synchronized ( this.registrations ) {
                this.registrations.put(reference, reg);
            }
        }
    }

    private final void remove(final ServiceReference reference) {
        final ServiceRegistration reg;
        synchronized ( this.registrations ) {
            reg = this.registrations.remove(reference);
        }
        if ( reg != null ) {
            reg.unregister();
        }
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(final ServiceReference reference) {
        final Object service = this.bundleContext.getService(reference);
        if ( service != null ) {
            this.add(reference, service);
        }
        return service;
    }
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(final ServiceReference reference, final Object service) {
        this.remove(reference);
        this.add(reference, service);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(final ServiceReference reference, final Object service) {
        this.remove(reference);
        this.bundleContext.ungetService(reference);
    }
}
