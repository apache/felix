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

import org.apache.felix.inventory.InventoryPrinterManager;
import org.apache.felix.inventory.impl.webconsole.WebConsoleAdapter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Activate bridges and register manager.
 */
public class Activator implements BundleActivator {

    private InventoryPrinterManagerImpl printerManager;

    private ServiceRegistration managerRegistration;

    private WebConsoleAdapter webAdapter;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        this.webAdapter = new WebConsoleAdapter(context);
        this.printerManager = new InventoryPrinterManagerImpl(context);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Felix Inventory Printer Manager");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        this.managerRegistration = context.registerService(
                InventoryPrinterManager.class.getName(),
                this.printerManager, props);
}

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        if( this.managerRegistration != null ) {
            this.managerRegistration.unregister();
            this.managerRegistration = null;
        }
        if ( this.printerManager != null ) {
            this.printerManager.dispose();
            this.printerManager = null;
        }
        if ( this.webAdapter != null ) {
            this.webAdapter.dispose();
            this.webAdapter = null;
        }
    }

}
