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
package org.apache.felix.framework;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * This test performs concurrent registration of service components that have dependencies between each other.
 * There are 10 components. The first one has an optional dependency on the second one, the second on the third , etc ... The last component
 * does not have any dependencies.
 * At the beginning of a concurrent test, the nth component creates a service tracker on the nth+1 component and registers in the registry
 * (and components and their associated Service Trackers are registered/opened concurrently).
 * At the end of an iteration test, we check that all nth component are properly injected (satisfied) with the nth+1 component (except the 
 * last one which has no dependency).
 */
public class ConcurrencyTest extends TestCase
{
    public static final int DELAY = 1000;
    final static int NPROCS = Runtime.getRuntime().availableProcessors();
    final static int COMPONENTS = (NPROCS == 1) ? 4 : NPROCS;
    final static int ITERATIONS = 50000;

    /**
     * Threadpool which can be optionally used by parallel scenarios.
     */
    private final static Executor TPOOL = Executors.newFixedThreadPool(COMPONENTS);

    /**
     * Latch used to ensure that the test has completed propertly.
     */
    private final CountDownLatch m_testDone = new CountDownLatch(1);

    /**
     * Starts a concurrent test.
     */
    public void testConcurrentComponents() throws Exception
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0," + "org.osgi.service.packageadmin; version=1.2.0,"
                + "org.osgi.service.startlevel; version=1.1.0," + "org.osgi.util.tracker; version=1.3.3,"
                + "org.osgi.service.url; version=1.0.0");
        File cacheDir = File.createTempFile("felix-cache", ".dir");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        Framework f = new Felix(params);
        f.init();
        f.start();

        try
        {
            out.println("Starting load test.");
            Loader loader = new Loader(f.getBundleContext());
            loader.start();

            Assert.assertTrue(m_testDone.await(60, TimeUnit.SECONDS));
            loader.stop();
        }
        finally
        {
            f.stop();
            Thread.sleep(DELAY);
            deleteDir(cacheDir);
        }
    }

    private static void deleteDir(File root) throws IOException
    {
        if (root.isDirectory())
        {
            for (File file : root.listFiles())
            {
                deleteDir(file);
            }
        }
        assertTrue(root.delete());
    }

    /**
     * A simple component, that creates a service tracker on another component and registers in the osgi service registry.
     */
    public class Component implements ServiceTrackerCustomizer<Component, Component>
    {
        private final BundleContext m_ctx;
        private final int m_id;
        private volatile int m_dependsOnId = -1;
        private volatile ServiceTracker<Component, Component> m_tracker;
        private volatile boolean m_satisfied;
        private volatile ServiceRegistration<?> m_registration;

        public Component(BundleContext ctx, int id)
        {
            m_ctx = ctx;
            m_id = id;
        }

        public void dependsOn(int componentId)
        {
            m_dependsOnId = componentId;
        }

        public void start()
        {
            // if we depends on another component, add the dependency.
            if (m_dependsOnId != -1)
            {
                Filter filter;
                try
                {
                    filter = m_ctx.createFilter(
                        "(&(objectClass=" + Component.class.getName() + ")(id=" + m_dependsOnId + "))");
                }
                catch (InvalidSyntaxException e)
                {
                    e.printStackTrace();
                    return;
                }
                m_tracker = new ServiceTracker<Component, Component>(m_ctx, filter, this);
                m_tracker.open();
            }
            else
            {
                // We don't depend on anything, mark our satisfied flag to true
                m_satisfied = true;
            }
            register();
        }

        public boolean isSatisfied()
        {
            return m_satisfied;
        }

        public void stop()
        {
            if (m_tracker != null)
            {
                m_tracker.close();
            }
            m_registration.unregister();
        }

        public Component addingService(ServiceReference<Component> reference)
        {
            Component service = m_ctx.getService(reference);
            String id = (String) reference.getProperty("id");
            if (String.valueOf(m_dependsOnId).equals(id))
            {
                m_satisfied = true;
            }
            else
            {
                System.err.println("Component#" + m_id + " received wrong dependency #" + id);
            }

            return service;
        }

        public void modifiedService(ServiceReference<Component> reference, Component service)
        {
        }

        public void removedService(ServiceReference<Component> reference, Component service)
        {
            try
            {
                m_ctx.ungetService(reference);
            }
            catch (IllegalStateException e)
            {
                e.printStackTrace();
            }
        }
        
        private void register() {
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put("id", String.valueOf(m_id));
            m_registration = m_ctx.registerService(Component.class.getName(), this, properties);
        }
    }

    public class Loader implements Runnable
    {
        final BundleContext m_ctx;
        private Thread m_thread;

        Loader(BundleContext ctx)
        {
            m_ctx = ctx;
        }

        public void start()
        {
            m_thread = new Thread(this);
            m_thread.start();
        }

        public void stop()
        {
            m_thread.interrupt();
        }

        public void run()
        {
            // Creates all components. Each nth components will depends on the
            // nth+1 component. The last component does not depend on another one.

            out.println("Starting Concurrency test ...");
            for (int i = 0; i < ITERATIONS; i++)
            {
                try
                {
                    if (i % 1000 == 0)
                    {
                        out.println(".");
                    }
                    createComponentsConcurrently(i);
                }
                catch (Throwable error)
                {
                    err.println("Test failed");
                    error.printStackTrace(err);
                    return;
                }
            }

            out.println("\nTest successful.");
            m_testDone.countDown();
        }

        private void createComponentsConcurrently(int iteration) throws Exception
        {
            // Create components.
            final List<Component> components = new ArrayList<Component>();
            for (int i = 0; i < COMPONENTS; i++)
            {
                components.add(createComponents(iteration, i));
            }

            // Start all components asynchronously.
            final CountDownLatch latch = new CountDownLatch(components.size());
            for (int i = 0; i < COMPONENTS; i++)
            {
                final Component component = components.get(i);
                TPOOL.execute(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            component.start();
                        }
                        finally
                        {
                            latch.countDown();
                        }

                    }
                });
            }

            if (!latch.await(5, TimeUnit.SECONDS))
            {
                System.err.println("ThreadPool did not complete timely.");
                return;
            }

            // Count the number of satisfied components.
            long satisfied = 0;
            for (int i = 0; i < COMPONENTS; i++)
            {
                satisfied += (components.get(i).isSatisfied()) ? 1 : 0;
            }

            // Report an error if we don't have expected satisfied components.
            if (satisfied != COMPONENTS) {
                for (int i = 0; i < COMPONENTS; i ++) {
                    if (! components.get(i).isSatisfied()) {
                        out.println("Component #" + i + " unsatisfied.");
                    }
                }
                Assert.fail("Found unsatisfied components: " + String.valueOf(COMPONENTS - satisfied));
                return;
            }            

            // Stop all components
            for (int i = 0; i < COMPONENTS; i++)
            {
                components.get(i).stop();
            }
        }

        private Component createComponents(int iteration, int i)
        {
            Component component = new Component(m_ctx, iteration + i);
            if (i < (COMPONENTS - 1))
            {
                component.dependsOn(iteration + i + 1);
            }
            return component;
        }
    }
}
