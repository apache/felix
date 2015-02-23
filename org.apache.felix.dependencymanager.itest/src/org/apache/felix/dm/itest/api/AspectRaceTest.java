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
package org.apache.felix.dm.itest.api;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This class validates that some aspect aware services are correctly managed and ordered when components and aspects are 
 * registered concurrently.
 * 
 * By default, this class uses a custom threadpool, but a subclass may override this class and call "setParallel()" method, in 
 * this case we won't use any threadpool, since calling setParallel() method means we are using a parallel Dependency Manager.
 * 
 * @see AspectRaceParallelTest
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectRaceTest extends TestBase {
    final static int SERVICES = 3;
    final static int ASPECTS_PER_SERVICE = 10;
    final static int ITERATIONS = 1000;
    final AtomicInteger m_IDGenerator = new AtomicInteger();
    ExecutorService m_threadpool;

    public void testConcurrentAspects() {
        try {
            warn("starting aspect race test");
            initThreadPool(); // only if setParallel() has not been called (only if a parallel DM is not used).

            for (int loop = 1; loop <= ITERATIONS; loop++) {
                // Perform concurrent injections of "S" service and S aspects into the Controller component;
                debug("Iteration: " + loop);
                
                // Use a helper class to wait for components to be started/stopped.
                int count = 1 /* for controller */ + SERVICES + (SERVICES * ASPECTS_PER_SERVICE);
                ComponentTracker tracker = new ComponentTracker(count, count);
                
                // Create the components (controller / services / aspects)
                Controller controller = new Controller();
                Factory f = new Factory();
                f.createComponents(controller, tracker);
                
                // Activate the components asynchronously
                f.registerComponents();
                
                // Wait for the components to be started (using the tracker)
                if (!tracker.awaitStarted(5000)) {
                    throw new IllegalStateException("Could not start components timely.");
                }
                
                // Check aspect chains consistency.
                controller.checkConsistency();
                
                // unregister all services and aspects.
                f.unregisterComponents();
                
                // use component tracker to wait for all components to be stopped.
                if (!tracker.awaitStopped(5000)) {
                    throw new IllegalStateException("Could not stop components timely.");
                }

                if ((loop) % 50 == 0) {
                    warn("Performed " + loop + " tests.");
                }

                if (super.errorsLogged()) {
                    throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
                }
            }
        }

        catch (Throwable t) {
            error("Test failed", t);
            Assert.fail("Test failed: " + t.getMessage());
        } finally {
            m_dm.clear();
            shutdownThreadPool();
        }
    }

    private void initThreadPool() {
        // Create a threadpool only if setParallel() method has not been called.
        if (! m_parallel) {
            int cores = Math.max(16, Runtime.getRuntime().availableProcessors());
            m_threadpool = Executors.newFixedThreadPool(cores);
        }
    }
    
    void shutdownThreadPool() {
        if (! m_parallel && m_threadpool != null) {
            m_threadpool.shutdown();
            try {
                m_threadpool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
    }

    public interface S {
        void invoke(Ensure e);

        int getRank();
    }

    public static class SImpl implements S {

        SImpl() {
        }

        public void invoke(Ensure e) {
            e.step(1);
        }

        public String toString() {
            return "SImpl";
        }

        @Override
        public int getRank() {
            return Integer.MIN_VALUE;
        }
    }

    public class SAspect implements S {
        volatile S m_next;
        final int m_rank;
        volatile Component m_component;

        SAspect(int rank) {
            m_rank = rank;
        }

        public synchronized void added(S s) {
            debug("aspect.added: this rank=%d, next rank=%d", getRank(), s.getRank());
            m_next = s;
        }

        public synchronized void swap(S oldS, S newS) {
            debug("aspect.swap: this rank=%d, old rank=%d, next rank=%d", getRank(), oldS.getRank(), newS.getRank());
            m_next = newS;
        }

        public synchronized void removed(S s) {
            debug("aspect.remove: this rank=%d, removed rank=%d", getRank(), s.getRank());
            m_next = null;
        }

        public synchronized void invoke(Ensure e) {
            debug("aspect.invoke: this rank=%d, next rank=%d", this.getRank(), m_next.getRank());
            Assert.assertTrue(m_rank > m_next.getRank());
            m_next.invoke(e);
        }

        public String toString() {
            return "[Aspect/rank=" + m_rank + "], next="
                + ((m_next != null) ? m_next : "null");
        }

        @Override
        public int getRank() {
            return m_rank;
        }
    }

    class Factory {
        int m_serviceId;
        Component m_controller;
        final ConcurrentLinkedQueue<Component> m_services = new ConcurrentLinkedQueue<Component>();
        final ConcurrentLinkedQueue<Component> m_aspects = new ConcurrentLinkedQueue<Component>();   
        
        private void createComponents(Controller controller, ComponentTracker tracker) {
            // create the controller
            int controllerID = m_IDGenerator.incrementAndGet();
            m_controller = m_dm.createComponent()
                .setImplementation(controller)
                .setComposition("getComposition")
                .add(tracker);
            for (int i = 0; i < SERVICES; i ++) {
                m_controller.add(m_dm.createServiceDependency()
                    .setService(S.class, "(controller.id=" + controllerID + ")")
                    .setCallbacks("bind", null, "unbind", "swap")
                    .setRequired(true));
            }
            
            // create the services
            for (int i = 1; i <= SERVICES; i++) {
                int aspectId = m_IDGenerator.incrementAndGet();
                Component s = m_dm.createComponent();
                Hashtable<String, String> props = new Hashtable<String, String>();
                props.put("controller.id", String.valueOf(controllerID));
                props.put("aspect.id", String.valueOf(aspectId));
                s.setInterface(S.class.getName(), props)
                 .setImplementation(new SImpl());
                s.add(tracker);
                m_services.add(s);
                
                // create the aspects for that service
                for (int j = 1; j <= ASPECTS_PER_SERVICE; j++) {
                    final int rank = j;
                    SAspect sa = new SAspect(rank);
                    Component a = 
                        m_dm.createAspectService(S.class, "(aspect.id=" + aspectId + ")", rank, "added", null, "removed", "swap")
                            .setImplementation(sa);
                    a.add(tracker);
                    m_aspects.add(a);
                }
            }
        }

        public void registerComponents() {
            // If setParallel() has been called (we are using a parallel dependency manager), then no needs to use a custom thread pool.
            if (m_parallel) { // using a parallel DM.
                for (final Component s : m_services) {
                    m_dm.add(s);
                }
                m_dm.add(m_controller);
                for (final Component a : m_aspects) {
                    m_dm.add(a);
                }
            } else {
                for (final Component s : m_services) {
                    m_threadpool.execute(new Runnable() {
                        public void run() {
                            m_dm.add(s);
                        }
                    });
                }
                m_threadpool.execute(new Runnable() {
                    public void run() {
                        m_dm.add(m_controller);
                    }
                });
                for (final Component a : m_aspects) {
                    m_threadpool.execute(new Runnable() {
                        public void run() {
                            m_dm.add(a);
                        }
                    });
                }
            }
        }

        public void unregisterComponents() throws InterruptedException, InvalidSyntaxException {        
            m_dm.remove(m_controller);
            for (final Component s : m_services) {
                m_dm.remove(s);
            }
            for (final Component a : m_aspects) {
                m_dm.remove(a);
            }
        }
    }

    public class Controller {
        final Composition m_compo = new Composition();
        final HashSet<S> m_services = new HashSet<S>();

        Object[] getComposition() {
            return new Object[] { this, m_compo };
        }

        synchronized void bind(ServiceReference sr, Object service) {
            debug("controller.bind: %s", service);
            S s = (S) service;
            m_services.add(s);
            debug("bind: service count after bind: %d", m_services.size());
        }

        synchronized void swap(S previous, S current) {
            debug("controller.swap: previous=%s, current=%s", previous, current);
            if (!m_services.remove(previous)) {
                debug("swap: unknow previous service: " + previous);
            }
            m_services.add(current);
            debug("controller.swap: service count after swap: %d", m_services.size());
        }

        synchronized void unbind(S a) {
            debug("unbind " + a);
            m_services.remove(a);
        }
        
        synchronized void checkConsistency() {
            debug("service count: %d", m_services.size());
            for (S s : m_services) {
                info("checking service: %s", s);
                Ensure ensure = new Ensure(false);
                s.invoke(ensure);
            }
        }
    }

    public static class Composition {
    }
}
