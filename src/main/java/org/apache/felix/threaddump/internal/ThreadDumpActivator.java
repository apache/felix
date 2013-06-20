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
package org.apache.felix.threaddump.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.InventoryPrinter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGi bundle activator which takes care of (un)binding the
 * Thread Dump Inventory Printer service.
 */
public final class ThreadDumpActivator implements BundleActivator
{

    /**
     * The service description.
     */
    private static final String SERVICE_TITLE = "Apache Felix Thread Dump";

    /**
     * The service identifier.
     */
    private static final String SERVICE_NAME = "threaddump";

    /**
     * The ThreadDumper ServiceRegistration reference.
     */
    private ServiceRegistration threadDumperRegistration;

    public void start(BundleContext context)
    {
        final Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_VENDOR, context.getBundle().getHeaders(Constants.BUNDLE_VENDOR));
        props.put(Constants.SERVICE_DESCRIPTION, SERVICE_TITLE);
        props.put(InventoryPrinter.NAME, SERVICE_NAME);
        props.put(InventoryPrinter.TITLE, SERVICE_TITLE);

        threadDumperRegistration = context.registerService(InventoryPrinter.SERVICE, new ThreadDumpInventoryPrinter(),
            props);
    }

    public void stop(BundleContext context)
    {
        threadDumperRegistration.unregister();
    }

}
