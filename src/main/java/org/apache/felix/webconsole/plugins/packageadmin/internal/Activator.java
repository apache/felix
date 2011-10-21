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
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator
{

    private ServiceTracker pkgAdminTracker;

    private ServiceRegistration pkgAdminPlugin;

    private ServiceRegistration depFinderPlugin;

    public void start(final BundleContext context) throws Exception
    {
        this.pkgAdminTracker = new ServiceTracker(context, "org.osgi.service.packageadmin.PackageAdmin", null);
        this.pkgAdminTracker.open();

        registerPackageAdminPlugin(context);
        registerDependencyFinderPlugin(context);
    }

    private void registerPackageAdminPlugin(final BundleContext context)
    {
        final PackageAdminPlugin plugin = new PackageAdminPlugin(context, pkgAdminTracker);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", PackageAdminPlugin.LABEL);
        props.put("felix.webconsole.title", PackageAdminPlugin.TITLE);
        props.put("felix.webconsole.configprinter.modes", new String[]
            { "zip", "txt" });
        this.pkgAdminPlugin = context.registerService("javax.servlet.Servlet", plugin, props);
    }

    private void registerDependencyFinderPlugin(final BundleContext context)
    {
        final DependencyFinderPlugin plugin = new DependencyFinderPlugin(context, pkgAdminTracker);
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", DependencyFinderPlugin.LABEL);
        props.put("felix.webconsole.title", DependencyFinderPlugin.TITLE);
        this.depFinderPlugin = context.registerService("javax.servlet.Servlet", plugin, props);
    }

    public void stop(final BundleContext context) throws Exception
    {
        if (this.pkgAdminPlugin != null)
        {
            this.pkgAdminPlugin.unregister();
            this.pkgAdminPlugin = null;
        }
        if (this.depFinderPlugin != null)
        {
            this.depFinderPlugin.unregister();
            this.depFinderPlugin = null;
        }
        if (this.pkgAdminTracker != null)
        {
            this.pkgAdminTracker.close();
            this.pkgAdminTracker = null;
        }
    }

}
