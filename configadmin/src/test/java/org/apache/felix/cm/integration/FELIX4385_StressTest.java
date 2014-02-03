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
package org.apache.felix.cm.integration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * The <code>FELIX4385_StressTest</code> class tests the issue related to concurrency between configuration 
 * creation/update/removal and ManagedService registration/unregistration.
 * The test performs some loops, each one is then executing the following scenario:
 * Some ManagedServices are concurrently registered in the OSGi registry using an Executor, and for each 
 * managed service, we create a Configuration.
 * We then wait until every managed services have been updated with a non null configuration. Care is taken when a 
 * ManagedService is called with an initial update(null) callback, because when a configuration is created the very first
 * time, an empty configuration is delivered to the corresponding managed service until the configuration is really updated.
 * Once all managed services have been updated, we then concurrently unregister the managed services, and we also
 * delete every created configurations. We don't use an executor when deleting configuration because the configuration 
 * removal is already asynchronous.
 * 
 * <p>
 * @see <a href="https://issues.apache.org/jira/browse/FELIX-4385">FELIX-4385</a>
 */
@RunWith(JUnit4TestRunner.class)
public class FELIX4385_StressTest extends ConfigurationTestBase
{
    final static int MAXWAIT = 10000;
    final static int MANAGED_SERVICES = 3;
    volatile ExecutorService executor;

    @Test
    public void test_ConcurrentManagedServicesWithConcurrentConfigurations()
    {
        final Log log = new Log(bundleContext);
        log.info("starting test_ConcurrentManagedServicesWithConcurrentConfigurations");
        // Use at least 10 parallel threads, or take all available processors if the running host contains more than 10 processors.
        int parallelism = Math.max(10, Runtime.getRuntime().availableProcessors());
        final ConfigurationAdmin ca = getConfigurationAdmin();
        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try
        {
            int pidCounter = 0;

            long timeStamp = System.currentTimeMillis();
            for (int loop = 0; loop < 1000; loop++)
            {
                log.debug("loop#%d -------------------------", (loop + 1));

                final CountDownLatch managedServiceUpdated = new CountDownLatch(MANAGED_SERVICES);
                final CountDownLatch managedServiceUnregistered = new CountDownLatch(MANAGED_SERVICES);

                // Create some ManagedServices concurrently
                log.info("registering aspects concurrently");
                final CopyOnWriteArrayList<ServiceRegistration> managedServices = new CopyOnWriteArrayList<ServiceRegistration>();
                final CopyOnWriteArrayList<Configuration> confs = new CopyOnWriteArrayList<Configuration>();

                for (int i = 0; i < MANAGED_SERVICES; i++)
                {
                    final String pid = "pid." + i + "-" + (pidCounter++);
                    executor.execute(new Runnable()
                    {
                        public void run()
                        {
                            Hashtable props = new Hashtable();
                            props.put(Constants.SERVICE_PID, pid);

                            ServiceRegistration sr = bundleContext.registerService(
                                ManagedService.class.getName(),
                                new TestManagedService(managedServiceUpdated), props);
                            managedServices.add(sr);
                            try
                            {
                                Configuration c = ca.getConfiguration(pid, null);
                                c.update(new Hashtable()
                                {
                                    {
                                        put("foo", "bar");
                                    }
                                });
                                confs.add(c);
                            }
                            catch (IOException e)
                            {
                                log.error("could not create pid %s", e, pid);
                                return;
                            }
                        }
                    });
                }

                if (!managedServiceUpdated.await(MAXWAIT, TimeUnit.MILLISECONDS))
                {
                    TestCase.fail("Detected errors logged during concurrent test");
                    break;
                }
                log.info("all managed services updated");

                // Unregister managed services concurrently
                log.info("unregistering services concurrently");
                for (final ServiceRegistration sr : managedServices)
                {
                    executor.execute(new Runnable()
                    {
                        public void run()
                        {
                            sr.unregister();
                            managedServiceUnregistered.countDown();
                        }
                    });
                }

                // Unregister configuration concurrently
                log.info("unregistering configuration concurrently");
                for (final Configuration c : confs)
                {
                    c.delete();
                }

                // Wait until managed services have been unregistered
                if (!managedServiceUnregistered.await(MAXWAIT, TimeUnit.MILLISECONDS))
                {
                    TestCase.fail("Managed Servives could not be unregistered timely");
                    break;
                }

                if (log.errorsLogged())
                {
                    TestCase.fail("Detected errors logged during concurrent test");
                    break;
                }

                log.info("finished one test loop");
                if ((loop + 1) % 100 == 0)
                {
                    long duration = System.currentTimeMillis() - timeStamp;
                    System.out.println(String.format("Performed %d tests in %d ms.", (loop + 1), duration));
                    timeStamp = System.currentTimeMillis();
                }
            }
        }

        catch (Throwable t)
        {
            Assert.fail("Test failed: " + t.getMessage());
        }

        finally
        {
            shutdown(executor);
            log.close();
        }
    }

    void shutdown(ExecutorService exec)
    {
        exec.shutdown();
        try
        {
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
        }
    }

    /**
     * One ManagedService concurrently registered in the OSGI registry.
     * We count down a latch once we have been updated with our configuration.
     */
    public class TestManagedService implements ManagedService
    {
        private final CountDownLatch latch;
        private Dictionary<String, ?> props;

        TestManagedService(CountDownLatch latch)
        {
            this.latch = latch;
        }

        public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException
        {
            if (this.props == null && properties == null)
            {
                // GetConfiguration has been called, but configuration have not yet been delivered.
                return;
            }
            this.props = properties;
            latch.countDown();
        }
    }
}
