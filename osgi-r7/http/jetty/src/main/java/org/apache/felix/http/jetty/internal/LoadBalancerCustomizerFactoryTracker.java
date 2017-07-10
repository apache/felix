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
        this.bundleContext = context;
        this.customizerWrapper = customizerWrapper;
    }

    @Override
    public ServiceReference<LoadBalancerCustomizerFactory> addingService(final ServiceReference<LoadBalancerCustomizerFactory> reference)
    {
        final ServiceReference<LoadBalancerCustomizerFactory> highestReference;
        synchronized (set)
        {
            set.add(reference);
            highestReference = set.last();
        }

        // only change if service is higher
        if ( highestReference.compareTo(reference) == 0 )
        {
            boolean updated = false;
            final LoadBalancerCustomizerFactory factory = bundleContext.getService(reference);
            if (factory != null)
            {
                final Customizer customizer = factory.createCustomizer();
                if ( customizer != null )
                {
                    customizerWrapper.setCustomizer(customizer);
                    updated = true;
                }
                else
                {
                    bundleContext.ungetService(reference);
                }
            }
            if ( !updated)
            {
                // we can't get the service, remove reference
                synchronized (set)
                {
                    set.remove(reference);
                }
                return null;
            }
        }
        return reference;
    }

    @Override
    public void removedService(final ServiceReference<LoadBalancerCustomizerFactory> reference, final ServiceReference<LoadBalancerCustomizerFactory> service)
    {
        final boolean change;
        ServiceReference<LoadBalancerCustomizerFactory> highestReference = null;
        synchronized (set)
        {
            if (set.isEmpty())
            {
                change = false;
            }
            else
            {
                change = set.last().compareTo(reference) == 0;
            }
            set.remove(reference);
            highestReference = (set.isEmpty() ? null : set.last());
        }

        if (change)
        {
            boolean done = false;

            do
            {
                // update the customizer Wrapper
                if ( highestReference == null )
                {
                    customizerWrapper.setCustomizer(null);
                    done = true;
                }
                else
                {
                    final LoadBalancerCustomizerFactory factory = bundleContext.getService(highestReference);
                    if (factory != null)
                    {
                        final Customizer customizer = factory.createCustomizer();
                        if ( customizer != null )
                        {
                            customizerWrapper.setCustomizer(customizer);
                            done = true;
                        }
                        else
                        {
                            bundleContext.ungetService(highestReference);
                        }
                    }

                    if ( !done )
                    {
                        synchronized ( set )
                        {
                            set.remove(highestReference);
                            highestReference = (set.isEmpty() ? null : set.last());
                        }
                    }
                }
            } while ( !done);
            bundleContext.ungetService(reference);
        }
    }
}
