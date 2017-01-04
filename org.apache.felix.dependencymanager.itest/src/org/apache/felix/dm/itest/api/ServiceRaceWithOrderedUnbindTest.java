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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;


/**
 * This test class simulates a client having many dependencies being registered concurrently.
 * (threads are created manually, and we are not using a ComponentExecutorFactory).
 * The services are then unregistered from a single thread (like it is the case when the osgi 
 * framework is stopped where bunldes are stopped synchronously).
 * So, when unbind methods are called, we verify that unbound services are still started at 
 * the time unbind callbacks are invoked. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ServiceRaceWithOrderedUnbindTest extends TestBase {
    final static int STEP_WAIT = 5000;
    final static int DEPENDENCIES = 10;
    final static int LOOPS = 3000;
    final Ensure m_done = new Ensure(true);

    // Timestamp used to log the time consumed to execute 100 tests.
    long m_timeStamp;
    
    public interface Dep {    
    	boolean isStarted();
    }
    
    public class DepImpl implements Dep {
    	volatile boolean m_started;
    	
    	void start() {
    		m_started = true;
    	}
    	
    	void stop() {
    		m_started = false;
    	}
    	
    	public boolean isStarted() {
    		return m_started;
    	}
    }
    
    /**
     * Creates many service dependencies, and activate/deactivate them concurrently.  
     */
    public void testCreatesComponentsConcurrently() {
        m_dm.add(m_dm.createComponent()
            .setImplementation(this)
            .setCallbacks(null, "start", null, null));
        m_done.waitForStep(1, 60000);
        m_dm.clear();
        Assert.assertFalse(super.errorsLogged());
    }
    
    void start() {
        new Thread(this::doStart).start();
    }
    
    void doStart() {
        info("Starting createParallelComponentRegistgrationUnregistration test");
        initThreadPool(); // only if setParallel() has not been called (only if a parallel DM is not used).

        try {
            m_timeStamp = System.currentTimeMillis();
            for (int loop = 0; loop < LOOPS; loop++) {
                doTest(loop);
            }
        }
        catch (Throwable t) {
            error("got unexpected exception", t);
        }
        finally {
            shutdownThreadPool();
            m_done.step(1);
        }
    }

    private void initThreadPool() {
        if (! m_parallel) { 
            // We are not using a parallel DM, so we create a custom threadpool in order to add components concurrently.
            int cores = Math.max(16, Runtime.getRuntime().availableProcessors());
            info("using " + cores + " cores.");
            m_threadPool = new ForkJoinPool(Math.max(cores, DEPENDENCIES + 3 /* start/stop/configure */));
        }
    }

    void shutdownThreadPool() {
        if (! m_parallel && m_threadPool != null) {
        	m_threadPool.shutdown();
            try {
            	m_threadPool.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
    }

    void doTest(int loop) throws Throwable {
        debug("loop#%d -------------------------", loop);

        final Ensure step = new Ensure(false);

        // Create one client component, which depends on many service dependencies
        final Component client = m_dm.createComponent();
        final Client clientImpl = new Client(step);
        client.setImplementation(clientImpl);

        // Before creating the client, register a component listener to check
        // the client is really started or deactivated.
        ComponentStateListener clientListener = (c, s) -> {
        	switch(s) {
        	case TRACKING_OPTIONAL:
        		step.step(1);
        		break;
        	case INACTIVE:
        		step.step(2);
        		break;
        	default:
        		break;
        	}
        };
        client.add(clientListener);
        
        // Create client service dependencies
        final ServiceDependency[] dependencies = new ServiceDependency[DEPENDENCIES];
        for (int i = 0; i < DEPENDENCIES; i++) {
            final String filter = "(id=loop" + loop + "." + i + ")";
            dependencies[i] = m_dm.createServiceDependency().setService(Dep.class, filter)
                .setRequired(true)
                .setCallbacks("add", "remove");
            client.add(dependencies[i]);
        }
        
        // Activate the client service dependencies concurrently.
        List<Component> deps = new ArrayList();
        for (int i = 0; i < DEPENDENCIES; i++) {
            Hashtable h = new Hashtable();
            h.put("id", "loop" + loop + "." + i);
            final Component s = m_dm.createComponent()
                .setInterface(Dep.class.getName(), h)
                .setImplementation(new DepImpl());
            deps.add(s);
            schedule(() -> m_dm.add(s));
        }

        // Start the client (concurrently)
        schedule(() -> m_dm.add(client));
        
        // Ensure that client has been started.
        step.waitForStep(1, STEP_WAIT); // client has entered in TRACKING_OPTIONAL state
        Assert.assertEquals(DEPENDENCIES, clientImpl.getDependencies());
        
        // Make sure threadpool is quiescent, then deactivate all components.
        if (! m_threadPool.awaitQuiescence(5000, TimeUnit.MILLISECONDS)) {
        	throw new RuntimeException("Could not start components timely.");
        }

        // Stop all dependencies, and client
        schedule(() ->  {
            for (Component dep : deps) {
                final Component dependency = dep;
                m_dm.remove(dependency);
            }
            m_dm.remove(client);
        });
                
        // Ensure that client has been stopped, then destroyed, then unbound from all dependencies
        step.waitForStep(2, STEP_WAIT); // Client entered in INACTIVE state
        step.ensure();
        Assert.assertEquals(0, clientImpl.getDependencies());

        // Make sure threadpool is quiescent before doing next iteration.
        if (! m_threadPool.awaitQuiescence(5000, TimeUnit.MILLISECONDS)) {
        	throw new RuntimeException("Could not start components timely.");
        }

        if (super.errorsLogged()) {
            throw new IllegalStateException("Race test interrupted (some error occured, see previous logs)");
        }

        debug("finished one test loop");
        if ((loop + 1) % 100 == 0) {
            long duration = System.currentTimeMillis() - m_timeStamp;
            warn("Performed 100 tests (total=%d) in %d ms.", (loop + 1), duration);
            m_timeStamp = System.currentTimeMillis();
        }
    }

    private void schedule(Runnable task) {
        if (! m_parallel) {
            // not using parallel DM, so use our custom threadpool.
        	m_threadPool.execute(task);
        } else {
            task.run();
        }        
    }

    public class Client {
        final Ensure m_step;
        volatile int m_dependencies;
        
        public Client(Ensure step) {
            m_step = step;
        }
        
        void add(Dep d) {
            Assert.assertNotNull(d);
            m_dependencies ++;
        }
        
        void remove(Dep d) {
            Assert.assertNotNull(d);
            if (! d.isStarted()) {
            	Thread.dumpStack();
            }
            Assert.assertTrue(d.isStarted());
            m_dependencies --;
        }
                        
        int getDependencies() {
            return m_dependencies;
        }        
    }
}
