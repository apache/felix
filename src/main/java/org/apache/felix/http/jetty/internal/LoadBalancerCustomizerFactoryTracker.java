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
package org.apache.felix.http.jetty.internal;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.http.jetty.LoadBalancerCustomizerFactory;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class LoadBalancerCustomizerFactoryTracker extends ServiceTracker<LoadBalancerCustomizerFactory, ServiceReference<LoadBalancerCustomizerFactory>>
{

    private final CustomizerWrapper customizerWrapper;
    private final BundleContext bundleContext;
    private final SortedSet<ServiceReference<LoadBalancerCustomizerFactory>> set = new TreeSet<ServiceReference<LoadBalancerCustomizerFactory>>();

    public LoadBalancerCustomizerFactoryTracker(final BundleContext context, final CustomizerWrapper customizerWrapper)
    {
        super(context, LoadBalancerCustomizerFactory.class.getName(), null);
        this.bundleContext =context;
        this.customizerWrapper = customizerWrapper;
    }

    @Override
    public ServiceReference<LoadBalancerCustomizerFactory> addingService(final ServiceReference<LoadBalancerCustomizerFactory> reference)
    {
        super.addingService(reference);

        final ServiceReference<LoadBalancerCustomizerFactory> highestReference;
        synchronized (set)
        {
            set.add(reference);
            highestReference = set.last();
        }

        boolean updated = false;
        if (highestReference != null)
        {
            final LoadBalancerCustomizerFactory factory = bundleContext.getService(highestReference);
            if (factory != null)
            {
                final Customizer customizer = factory.createCustomizer();
                customizerWrapper.setCustomizer(customizer);
                updated = true;
            }
        }
        // something went wrong, null out wrapper
        if ( !updated )
        {
            customizerWrapper.setCustomizer(null);
        }

        return reference;
    }

    @Override
    public void removedService(final ServiceReference<LoadBalancerCustomizerFactory> reference, final ServiceReference<LoadBalancerCustomizerFactory> service)
    {
        super.removedService(reference, service);

        final ServiceReference<LoadBalancerCustomizerFactory> highestReference;
        synchronized (set)
        {
            set.remove(reference);
            highestReference = set.last();
        }

        boolean updated = false;
        if (highestReference != null)
        {
            //update the customizer Wrapper
            final LoadBalancerCustomizerFactory factory = bundleContext.getService(highestReference);
            if (factory != null)
            {
                final Customizer customizer = factory.createCustomizer();
                customizerWrapper.setCustomizer(customizer);
                updated = true;
            }
        }
        // something went wrong / or no service registered anymore, null out wrapper
        if ( !updated )
        {
            customizerWrapper.setCustomizer(null);
        }
    }
}
