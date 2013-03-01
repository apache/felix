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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.impl.webconsole.ConsoleConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The web console plugin for a inventory printer.
 */
public class WebConsolePlugin extends AbstractWebConsolePlugin
{

    private static final long serialVersionUID = 1L;

    /** Printer name. */
    private final String printerName;

    /**
     * Constructor
     * 
     * @param inventoryPrinterManager The inventory printer manager.
     * @param printerName The name of the printer this plugin is displaying.
     */
    WebConsolePlugin(final InventoryPrinterManagerImpl inventoryPrinterManager, final String printerName)
    {
        super(inventoryPrinterManager);
        this.printerName = printerName;
    }

    protected InventoryPrinterHandler getInventoryPrinterHandler()
    {
        return this.inventoryPrinterManager.getHandler(this.printerName);
    }

    public static ServiceRegistration register(final BundleContext context, final InventoryPrinterManagerImpl manager,
        final InventoryPrinterDescription desc)
    {
        final Dictionary props = new Hashtable();
        props.put(ConsoleConstants.PLUGIN_LABEL, "status-" + desc.getName());
        props.put(ConsoleConstants.PLUGIN_TITLE, desc.getTitle());
        props.put(ConsoleConstants.PLUGIN_CATEGORY, ConsoleConstants.WEB_CONSOLE_CATEGORY);
        return context.registerService(ConsoleConstants.INTERFACE_SERVLET, new ServiceFactory()
        {

            public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service)
            {
                // nothing to do
            }

            public Object getService(final Bundle bundle, final ServiceRegistration registration)
            {
                return new WebConsolePlugin(manager, desc.getName());
            }

        }, props);

    }
}