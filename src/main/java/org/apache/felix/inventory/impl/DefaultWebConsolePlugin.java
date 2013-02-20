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
package org.apache.felix.inventory.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.zip.ZipOutputStream;

import org.apache.felix.inventory.InventoryPrinterHandler;
import org.apache.felix.inventory.InventoryPrinterManager;
import org.apache.felix.inventory.PrinterMode;
import org.apache.felix.inventory.impl.webconsole.ConsoleConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The web console plugin for a inventory printer.
 */
public class DefaultWebConsolePlugin extends AbstractWebConsolePlugin implements InventoryPrinterHandler {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * @param inventoryPrinterAdapter The adapter
     */
    DefaultWebConsolePlugin(final InventoryPrinterManager inventoryPrinterManager) {
        super(inventoryPrinterManager);
    }

    @Override
    protected InventoryPrinterHandler getInventoryPrinterHandler() {
        return this;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinterHandler#getTitle()
     */
    public String getTitle() {
        return "Overview";
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinterHandler#getName()
     */
    public String getName() {
        return "config";
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinterHandler#getCategory()
     */
    public String getCategory() {
        return "Inventory";
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinterHandler#getModes()
     */
    public PrinterMode[] getModes() {
        return new PrinterMode[] {PrinterMode.TEXT};
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinterHandler#supports(org.apache.felix.inventory.PrinterMode)
     */
    public boolean supports(final PrinterMode mode) {
        return mode == PrinterMode.TEXT;
    }

    /**
     * @see org.apache.felix.inventory.InventoryPrinter#print(org.apache.felix.inventory.PrinterMode, java.io.PrintWriter)
     */
    public void print(final PrinterMode mode, final PrintWriter printWriter) {
        final InventoryPrinterHandler[] handlers = this.inventoryPrinterManager.getAllHandlers();
        printWriter.print("Currently registered ");
        printWriter.print(String.valueOf(handlers.length));
        printWriter.println(" inventory printer.");
        printWriter.println();
        for(final InventoryPrinterHandler handler : handlers) {
            printWriter.println(handler.getTitle());
        }
    }

    /**
     * @see org.apache.felix.inventory.ZipAttachmentProvider#addAttachments(java.lang.String, java.util.zip.ZipOutputStream)
     */
    public void addAttachments(String namePrefix, ZipOutputStream zos)
    throws IOException {
        // no attachments support
    }

    public static ServiceRegistration register(final BundleContext context,
            final InventoryPrinterManager manager) {
        final DefaultWebConsolePlugin dwcp = new DefaultWebConsolePlugin(manager);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ConsoleConstants.PLUGIN_LABEL, dwcp.getName());
        props.put(ConsoleConstants.PLUGIN_TITLE, dwcp.getTitle());
        props.put(ConsoleConstants.PLUGIN_CATEGORY, dwcp.getCategory());
        return context.registerService(ConsoleConstants.INTERFACE_SERVLET, new ServiceFactory() {

            public void ungetService(final Bundle bundle, final ServiceRegistration registration,
                    final Object service) {
                // nothing to do
            }

            public Object getService(final Bundle bundle, final ServiceRegistration registration) {
                return dwcp;
            }

        }, props);
    }
}