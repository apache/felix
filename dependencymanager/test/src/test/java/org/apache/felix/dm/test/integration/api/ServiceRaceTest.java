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
    final static int INVOKES = 10;
    volatile ExecutorService m_execServices; // used to register/unregister S services
    volatile ExecutorService m_execAspects; // used to register/unregister Aspects

    @Inject
    volatile ConfigurationAdmin m_ca;

    @Test
    public void testConcurrentServices() {
        warn("starting concurrent services");
        int cores = Math.max(10, Runtime.getRuntime().availableProcessors());
        final DependencyManager dm = new DependencyManager(context);

        try {
            m_execServices = Executors.newFixedThreadPool(cores);
            m_execAspects = Executors.newFixedThreadPool(cores);
            int aspectPidCounter = 1;
            int aspectCounter = 1;
            long timeStamp = System.currentTimeMillis();
            final int tests = 100000;
            for (int loop = 0; loop < tests; loop++) {
                debug("loop#%d -------------------------", (loop + 1));

                final Ensure clientStarted = new Ensure(false);
                final Ensure clientStopped = new Ensure(false);
                final Ensure serviceStarted = new Ensure(false);
                final Ensure serviceStopped = new Ensure(false);
                final Ensure serviceInvoked = new Ensure(false);
                final Ensure aspectStarted = new Ensure(false);
                final Ensure aspectStopped = new Ensure(false);
                final Ensure aspectUpdated = new Ensure(false);
                final Ensure aspectInvoked = new Ensure(false);

                // Create one client depending on many S services
                Client client = new Client(clientStarted, clientStopped);
                Component c = dm.createComponent().setImplementation(client);
                for (int i = 0; i < SERVICES; i++) {
                    c.add(dm.createServiceDependency().setService(S.class, "(name=S" + i + ")").setRequired(true).setCallbacks(
                        "add", null, "remove", "swap"));
                }
                dm.add(c);

                // Create S services concurrently
                info("registering S services concurrently");
                final ConcurrentLinkedQueue<Component> services = new ConcurrentLinkedQueue<Component>();
                for (int i = 0; i < SERVICES; i++) {
                    final String name = "S" + i;
                    m_execServices.execute(new Runnable() {
                        public void run() {
                            Hashtable h = new Hashtable();
                            h.put("name", name);
                            Component sImpl = dm.createComponent().setImplementation(
                                new SImpl(serviceStarted, serviceStopped, serviceInvoked, name)).setInterface(
                                S.class.getName(), h);
                            services.add(sImpl);
                            dm.add(sImpl);
                        }
                    });
                }

                // Create S aspects concurrently
                info("registering aspects concurrently");
                final Queue<Component> aspects = new ConcurrentLinkedQueue<Component>();
                final Queue<Configuration> aspectPids = new ConcurrentLinkedQueue<Configuration>();
                for (int i = 0; i < SERVICES; i++) {
                    final String name = "Aspect" + i + "-" + (aspectCounter++);
                    final String filter = "(name=S" + i + ")";
                    final String pid = "Aspect" + i + ".pid" + (aspectPidCounter++);
                    final int rank = (i+1);
                    m_execServices.execute(new Runnable() {
                        public void run() {
                            SAspect sa = new SAspect(aspectStarted, aspectStopped, aspectUpdated, aspectInvoked, name, rank);
                            debug("Adding aspect " + sa);
                            Component aspect = dm.createAspectService(S.class, filter, rank).setImplementation(sa).add(
                                dm.createConfigurationDependency().setPid(pid));
                            aspects.add(aspect);
                            dm.add(aspect);
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
                                return;
                            }
                        }
                    });
                }

                // Make sure all services and aspects are created
                clientStarted.waitForStep(1, STEP_WAIT);
                aspectUpdated.waitForStep(SERVICES, STEP_WAIT);
                aspectStarted.waitForStep(SERVICES, STEP_WAIT);
                info("all aspects and services registered");

                // Make sure client invoked services and aspects SERVICES * INVOKES times
                serviceInvoked.waitForStep(SERVICES * INVOKES, STEP_WAIT);
                aspectInvoked.waitForStep(SERVICES * INVOKES, STEP_WAIT);
                info("All aspects and services have been properly invoked");

                // Unregister services concurrently
                info("unregistering services concurrently");
                for (final Component sImpl : services) {
                    m_execServices.execute(new Runnable() {
                        public void run() {
                            dm.remove(sImpl);
                        }
                    });
                }

                // Unregister aspects (and configuration) concurrently
                info("unregistering aspects concurrently");
                for (final Component a : aspects) {
                    m_execAspects.execute(new Runnable() {
                        public void run() {
                            debug("removing aspect %s", a);
                            dm.remove(a);
                        }
                    });
                }
                info("unregistering aspect configuration concurrently");
                for (Configuration aspectConf : aspectPids) {
                    aspectConf.delete();
                }

                info("removing client from dm");
                dm.remove(c);

                // Wait until all services/aspects have been stopped
                serviceStopped.waitForStep(SERVICES, STEP_WAIT);
                aspectStopped.waitForStep(SERVICES, STEP_WAIT);
                clientStopped.waitForStep(1, STEP_WAIT);

                if (super.errorsLogged()) {
                    throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
                }

                info("finished one test loop");
                if ((loop + 1) % 100 == 0) {
                    long duration = System.currentTimeMillis() - timeStamp;
                    warn("Performed %d tests (total=%d) in %d ms.", (loop + 1), tests, duration);
                    timeStamp = System.currentTimeMillis();
                }
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
        final Ensure m_started, m_stopped, m_invoked;
        final String m_name;

        public SImpl(Ensure started, Ensure stopped, Ensure invoked, String name) {
            m_name = name;
            m_started = started;
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
            m_started.step();
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
            if (m_services.remove(prevRef.getProperty("name")) == null) {
                throw new IllegalStateException("client being swapped with an unknown old service: oldRef=" + prevRef.getProperty("name") + 
                    ", newRef=" + nextRef.getProperty("name") + ", current injected services=" + m_services);
            }
            m_services.put((String) nextRef.getProperty("name"), next);
        }

        synchronized void add(Map<String, String> props, S s) {
            info("client.add: %s", s);
            m_services.put(props.get("name"), s);
        }

        synchronized void remove(Map<String, String> props, S s) {
            info("client.remove: %s", s);
            if (m_services.remove(props.get("name")) == null) {
                throw new IllegalStateException("client being removed with an unknown old service: " + props.get("name"));
            }
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
        final Ensure m_invoked, m_started, m_stopped, m_updated;
        volatile Dictionary<String, String> m_conf;

        SAspect(Ensure started, Ensure stopped, Ensure updated, Ensure invoked, String name, int rank) {
            m_started = started;
            m_stopped = stopped;
            m_updated = updated;
            m_invoked = invoked;
            m_name = name;
            m_rank = rank;
        }

        public void updated(Dictionary<String, String> conf) {
            if (conf == null) {
                info("Aspect %s injected with a null configuration", this);
                return;
            }
            debug("Aspect %s injected with configuration: %s", this, conf);
            m_conf = conf;
            m_updated.step();
        }

        public void start() {
            info("started aspect %s", this);
            m_started.step();
        }

        public void stop() {
            info("stopped aspect %s", this);
            m_stopped.step();
        }

        public void invoke(int prevRank) {
            Assert.assertTrue(prevRank > m_rank);
            if (m_conf == null) {
                error("Aspect: %s has not been injected with its configuration", this);
                return;
            }

            if (!m_name.equals(m_conf.get("name"))) {
                error("Aspect %s has not been injected with expected configuration: %s", this, m_conf);
                return;
            }
            m_invoked.step();
            m_next.invoke(m_rank);
        }

        public String toString() {
            return m_name;
        }
    }
}
