/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import org.apache.felix.connect.launch.ClasspathScanner;
import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.felix.connect.launch.PojoServiceRegistryFactory;

public class PojoServiceRegistryFactoryImpl implements PojoServiceRegistryFactory, FrameworkFactory
{

    public PojoServiceRegistry newPojoServiceRegistry(Map<String, Object> configuration) throws Exception
    {
        return new PojoSR(configuration);
    }

    public Framework newFramework(Map<String, String> configuration)
    {
        return new FrameworkImpl(configuration.get("pojosr.filter"));
    }

    private static final class FrameworkImpl implements Framework
    {
        private final String m_filter;
        private volatile Bundle m_bundle = null;
        private volatile PojoServiceRegistry m_reg = null;

        public FrameworkImpl(String filter)
        {
            m_filter = filter;
        }

        public void init() throws BundleException
        {
            try
            {
                m_reg = new PojoServiceRegistryFactoryImpl()
                        .newPojoServiceRegistry(new HashMap<String, Object>());
                m_bundle = m_reg.getBundleContext().getBundle();
            }
            catch (Exception ex)
            {
                throw new BundleException("Unable to scan classpath", ex);
            }
        }

        public int getState()
        {
            return (m_bundle == null) ? Bundle.INSTALLED : m_bundle.getState();
        }

        public void start(int options) throws BundleException
        {
            start();
        }

        public void start() throws BundleException
        {
            try
            {
                m_reg.startBundles((m_filter != null) ? new ClasspathScanner()
                        .scanForBundles(m_filter)
                        : new ClasspathScanner().scanForBundles());
            }
            catch (Exception e)
            {
                throw new BundleException("Error starting framework", e);
            }
        }

        public void stop(int options) throws BundleException
        {
            m_bundle.stop(options);
        }

        public void stop() throws BundleException
        {
            m_bundle.stop();
        }

        public void update(InputStream input) throws BundleException
        {
            m_bundle.update(input);
        }

        public void update() throws BundleException
        {
            m_bundle.update();
        }

        public void uninstall() throws BundleException
        {
            m_bundle.uninstall();
        }

        public Dictionary<String, String> getHeaders()
        {
            return m_bundle.getHeaders();
        }

        public long getBundleId()
        {
            return m_bundle.getBundleId();
        }

        public String getLocation()
        {
            return m_bundle.getLocation();
        }

        public ServiceReference[] getRegisteredServices()
        {
            return m_bundle.getRegisteredServices();
        }

        public ServiceReference[] getServicesInUse()
        {
            return m_bundle.getServicesInUse();
        }

        public boolean hasPermission(Object permission)
        {
            return m_bundle.hasPermission(permission);
        }

        public URL getResource(String name)
        {
            return m_bundle.getResource(name);
        }

        public Dictionary<String, String> getHeaders(String locale)
        {
            return m_bundle.getHeaders(locale);
        }

        public String getSymbolicName()
        {
            return m_bundle.getSymbolicName();
        }

        public Class loadClass(String name) throws ClassNotFoundException
        {
            return m_bundle.loadClass(name);
        }

        public Enumeration<URL> getResources(String name) throws IOException
        {
            return m_bundle.getResources(name);
        }

        public Enumeration<String> getEntryPaths(String path)
        {
            return m_bundle.getEntryPaths(path);
        }

        public URL getEntry(String path)
        {
            return m_bundle.getEntry(path);
        }

        public long getLastModified()
        {
            return m_bundle.getLastModified();
        }

        public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse)
        {
            return m_bundle.findEntries(path, filePattern, recurse);
        }

        public BundleContext getBundleContext()
        {
            return m_bundle.getBundleContext();
        }

        public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType)
        {
            return m_bundle.getSignerCertificates(signersType);
        }

        public Version getVersion()
        {
            return m_bundle.getVersion();
        }

        public FrameworkEvent waitForStop(long timeout)
                throws InterruptedException
        {
            final Object lock = new Object();

            m_bundle.getBundleContext().addBundleListener(new SynchronousBundleListener()
            {

                public void bundleChanged(BundleEvent event)
                {
                    if ((event.getBundle() == m_bundle) && (event.getType() == BundleEvent.STOPPED))
                    {
                        synchronized (lock)
                        {
                            lock.notifyAll();
                        }
                    }
                }
            });
            synchronized (lock)
            {
                while (m_bundle.getState() != Bundle.RESOLVED)
                {
                    if (m_bundle.getState() == Bundle.STOPPING)
                    {
                        lock.wait(100);
                    }
                    else
                    {
                        lock.wait();
                    }
                }
            }
            return new FrameworkEvent(FrameworkEvent.STOPPED, m_bundle, null);
        }

        public File getDataFile(String filename)
        {
            return m_bundle.getDataFile(filename);
        }

        public int compareTo(Bundle o)
        {
            if (o == this)
            {
                return 0;
            }
            return m_bundle.compareTo(o);
        }

        public <A> A adapt(Class<A> type)
        {
            return m_bundle.adapt(type);
        }

    }
}
