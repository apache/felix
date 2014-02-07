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
package org.apache.felix.dm.test.integration.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.components.Ensure;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Another race test for concurrent service registrations/unregistrations.
 * Aspects are also depending on some configuration pids, which are also registered/unregistered concurrently.
 */
@RunWith(PaxExam.class)
public class ServiceRaceTest extends TestBase {
    final static int STEP_WAIT = 10000;
    final static int SERVICES = 3;
    final static int INVOKES = 1;
    final static int TESTS = 10000;
    
    final static boolean INCLUDE_CONFIG_DEPENDENCY = false;
    
    volatile ExecutorService m_execServices; // used to register/unregister S services
    volatile ExecutorService m_execAspects; // used to register/unregister Aspects

    @Inject
    volatile ConfigurationAdmin m_ca;

    @Test
    public void testConcurrentServices() {
        warn("starting concurrent services");
        int cores = Math.max(16, Runtime.getRuntime().availableProcessors());
        warn("using " + cores + " cores.");
        warn("include configuration dependency: " + INCLUDE_CONFIG_DEPENDENCY);
        final DependencyManager dm = new DependencyManager(context);
        Random rnd = new Random();

        try {
            m_execServices = Executors.newFixedThreadPool(cores);
            m_execAspects = Executors.newFixedThreadPool(cores);
            int serviceIdCounter = 1;
            long timeStamp = System.currentTimeMillis();
            for (int loop = 0; loop < TESTS; loop++) {
                debug("loop#%d -------------------------", (loop + 1));

                final Ensure clientStarted = new Ensure(false); // no debug
                final Ensure clientStopped = new Ensure(false);
                final Ensure servicesStopped = new Ensure(false);
                final Ensure servicesInvoked = new Ensure(false);
                final Ensure aspectsInvoked = new Ensure(false);
                final Ensure aspectsRemoved = new Ensure(false);

                // Create one client depending on many S services
                Client client = new Client(clientStarted, clientStopped);
                Component c = dm.createComponent().setImplementation(client);
                for (int i = 0; i < SERVICES; i++) {
                    String filter = "(name=S" + i + "-" + serviceIdCounter + ")";
                    c.add(dm.createServiceDependency().setService(S.class, filter).setRequired(true).setCallbacks(
                        "add", null, "remove", "swap"));
                }
                dm.add(c);

                // Create S services concurrently
                info("registering S services concurrently");
                final ConcurrentLinkedQueue<Component> services = new ConcurrentLinkedQueue<Component>();
                for (int i = 0; i < SERVICES; i++) {
                    final String name = "S" + i + "-" + serviceIdCounter;
                    Hashtable h = new Hashtable();
                    h.put("name", name);
                    final Component sImpl = dm.createComponent().setImplementation(
                        new SImpl(servicesStopped, servicesInvoked, name)).setInterface(
                        S.class.getName(), h);
                    services.add(sImpl);
                    m_execServices.execute(new Runnable() {
                        public void run() {
                            dm.add(sImpl);
                        }
                    });
                }

                // Create S aspects concurrently
                info("registering aspects concurrently");
                final Queue<Component> aspects = new ConcurrentLinkedQueue<Component>();
                for (int i = 0; i < SERVICES; i++) {
                    final String name = "Aspect" + i + "-" + serviceIdCounter;
                    final String filter = "(name=S" + i + "-" + serviceIdCounter + ")";
                    final String pid = "Aspect" + i + "-" + serviceIdCounter + ".pid";
                    final int rank = (i+1);
                    SAspect sa = new SAspect(aspectsInvoked, name, rank);
                    debug("Adding aspect " + sa);
                    final Component aspect = dm.createAspectService(S.class, filter, rank).setImplementation(sa);
                    if (INCLUDE_CONFIG_DEPENDENCY) {
                        aspect.add(dm.createConfigurationDependency().setPid(pid));
                    }
                    aspects.add(aspect);
                    m_execAspects.execute(new Runnable() {
                        public void run() {
                            dm.add(aspect);
                        }
                    });
                }
                
                // Provide all aspect configuration (asynchronously)
                final Queue<Configuration> aspectPids = new ConcurrentLinkedQueue<Configuration>();
                if (INCLUDE_CONFIG_DEPENDENCY) {
                    for (int i = 0; i < SERVICES; i++) {
                        final String name = "Aspect" + i + "-" + serviceIdCounter;
                        final String pid = "Aspect" + i + "-" + serviceIdCounter + ".pid";
                        final int rank = (i+1);
                        SAspect sa = new SAspect(aspectsInvoked, name, rank);
                        debug("Adding aspect configuration pid %s for aspect %s", pid, sa);
                        try {
                            Configuration aspectConf = m_ca.getConfiguration(pid, null);
                            aspectConf.update(new Hashtable() {
                                {
                                    put("name", name);
                                }
                            });
                            aspectPids.add(aspectConf);
                        }
                        catch (IOException e) {
                            error("could not create pid %s for aspect %s", e, pid, name);
                        }
                    }
                }
                
                // Increment service id counter, for next iteration.
                serviceIdCounter ++;

                // Make sure client is started
                clientStarted.waitForStep(1, STEP_WAIT);
                info("all services have been started");

                // Make sure client invoked services SERVICES * INVOKES times
                servicesInvoked.waitForStep(SERVICES * INVOKES, STEP_WAIT);
                info("All services have been properly invoked");
                
                // Now ensure that some random aspects have been randomly invoked.
                int aspectCount = Math.max(1, rnd.nextInt(SERVICES));
                int aspectInvocations = Math.max(1, rnd.nextInt(INVOKES));                
                aspectsInvoked.waitForStep(aspectCount * aspectInvocations, STEP_WAIT);
                info("%d aspects have been properly invoked %d times", aspectCount, aspectInvocations);                                
                                
                // Unregister services concurrently (at this point, it is possible that we have still some aspects being activating !
                info("unregistering services concurrently");
                for (final Component sImpl : services) {
                    m_execServices.execute(new Runnable() {
                        public void run() {
                            dm.remove(sImpl);
                        }
                    });
                }

                // unregister aspects concurrently (some aspects can potentially be still activating !)
                info("unregistering aspects concurrently");
                for (final Component a : aspects) {
                    m_execAspects.execute(new Runnable() {
                        public void run() {
                            debug("removing aspect %s", a);
                            dm.remove(a);
                            aspectsRemoved.step();
                        }
                    });
                }
                
                info("unregistering aspects configuration concurrently");
                for (Configuration aspectConf : aspectPids) {
                    aspectConf.delete(); // asynchronous
                }
                
                info("removing client");
                dm.remove(c);

                // Wait until all services have been stopped
                servicesStopped.waitForStep(SERVICES, STEP_WAIT);
                
                // Wait until client has been stopped
                clientStopped.waitForStep(1, STEP_WAIT);
                
                // Wait until all aspects have been deleted
                aspectsRemoved.waitForStep(SERVICES, STEP_WAIT);
                
                if (super.errorsLogged()) {
                    throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
                }

                info("finished one test loop: current components=%d", dm.getComponents().size());
                if ((loop + 1) % 100 == 0) {
                    long duration = System.currentTimeMillis() - timeStamp;
                    warn("Performed %d tests (total=%d) in %d ms.", (loop + 1), TESTS, duration);
                    timeStamp = System.currentTimeMillis();
                }
                
                // Cleanup all remaining components (but all components should have been removed now).
                dm.clear();
            }
        }

        catch (Throwable t) {
            error("Test failed", t);
            Assert.fail("Test failed: " + t.getMessage());
        }
        finally {
            shutdown(m_execServices);
            shutdown(m_execAspects);
            dm.clear();
        }
    }

    void shutdown(ExecutorService exec) {
        exec.shutdown();
        try {
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
        }
    }

    public interface S {
        void invoke(int prevAspectId);
    }

    public class SImpl implements S {
        final Ensure m_stopped, m_invoked;
        final String m_name;

        public SImpl(Ensure stopped, Ensure invoked, String name) {
            m_name = name;
            m_stopped = stopped;
            m_invoked = invoked;
        }

        public void invoke(int prevRank) {
            Assert.assertTrue(prevRank > 0);
            m_invoked.step();
        }

        public String toString() {
            return m_name;
        }

        public void start() {
            info("started %s", this);
        }

        public void stop() {
            info("stopped %s", this);
            m_stopped.step();
        }
    }

    public class Client implements Runnable {
        final Ensure m_started, m_stopped;
        final Map<String, S> m_services = new ConcurrentHashMap<String, S>();
        volatile Thread m_thread;
        volatile Exception m_firstStartStackTrace;
        volatile boolean m_running;

        Client(Ensure started, Ensure stopped) {
            m_started = started;
            m_stopped = stopped;
        }

        synchronized void swap(ServiceReference prevRef, S prev, ServiceReference nextRef, S next) {
            info("client.swap: prev=%s, next=%s", prev, next);
            m_services.put((String) nextRef.getProperty("name"), next);
        }

        synchronized void add(Map<String, String> props, S s) {
            info("client.add: %s", s);
            m_services.put(props.get("name"), s);
        }

        synchronized void remove(Map<String, String> props, S s) {
            info("client.remove: %s", s);
            m_services.remove(props.get("name"));
        }

        public synchronized void start() {
            if (m_firstStartStackTrace != null) {
                error("client already started", new Exception());
                error("first start was done here:", m_firstStartStackTrace);
                return;
            }
            if (m_running) {
                error("Client already started");
                return;
            }
            if (m_services.size() != SERVICES) {
                error("Client started with unexpected number of injected services: %s", m_services);
                return;
            }
            m_firstStartStackTrace = new Exception("First start stacktrace");
            info("client starting");

            m_thread = new Thread(this, "Client");
            m_thread.setDaemon(true);
            m_running = true;
            m_thread.start();
        }

        public void run() {
            m_started.step();
            while (m_running) {
                for (int i = 0; i < INVOKES; i++) {
                    for (Map.Entry<String, S> e : m_services.entrySet()) {
                        e.getValue().invoke(Integer.MAX_VALUE); // We are on the top of the aspect list
                    }
                }
            }
        }

        public synchronized void stop() {
            if (!m_running) {
                error("client can't be stopped (not running)");
                Thread.dumpStack();
                return;
            }

            info("stopping client");
            m_running = false;
            try {
                m_thread.join();
            }
            catch (InterruptedException e) {
                error("interrupted while stopping client", e);
            }
            info("client stopped");
            m_firstStartStackTrace = null;
            m_stopped.step();
        }
    }

    public class SAspect implements S {
        volatile S m_next;
        final String m_name;
        final int m_rank;
        final Ensure m_invoked;
        volatile Dictionary<String, String> m_conf;

        SAspect(Ensure invoked, String name, int rank) {
            m_invoked = invoked;
            m_name = name;
            m_rank = rank;
        }

        public void updated(Dictionary<String, String> conf) {
            if (INCLUDE_CONFIG_DEPENDENCY) {
                if (conf == null) {
                    info("Aspect %s injected with a null configuration", this);
                    return;
                }
                debug("Aspect %s injected with configuration: %s", this, conf);
                m_conf = conf;
            }
        }

        public void start() {
            info("started aspect %s", this);
        }

        public void stop() {
            info("stopped aspect %s", this);
        }

        public void invoke(int prevRank) {
            Assert.assertTrue(prevRank > m_rank);
            if (INCLUDE_CONFIG_DEPENDENCY) {
                if (m_conf == null) {
                    error("Aspect: %s has not been injected with its configuration", this);
                    return;
                }
    
                if (!m_name.equals(m_conf.get("name"))) {
                    error("Aspect %s has not been injected with expected configuration: %s", this, m_conf);
                    return;
                }
            }
            m_invoked.step();
            m_next.invoke(m_rank);
        }

        public String toString() {
            return m_name;
        }
    }
}
