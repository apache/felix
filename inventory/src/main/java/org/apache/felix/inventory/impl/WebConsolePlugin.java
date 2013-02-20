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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.StatusPrinterHandler;
import org.apache.felix.inventory.StatusPrinterManager;
import org.apache.felix.inventory.impl.webconsole.ConsoleConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The web console plugin for a status printer.
 */
public class WebConsolePlugin extends AbstractWebConsolePlugin {

    private static final long serialVersionUID = 1L;

    /** Printer name. */
    private final String printerName;

    /**
     * Constructor
     * @param statusPrinterManager The status printer manager.
     * @param printerName The name of the printer this plugin is displaying.
     */
    WebConsolePlugin(final StatusPrinterManager statusPrinterManager,
            final String printerName) {
        super(statusPrinterManager);
        this.printerName = printerName;
    }

    @Override
    protected StatusPrinterHandler getStatusPrinterHandler() {
        return this.statusPrinterManager.getHandler(this.printerName);
    }

    public static ServiceRegistration register(
            final BundleContext context,
            final StatusPrinterManager manager,
            final StatusPrinterDescription desc) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(ConsoleConstants.PLUGIN_LABEL, "status-" + desc.getName());
        props.put(ConsoleConstants.PLUGIN_TITLE, desc.getTitle());
        props.put(ConsoleConstants.PLUGIN_CATEGORY, desc.getCategory() == null ? "Status" : desc.getCategory());
        return context.registerService(ConsoleConstants.INTERFACE_SERVLET, new ServiceFactory() {

            public void ungetService(final Bundle bundle, final ServiceRegistration registration,
                    final Object service) {
                // nothing to do
            }

            public Object getService(final Bundle bundle, final ServiceRegistration registration) {
                return new WebConsolePlugin(manager, desc.getName());
            }

        }, props);

    }
}