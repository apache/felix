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

import org.apache.felix.inventory.impl.webconsole.WebConsoleAdapter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Activate bridges and internal manager.
 */
public class Activator implements BundleActivator
{

    private static Object logService;

    private ServiceTracker logServiceTracker;

    private InventoryPrinterManagerImpl printerManager;

    private WebConsoleAdapter webAdapter;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception
    {
        this.logServiceTracker = new ServiceTracker(context, "org.osgi.service.log.LogService", null)
        {
            public Object addingService(ServiceReference reference)
            {
                Activator.logService = super.addingService(reference);
                return Activator.logService;
            }

            public void removedService(ServiceReference reference, Object service)
            {
                Activator.logService = null;
                super.removedService(reference, service);
            }
        };
        this.logServiceTracker.open();

        this.webAdapter = new WebConsoleAdapter(context);
        this.printerManager = new InventoryPrinterManagerImpl(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception
    {
        if (this.printerManager != null)
        {
            this.printerManager.dispose();
            this.printerManager = null;
        }
        if (this.webAdapter != null)
        {
            this.webAdapter.dispose();
            this.webAdapter = null;
        }
        Activator.logService = null;
        if (this.logServiceTracker != null)
        {
            this.logServiceTracker.close();
            this.logServiceTracker = null;
        }
    }

    public static void log(final ServiceReference sr, final int level, final String message, final Throwable exception)
    {
        Object logService = Activator.logService;
        if (logService != null)
        {
            ((LogService) logService).log(sr, level, message, exception);
        }
        else
        {
            final String code;
            switch (level)
            {
                case LogService.LOG_INFO:
                    code = "*INFO *";
                    break;

                case LogService.LOG_WARNING:
                    code = "*WARN *";
                    break;

                case LogService.LOG_ERROR:
                    code = "*ERROR*";
                    break;

                case LogService.LOG_DEBUG:
                default:
                    code = "*DEBUG*";
            }

            System.err.println(code + " " + message);
            if (exception != null)
            {
                exception.printStackTrace(System.out);
            }
        }
    }
}
