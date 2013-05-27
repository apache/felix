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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.ZipAttachmentProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Helper class for a inventory printer.
 *
 * The adapter simplifies accessing and working with the inventory printer.
 */
public class InventoryPrinterAdapter implements InventoryPrinterHandler, Comparable
{

    /**
     * Comparator for adapters based on the service ranking.
     */
    public static final Comparator RANKING_COMPARATOR = new Comparator()
    {

        public int compare(final Object o1, final Object o2)
        {
            return ((InventoryPrinterAdapter) o1).description.compareTo(((InventoryPrinterAdapter) o2).description);
        }
    };

    /** The Inventory printer service. */
    private final InventoryPrinter printer;

    /** The printer description. */
    private final InventoryPrinterDescription description;

    /** Service registration for the web console. */
    private ServiceRegistration registration;

    /**
     * Constructor.
     */
    public InventoryPrinterAdapter(final InventoryPrinterDescription description, final InventoryPrinter printer)
    {
        this.description = description;
        this.printer = printer;
    }

    public void registerConsole(final BundleContext context, final InventoryPrinterManagerImpl manager)
    {
        if (this.registration == null)
        {
            final Object value = this.description.getServiceReference().getProperty(InventoryPrinter.WEBCONSOLE);
            if (value == null || !"false".equalsIgnoreCase(value.toString()))
            {
                this.registration = WebConsolePlugin.register(context, manager, this.description);
            }
        }
    }

    public void unregisterConsole()
    {
        if (this.registration != null)
        {
            this.registration.unregister();
            this.registration = null;
        }
    }

    /**
     * The human readable title for the inventory printer.
     */
    public String getTitle()
    {
        return this.description.getTitle();
    }

    /**
     * The unique name of the printer.
     */
    public String getName()
    {
        return this.description.getName();
    }

    /**
     * All supported formats.
     */
    public Format[] getFormats()
    {
        return this.description.getFormats();
    }

    /**
     * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.util.zip.ZipOutputStream,
     *      java.lang.String)
     */
    public void addAttachments(final ZipOutputStream zos, final String namePrefix) throws IOException
    {
        if (printer instanceof ZipAttachmentProvider)
        {
            ((ZipAttachmentProvider) printer).addAttachments(zos, namePrefix);
        }
    }

    /**
     * Whether the printer supports this format.
     */
    public boolean supports(final Format format)
    {
        for (int i = 0; i < this.description.getFormats().length; i++)
        {
            if (this.description.getFormats()[i] == format)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(org.apache.felix.inventory.Format,
     *      java.io.PrintWriter)
     */
    public void print(final PrintWriter printWriter, final Format format, final boolean isZip)
    {
        if (this.supports(format))
        {
            this.printer.print(printWriter, format, isZip);
        }
    }

    public InventoryPrinterDescription getDescription()
    {
        return this.description;
    }

    public int compareTo(final Object spa)
    {
        return this.description.getSortKey().compareTo(((InventoryPrinterAdapter) spa).description.getSortKey());
    }

    public int hashCode()
    {
        return this.description.getSortKey().hashCode();
    }

    public boolean equals(final Object spa)
    {
        return this.description.getSortKey().equals(((InventoryPrinterAdapter) spa).description.getSortKey());
    }

    public String toString()
    {
        return printer.getClass() + "(" + super.toString() + ")";
    }
}
