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
package org.apache.felix.webconsole.plugins.subsystem.internal;

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator
{
    private SimpleWebConsolePlugin plugin;

    public void start(BundleContext context) throws Exception
    {
        plugin = new WebConsolePlugin(context).register();
    }

    public void stop(BundleContext context) throws Exception
    {
        plugin = null;
    }

    /*
    public synchronized URLStreamHandlerService addingService(ServiceReference<URLStreamHandlerService> reference)
    {
        SimpleWebConsolePlugin p = plugin;
        if (p == null)
        {
            URLStreamHandlerService svc = bundleContext.getService(reference);
            plugin = new WebConsolePlugin(svc).register(bundleContext);
        }
        return bundleContext.getService(reference);
    }

    public void modifiedService(ServiceReference<URLStreamHandlerService> reference, URLStreamHandlerService service)
    {
        // Unused
    }

    public synchronized void removedService(ServiceReference<URLStreamHandlerService> reference, URLStreamHandlerService service)
    {
        if (plugin != null)
        {
            plugin.unregister();
            plugin = null;
        }
    }
    */
}

//public class Activator implements BundleActivator
//{
//    private SubsystemPluginServlet plugin;
//
//    public void start(final BundleContext context) throws Exception
//    {
//        plugin = new SubsystemPluginServlet();
//
////        // now we create the listener
////        this.eventListener = new EventListener(this.plugin, context);
////
////        // and the optional features handler
////        this.featuresHandler = new OptionalFeaturesHandler(this.plugin, context);
//
//        // finally we register the plugin
//        final Dictionary props = new Hashtable();
//        props.put( Constants.SERVICE_DESCRIPTION, "Subsystems Plugin for the Apache Felix Web Console" );
//        props.put( "felix.webconsole.label", "subsystems");
//        props.put( "felix.webconsole.title", "Subsystems");
////        props.put( "felix.webconsole.css", "/events/res/ui/events.css");
//        props.put( "felix.webconsole.category", "OSGi");
//        context.registerService(Servlet.class.getName(),
//                                plugin,
//                                props);
//    }
//
//    /**
//     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
//     */
//    public void stop(final BundleContext context) throws Exception
//    {
////        if ( this.pluginRegistration != null )
////        {
////            this.pluginRegistration.unregister();
////            this.pluginRegistration = null;
////        }
////        if ( this.eventListener != null )
////        {
////            this.eventListener.destroy();
////            eventListener = null;
////        }
////        if ( this.featuresHandler != null)
////        {
////            this.featuresHandler.destroy();
////            this.featuresHandler = null;
////        }
////        if ( this.plugin != null ) {
////            this.plugin.destroy();
////            this.plugin = null;
////        }
//    }
//}
