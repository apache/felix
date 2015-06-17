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
package test;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.impl.ComponentImpl;
import org.apache.felix.dm.impl.ConfigurationDependencyImpl;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * This test class simulates a client having many dependencies being registered/unregistered concurrently.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ServiceRaceTest extends TestBase {
    final static int STEP_WAIT = 5000;
    final static int DEPENDENCIES = 10;
    final static int LOOPS = 10000;

    // Executor used to bind/unbind service dependencies.
    ExecutorService m_threadpool;
    // Timestamp used to log the time consumed to execute 100 tests.
    long m_timeStamp;

    /**
     * Creates many service dependencies, and activate/deactivate them concurrently.  
     */
    @Test
    public void createParallelComponentRegistgrationUnregistration() {
        info("Starting createParallelComponentRegistgrationUnregistration test");
        int cores = Math.max(16, Runtime.getRuntime().availableProcessors());
        info("using " + cores + " cores.");

        m_threadpool = Executors.newFixedThreadPool(Math.max(cores, DEPENDENCIES + 3 /* start/stop/configure */));

        try {
            m_timeStamp = System.currentTimeMillis();
            for (int loop = 0; loop < LOOPS; loop++) {
                doTest(loop);
            }
        }
        catch (Throwable t) {
            warn("got unexpected exception", t);
        }
        finally {
            shutdown(m_threadpool);
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

    void doTest(int loop) throws Throwable {
        debug("loop#%d -------------------------", loop);

        final Ensure step = new Ensure(false);

        // Create one client component, which depends on many service dependencies
        final ComponentImpl client = new ComponentImpl();
        final Client theClient = new Client(step);
        client.setImplementation(theClient);

        // Create client service dependencies
        final SimpleServiceDependency[] dependencies = new SimpleServiceDependency[DEPENDENCIES];
        for (int i = 0; i < DEPENDENCIES; i++) {
            dependencies[i] = new SimpleServiceDependency();
            dependencies[i].setRequired(true);
            dependencies[i].setCallbacks("add", "remove");
            client.add(dependencies[i]);
        }
        final ConfigurationDependencyImpl confDependency = new ConfigurationDependencyImpl();
        confDependency.setPid("mypid");
        client.add(confDependency);

        // Create Configuration (concurrently).
        // We have to simulate the configuration update, using a component state listener, which will
        // trigger an update thread, but only once the component is started.
        final ComponentStateListener listener = new ComponentStateListener() {
            private volatile Dictionary m_conf;

            public void changed(Component c, ComponentState state) {
                if (state == ComponentState.WAITING_FOR_REQUIRED && m_conf == null) {
                    m_conf = new Hashtable();
                    m_conf.put("foo", "bar");
                    m_threadpool.execute(new Runnable() {
                        public void run() {
                            try {
                                confDependency.updated(m_conf);
                            }
                            catch (ConfigurationException e) {
                                warn("configuration failed", e);
                            }
                        }
                    });
                }
            }
        };
        client.add(listener);


        // Start the client (concurrently)
        m_threadpool.execute(new Runnable() {
            public void run() {
                client.start();
                
                // Activate the client service dependencies concurrently.
                // We *must* do this after having started the component (in a reality, the dependencies can be 
                // injected only one the tracker has been opened ...
                for (int i = 0; i < DEPENDENCIES; i++) {
                    final SimpleServiceDependency dep = dependencies[i];
                    final Event added = new EventImpl(i);
                    m_threadpool.execute(new Runnable() {
                        public void run() {
                            dep.add(added);
                        }
                    });
                }

            }
        });

        // Ensure that client has been started.
        int expectedStep = 1 /* conf */ + DEPENDENCIES + 1 /* start */;
        step.waitForStep(expectedStep, STEP_WAIT);
        Assert.assertEquals(DEPENDENCIES, theClient.getDependencies());
        Assert.assertNotNull(theClient.getConfiguration());
        client.remove(listener);

        // Stop the client and all dependencies concurrently.
        for (int i = 0; i < DEPENDENCIES; i++) {
            final SimpleServiceDependency dep = dependencies[i];
            final Event removed = new EventImpl(i);
            m_threadpool.execute(new Runnable() {
                public void run() {
                    dep.remove(removed);
                }
            });
        }
        m_threadpool.execute(new Runnable() {
            public void run() {
                client.stop();
            }
        });
        m_threadpool.execute(new Runnable() {
            public void run() {
                try {
                    // simulate a configuration suppression.
                    confDependency.updated(null);
                }
                catch (ConfigurationException e) {
                    warn("error while unconfiguring", e);
                }
            }
        });

        // Ensure that client has been stopped, then destroyed, then unbound from all dependencies
        expectedStep += 2; // stop/destroy
        expectedStep += DEPENDENCIES; // removed all dependencies
        step.waitForStep(expectedStep, STEP_WAIT);
        step.ensure();
        Assert.assertEquals(0, theClient.getDependencies());

        debug("finished one test loop");
        if ((loop + 1) % 100 == 0) {
            long duration = System.currentTimeMillis() - m_timeStamp;
            warn("Performed 100 tests (total=%d) in %d ms.", (loop + 1), duration);
            m_timeStamp = System.currentTimeMillis();
        }
    }

    public class Client {
        final Ensure m_step;
        int m_dependencies;
        volatile Dictionary m_conf;
        
        public Client(Ensure step) {
            m_step = step;
        }

        public void updated(Dictionary conf) throws ConfigurationException {
            if (conf != null) {
                Assert.assertNotNull(conf);
                Assert.assertEquals("bar", conf.get("foo"));
                m_conf = conf;
                m_step.step(1);
            }
        }
        
        synchronized void add() {
            m_step.step();
            m_dependencies++;
        }
        
        synchronized void remove() {
            m_step.step();
            m_dependencies--;
        }
                
        void start() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */);
        }

        void stop() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */ + 1 /* stop */);
        }
        
        void destroy() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */ + 1 /* stop */  + 1 /* destroy */);
        }
        
        synchronized int getDependencies() {
            return m_dependencies;
        }
        
        Dictionary getConfiguration() {
            return m_conf;
        }
    }
}
