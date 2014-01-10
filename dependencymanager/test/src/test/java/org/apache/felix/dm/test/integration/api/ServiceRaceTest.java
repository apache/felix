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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * FELIX-3910: Another race test for concurrent service registration/unregistration.
 */
@RunWith(PaxExam.class)
public class ServiceRaceTest extends TestBase {
    final static int SERVICES = 10;
    final static int INVOKES = 10;
    volatile ExecutorService m_execRegister; // used to register/unregister S services
    volatile ExecutorService m_execInvoke; // Used by Client to invoke S services

    @Inject
    volatile ConfigurationAdmin m_ca;

    @Test
    public void testConcurrentServices() {
        warn("starting concurrent services");
        int cores = Math.max(10, Runtime.getRuntime().availableProcessors());
        final DependencyManager dm = new DependencyManager(context);

        try {
            m_execRegister = Executors.newFixedThreadPool(cores);
            m_execInvoke = Executors.newFixedThreadPool(1);

            for (int loop = 0; loop < 10000; loop++) {
                Ensure e = new Ensure(false);
                long timeStamp = System.currentTimeMillis();
                
                // Create one client depending on 'SERVICES' S services
                Client client = new Client(e);
                Component c = dm
                        .createComponent()
                        .setImplementation(client);
                for (int i = 0; i < SERVICES; i ++) {
                    c.add(dm.createServiceDependency().setService(S.class, "(name=S" + i + ")").setRequired(true).setCallbacks(
                        "add", "remove"));
                }
                dm.add(c);

                // Create all the 'SERVICES' S services concurrently
                info("registering services concurrently");
                final Ensure addE = new Ensure(false);
                final List<Component> services = new CopyOnWriteArrayList<Component>();
                for (int i = 0; i < SERVICES; i ++) {
                    final String name = "S" + i;
                    m_execRegister.execute(new Runnable() {
                        public void run() {
                            Hashtable h = new Hashtable();
                            h.put("name", name);
                            Component sImpl = dm
                                .createComponent()
                                .setImplementation(new SImpl())
                                .setInterface(S.class.getName(), h);
                            services.add(sImpl);
                            dm.add(sImpl);
                            addE.step();
                        }
                    });
                }
                addE.waitForStep(SERVICES, 5000);
                
                // Make sure client is started:
                e.waitForStep(1, 5000);
                
                // Make sure client invoked services SERVICES * INVOKES times
                e.waitForStep(1 + (SERVICES * INVOKES), 10000);
                if ((loop+1) % 100 == 0) {
                    long duration = System.currentTimeMillis() - timeStamp;
                    warn("Performed %d tests in %d ms.", (loop+1), duration);
                    timeStamp = System.currentTimeMillis();
                }
                
                // Unregister services concurrently
                final Ensure removeE = new Ensure(false);
                info("unregistering services concurrently");
                for (final Component sImpl : services) {
                    m_execRegister.execute(new Runnable() {
                        public void run() {
                            dm.remove(sImpl);
                            removeE.step();
                        }
                    });
                }
                removeE.waitForStep(SERVICES, 5000);
                
                // Make sure Client has been stopped
                info("waiting for client to be stopped");
                int nextStep = 1 /* start */ + (SERVICES * INVOKES) + 1 /* stop */;
                e.waitForStep(nextStep, 5000);
                
                info("all services stopped");
                
                // Make sure all services are all unbound from our client.
                nextStep += SERVICES; // Client.removed should have been called for each unbound service.
                e.waitForStep(nextStep, 5000);
                
                // Clear everything before interating on next loop
                dm.clear();
                
                if (super.errorsLogged()) {
                    throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
                }
            }
        }

        catch (Throwable t) {
            error("Test failed", t);
            Assert.fail("Test failed: " + t.getMessage());
        }
        finally {
            shutdown(m_execRegister);
            shutdown(m_execInvoke);
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
        void invoke(Ensure e);
    }

    public static class SImpl implements S {
        public void invoke(Ensure e) {
            e.step();
        }
    }
    
    public class Client {
        final Ensure m_e;
        final Map<String, S> m_services = new ConcurrentHashMap<String, S>();

        Client(Ensure e) {
            m_e = e;
        }
        
        void add(Map<String, String> props, S s) {
            info("client.add: %s (name=%s)", s, props.get("name"));
            m_services.put(props.get("name"), s);
        }
        
        void remove(Map<String, String> props, S s) {
            info("client.remove: %s (name=%s)", s, props.get("name"));
            m_services.remove(props.get("name"));
            m_e.step();
        }
        
        public void start() {   
            if (m_services.size() != SERVICES) {
                error("Client started with unexpected number of injected services: %s", m_services);
                return;
            }
            m_e.step(1);
            m_execInvoke.execute(new Runnable() {
                public void run() {
                    for (int i = 0; i < INVOKES; i ++) {
                        for (Map.Entry<String, S> e : m_services.entrySet()) {
                            e.getValue().invoke(m_e);
                        }
                    }
                }
            });
        }
        
        public void stop() {
            m_e.step(1 /* start */ + (SERVICES * INVOKES) + 1);
        }
    }
}
