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
package org.apache.felix.inventory.impl.webconsole;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The web console adapter registers web console status printers
 * as inventory printers.
 */
public class WebConsoleAdapter implements ServiceTrackerCustomizer
{

    private final BundleContext bundleContext;

    private final ServiceTracker cfgPrinterTracker;

    private final Map registrations = new HashMap();

    private final ResourceBundleManager rbManager;

    public WebConsoleAdapter(final BundleContext btx) throws InvalidSyntaxException
    {
        this.bundleContext = btx;
        this.rbManager = new ResourceBundleManager(btx);
        this.cfgPrinterTracker = new ServiceTracker(this.bundleContext, this.bundleContext.createFilter("(|("
            + Constants.OBJECTCLASS + "=" + ConsoleConstants.INTERFACE_CONFIGURATION_PRINTER + ")" + "(&("
            + ConsoleConstants.PLUGIN_LABEL + "=*)(&(" + ConsoleConstants.PLUGIN_TITLE + "=*)("
            + ConsoleConstants.CONFIG_PRINTER_MODES + "=*))))"), this);
        this.cfgPrinterTracker.open();
    }

    /**
     * Dispose this service
     */
    public void dispose()
    {
        this.cfgPrinterTracker.close();
        final List regs = new ArrayList();
        synchronized (this.registrations)
        {
            regs.addAll(this.registrations.values());
            this.registrations.clear();
        }
        final Iterator i = regs.iterator();
        while (i.hasNext())
        {
            final ServiceRegistration reg = (ServiceRegistration) i.next();
            reg.unregister();
        }
        this.rbManager.dispose();
    }

    private void add(final ServiceReference reference, final Object service)
    {
        final ConfigurationPrinterAdapter cpa = ConfigurationPrinterAdapter.createAdapter(service, reference);
        if (cpa != null && cpa.title != null)
        {
            if (cpa.title.startsWith("%"))
            {
                String title = cpa.title.substring(1);

                final ResourceBundle rb = this.rbManager.getResourceBundle(reference.getBundle());
                if (rb != null)
                {
                    try
                    {
                        title = rb.getString(title);
                    }
                    catch (Exception e)
                    {
                        // ClassCastException, MissingResourceException
                        // ignore
                    }
                }
                cpa.title = title;
            }
            if (cpa.label == null)
            {
                cpa.label = cpa.title;
            }
            final Dictionary props = new Hashtable();
            props.put(InventoryPrinter.NAME, cpa.label);
            props.put(InventoryPrinter.TITLE, cpa.title);
            props.put(InventoryPrinter.FORMAT, cpa.getPrinterModes());

            final ServiceRegistration reg = this.bundleContext.registerService(InventoryPrinter.class.getName(),
                new WebConsolePrinter(cpa), props);
            synchronized (this.registrations)
            {
                this.registrations.put(reference, reg);
            }
        }
    }

    private final void remove(final ServiceReference reference)
    {
        final ServiceRegistration reg;
        synchronized (this.registrations)
        {
            reg = (ServiceRegistration) this.registrations.remove(reference);
        }
        if (reg != null)
        {
            reg.unregister();
        }
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public Object addingService(final ServiceReference reference)
    {
        final Object service = this.bundleContext.getService(reference);
        if (service != null)
        {
            this.add(reference, service);
        }
        return service;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public void modifiedService(final ServiceReference reference, final Object service)
    {
        this.remove(reference);
        this.add(reference, service);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public void removedService(final ServiceReference reference, final Object service)
    {
        this.remove(reference);
        this.bundleContext.ungetService(reference);
    }

    private static class WebConsolePrinter implements InventoryPrinter, ZipAttachmentProvider
    {

        final ConfigurationPrinterAdapter cpa;

        public WebConsolePrinter(final ConfigurationPrinterAdapter cpa)
        {
            this.cpa = cpa;
        }

        /**
         * @see org.apache.felix.inventory.InventoryPrinter#print(org.apache.felix.inventory.Format,
         *      java.io.PrintWriter)
         */
        public void print(final PrintWriter printWriter, final Format format, final boolean isZip)
        {
            final String m;
            if (!isZip && format == Format.HTML)
            {
                m = ConsoleConstants.MODE_WEB;
            }
            else if (!isZip && format == Format.TEXT)
            {
                m = ConsoleConstants.MODE_TXT;
            }
            else if (isZip && (format == Format.TEXT || format == Format.HTML))
            {
                m = ConsoleConstants.MODE_ZIP;
            }
            else
            {
                m = null;
            }
            if (m != null)
            {
                cpa.printConfiguration(printWriter, m);
            }
        }

        /**
         * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.util.zip.ZipOutputStream,
         *      java.lang.String)
         */
        public void addAttachments(final ZipOutputStream zos, final String namePrefix) throws IOException
        {
            final URL[] attachments = cpa.getAttachments();
            if (attachments != null)
            {
                for (int i = 0; i < attachments.length; i++)
                {
                    final URL current = attachments[i];
                    final String path = current.getPath();
                    final String name;
                    if (path == null || path.length() == 0)
                    {
                        // sanity code, we should have a path, but if not let's
                        // just create some random name
                        name = "file" + Double.doubleToLongBits(Math.random());
                    }
                    else
                    {
                        final int pos = path.lastIndexOf('/');
                        name = (pos == -1 ? path : path.substring(pos + 1));
                    }
                    final ZipEntry entry = new ZipEntry(namePrefix + name);
                    zos.putNextEntry(entry);
                    final InputStream is = current.openStream();
                    try
                    {
                        byte[] buffer = new byte[4096];
                        int n = 0;
                        while (-1 != (n = is.read(buffer)))
                        {
                            zos.write(buffer, 0, n);
                        }
                    }
                    finally
                    {
                        if (is != null)
                        {
                            try
                            {
                                is.close();
                            }
                            catch (final IOException ignore)
                            {
                            }
                        }
                    }
                    zos.closeEntry();
                }
            }
        }

    }
}
