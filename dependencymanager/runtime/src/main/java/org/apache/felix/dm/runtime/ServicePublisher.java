/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.runtime;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Service;
import org.apache.felix.dm.ServiceStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * This class is injected in Service's Runnable fields which are annotated with the Publisher annotation.
 * This Runnable acts as a Service publisher, allowing the Service to take control of when the Service
 * is actually exposed from the OSGi registry.
 */
public class ServicePublisher
{
    private final AtomicBoolean m_published = new AtomicBoolean(false);
    private Service m_srv;
    private BundleContext m_bc;
    private String[] m_services;
    private Dictionary<String, Object> m_properties;
    private volatile ServiceRegistration m_registration;
    private String m_publisherField;
    private String m_unpublisherField;

    /**
     * Class constructor.
     * @param publisherField The Service field name annotated with the Publisher annotation
     * @param unpublisherField the Servicel field where to inject a Runnable for unregistering the Service
     * @param srv the Service object where to inject the Runnables (we'll use defaultImplementation ...)
     * @param bc the Service bundle context
     * @param services the list of provided services which will be registered once our Runnable is invoked
     * @param props the published service properties.
     */
    public ServicePublisher(String publisherField, String unpublisherField, Service srv, BundleContext bc, String[] services, Dictionary<String, Object> props)
    {
        m_publisherField = publisherField;
        m_unpublisherField = unpublisherField;
        m_srv = srv;
        m_bc = bc;
        m_services = services;
        m_properties = props;
    }

    public void register(DependencyManager dm)
    {
        Log.instance().log(LogService.LOG_DEBUG, "registering Publisher for services %s", 
                           Arrays.toString(m_services));
        
        // First, store the service properties in the service itself. We do this because when
        // our lifecycle handler will invoke the service's start callback, it will eventually
        // append the eventual properties returned by the start() method into our current service
        // properties. 
        m_srv.setServiceProperties(m_properties);
        
        Publisher publisher = new Publisher();
        m_srv.add(dm.createServiceDependency()
                  .setService(Runnable.class, "(dm.publisher=" + System.identityHashCode(this) + ")")
                  .setRequired(false)
                  .setAutoConfig(m_publisherField)
                  .setDefaultImplementation(publisher));
        m_srv.addStateListener(publisher);

        if (m_unpublisherField != null)
        {
            Unpublisher unpublisher = new Unpublisher();
            m_srv.add(dm.createServiceDependency()
                  .setService(Runnable.class, "(dm.unpublisher=" + System.identityHashCode(this) + ")")
                  .setRequired(false)
                  .setAutoConfig(m_unpublisherField)
                  .setDefaultImplementation(unpublisher));
        }
    }

    private class Publisher implements Runnable, ServiceStateListener
    {
        private boolean m_started; // true if the service has started
        
        public void run()
        {
            if (m_published.compareAndSet(false, true))
            {
                // Only register the service if it has been started. Otherwise delay the registration
                // until the service start callback has been invoked.
                synchronized (this) {
                    if (! m_started)
                    {
                        Log.instance().log(LogService.LOG_DEBUG, "Delaying service publication for services %s (service not yet started)",
                                           Arrays.toString(m_services));

                        return;
                    }
                }
                publish();
            }
        }

        public void starting(Service service)
        {
        }

        public void started(Service service)
        {
            synchronized (this)
            {
                m_started = true;
            }
            if (m_published.get())
            {
                // Our runnable has been invoked before the service start callback has been called: 
                // Now that we are started, we fire the service registration.
                publish();
            }
        }

        public void stopping(Service service)
        {
            synchronized (this)
            {
                m_started = false;
            }

            if (m_published.compareAndSet(true, false))
            {
                if (m_registration != null)
                {
                    Log.instance().log(LogService.LOG_DEBUG, "unpublishing services %s (service is stopping)",
                                       Arrays.toString(m_services));

                    m_registration.unregister();
                    m_registration = null;
                }
            }
        }

        public void stopped(Service service)
        {
        }
        
        private void publish()
        {
            try
            {
                Log.instance().log(LogService.LOG_DEBUG, "publishing services %s",
                                   Arrays.toString(m_services));
                m_registration = m_bc.registerService(m_services, m_srv.getService(), m_srv.getServiceProperties());
            }
            catch (Throwable t)
            {
                m_published.set(false);
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                else
                {
                    throw new RuntimeException("Could not register services", t);
                }
            }
        }
    }

    private class Unpublisher implements Runnable
    {
        public void run()
        {
            if (m_published.compareAndSet(true, false))
            {
                if (m_registration != null)
                {
                    Log.instance().log(LogService.LOG_DEBUG, "unpublishing services %s",
                                       Arrays.toString(m_services));
                    m_registration.unregister();
                    m_registration = null;
                }
            }
        }
    }
}
