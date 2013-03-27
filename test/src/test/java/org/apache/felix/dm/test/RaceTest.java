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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@RunWith(JUnit4TestRunner.class)
public class RaceTest extends Base implements Runnable {
    @Configuration
    public static Option[] configuration() {
        return options(provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium")
                        .version(Base.OSGI_SPEC_VERSION), mavenBundle().groupId("org.apache.felix")
                        .artifactId("org.apache.felix.dependencymanager").versionAsInProject()));
    }

    static volatile CountDownLatch m_bindALatch;
    static volatile CountDownLatch m_unbindALatch;
    volatile ThreadPoolExecutor m_exec;
    static volatile BundleContext m_bctx;
    volatile boolean m_running = true;
    volatile CountDownLatch m_stopLatch = new CountDownLatch(1);

    @Test
    public void testConcurrentInjections(BundleContext ctx) {
        m_exec = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        try {
            m_bctx = ctx;
            DependencyManager m = new DependencyManager(ctx);
            Component controller = m
                    .createComponent()
                    .setImplementation(Controller.class)
                    .setComposition("getComposition")
                    .add(m.createServiceDependency().setService(A.class).setCallbacks("bindA", "unbindA")
                            .setRequired(true));

            m.add(controller);
            Thread t = new Thread(this);
            t.start();
            super.sleep(10000);

            m_running = false;
            t.interrupt();
            m_stopLatch.await(5000, TimeUnit.MILLISECONDS);
            Assert.assertFalse("Test failed.", super.errorsLogged());            
        } 
        
        catch (Throwable t) {
            t.printStackTrace();
        }
        finally {
            m_exec.shutdown();
            try {
                m_exec.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
    }

    public void run() {
        try {
            int loop = 0;
            while (m_running) {
                int N = 10;
                m_bindALatch = new CountDownLatch(N * 2);
                m_unbindALatch = new CountDownLatch(N * 2);

                AFactory aFactory = new AFactory();
                aFactory.register(N, m_exec);

                try {
                    if (!m_bindALatch.await(5000, TimeUnit.MILLISECONDS)) {
                        super.log(LOG_ERROR, "bind problem.");
                        return;
                    }
                } catch (InterruptedException e) {
                }

                aFactory.unregister(N, m_exec);

                if (!m_unbindALatch.await(5000, TimeUnit.MILLISECONDS)) {
                    super.log(LOG_ERROR, "unbind problem.");
                    return;
                }

                if ((++loop) % 1000 == 0) {
                    System.out.println("Performed " + loop + " tests.");
                }
            }
        } catch (InterruptedException e) {
            return;
        } catch (Throwable t) {
            super.log(LOG_ERROR, "error", t);
            return;
        }
        finally {
            m_stopLatch.countDown();
        }
    }

    public static class A {
        void foo() {
        }
    }

    class AFactory {
        ConcurrentLinkedQueue<ServiceRegistration> m_regs = new ConcurrentLinkedQueue<ServiceRegistration>();

        public void register(int n, Executor exec) {
            for (int i = 0; i < n; i++) {
                m_exec.execute(new Runnable() {
                    public void run() {
                        try {
                            A instance = new A();
                            m_regs.add(m_bctx.registerService(instance.getClass().getName(), instance, null));
                            m_bindALatch.countDown();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        public void unregister(int n, Executor exec) {
            for (int i = 0; i < n; i++) {
                m_exec.execute(new Runnable() {
                    public void run() {
                        try {
                            ServiceRegistration sr = m_regs.poll();
                            if (sr != null) {
                                sr.unregister();
                                m_unbindALatch.countDown();
                            }
                        } catch (Throwable e) {
                            log(LOG_ERROR, "error", e);
                        }
                    }
                });
            }
        }
    }

    public static class Controller {
        final Composition m_compo = new Composition();

        Object[] getComposition() {
            return new Object[] { this, m_compo };
        }

        void bindA(ServiceReference sr) {
            A a = (A) sr.getBundle().getBundleContext().getService(sr);
            if (a == null) {
                throw new IllegalStateException("bindA: bundleContext.getService returned null");
            }
            a.foo();
            m_bindALatch.countDown();
        }

        void unbindA(A a) {
            m_unbindALatch.countDown();
        }
    }

    public static class Composition {
    }
}
