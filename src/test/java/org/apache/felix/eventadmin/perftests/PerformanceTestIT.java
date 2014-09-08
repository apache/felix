/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.eventadmin.perftests;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.AbstractDelegateProvisionOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
public class PerformanceTestIT {
    // the name of the system property providing the bundle file to be installed and tested
    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    /** The logger. */
    protected static final Logger logger = LoggerFactory.getLogger(PerformanceTestIT.class);
    private static final int RUNS = 5;
    public static final int BATCH_SIZE = 500000;

    @Inject
    protected BundleContext bundleContext;

    /** Event admin reference. */
    private ServiceReference eventAdminReference;

    /** Event admin. */
    private EventAdmin eventAdmin;

    final AtomicLong counter = new AtomicLong();

    Collection<Listener> listeners = new ArrayList<Listener>();

    @Configuration
    public static Option[] configuration() {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        logger.info("Bundle jar at :"+bundleFileName);
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                    + BUNDLE_JAR_SYS_PROP + " system property" );
        }
        return options(
                vmOption("-Xms1024m"),
//                vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
                provision(
                        mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                        mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "2.1.2"),
                        mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.8"),
                        mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.0.4"),
                        CoreOptions.bundle(bundleFile.toURI().toString()),
                        mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.3.5")
                ),
                // below is instead of normal Pax Exam junitBundles() to deal
                // with build server issue
                new DirectURLJUnitBundlesOption(),
                systemProperty("pax.exam.invoker").value("junit"),
                bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link")
        );
    }

    protected EventAdmin loadEventAdmin() {
        if ( eventAdminReference == null || eventAdminReference.getBundle() == null ) {
            eventAdmin = null;
            eventAdminReference = bundleContext.getServiceReference(EventAdmin.class.getName());
        }
        if ( eventAdmin == null && eventAdminReference != null ) {
            eventAdmin = (EventAdmin) bundleContext.getService(eventAdminReference);
        }
        return eventAdmin;
    }

    public void addListener(Listener listener, String... topics) {
        listener.register(bundleContext,topics);
        listeners.add(listener);
    }

    private void removeListener(Listener listener) {
        listener.unregister();
    }


    protected void send(String topic, Dictionary<String, Object> properties, boolean sync) {
        final Event event = new Event(topic, properties);
        if ( sync ) {
            eventAdmin.sendEvent(event);
        } else {
            eventAdmin.postEvent(event);
        }
    }


    @Test
    public void measureThroughputSend() {
        loadEventAdmin();
        addListener(new Listener() {
            @Override
            public void handleEvent(Event event) {
                long calledTimes = counter.incrementAndGet();
                if (calledTimes == BATCH_SIZE ) {
                    synchronized (counter) {
                        counter.notify();
                    }
                }

            }
        }, "topic");

        // Warm-up
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        for (int i= 0;i < BATCH_SIZE;i++) {
            properties.put("key",i);
            send("topic", properties, false);
        }
        int average =0;
        for (int runs = 0; runs < RUNS;runs ++) {

            final CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
            addListener(new Listener() {
                @Override
                public void handleEvent(Event event) {
                    latch.countDown();
                }
            }, "topic" + runs);

            ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(BATCH_SIZE+1);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(1,
                    1,
                    1000,
                    TimeUnit.MILLISECONDS, workQueue);


            for (int i = 0; i < BATCH_SIZE; i++) {
                final String topicString = "topic"+runs;
                final Hashtable<String,Object> localProperties = new Hashtable<String, Object>();
                localProperties.put(topicString,i);
                workQueue.add(new Runnable() {
                    @Override
                    public void run() {
                        send(topicString, localProperties, true);
                    }
                });
            }

            long startTime = System.nanoTime();
            executor.prestartAllCoreThreads();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long endTime = System.nanoTime();
            long milliseconds = (endTime - startTime) / 1000000;
            logger.info("Post Run "+runs+" Elapsed :" + milliseconds);
            average += milliseconds;
        }

        logger.info("Send Avg: "+average / RUNS);
    }

    @Test
    public void measureThroughputPost() {
        loadEventAdmin();
        addListener(new Listener() {
            @Override
            public void handleEvent(Event event) {
                long calledTimes = counter.incrementAndGet();
                if (calledTimes == BATCH_SIZE ) {
                    synchronized (counter) {
                        counter.notify();
                    }
                }

            }
        }, "topic");

        // Warm-up
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        for (int i= 0;i < BATCH_SIZE;i++) {
            properties.put("key",i);
            send("topic", properties, false);
        }
        int average =0;
        for (int runs = 0; runs < RUNS;runs ++) {

            final CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
            addListener(new Listener() {
                @Override
                public void handleEvent(Event event) {
                    latch.countDown();
                }
            }, "topic" + runs);

            ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(BATCH_SIZE+1);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    1000,
                    TimeUnit.MILLISECONDS, workQueue);


            for (int i = 0; i < BATCH_SIZE; i++) {
                final String topicString = "topic"+runs;
                final Hashtable<String,Object> localProperties = new Hashtable<String, Object>();
                localProperties.put(topicString,i);
                workQueue.add(new Runnable() {
                    @Override
                    public void run() {
                        send(topicString, localProperties, false);
                    }
                });
            }

            long startTime = System.nanoTime();
            executor.prestartAllCoreThreads();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long endTime = System.nanoTime();
            long milliseconds = (endTime - startTime) / 1000000;
            logger.info("Post Run "+runs+" Elapsed :" + milliseconds);
            average += milliseconds;
        }

        logger.info("Post Avg: "+average / RUNS);
    }

    @After
    public void tearDown() {
        for (Listener listener : listeners) {
            removeListener(listener);
        }
    }



    private static abstract class Listener implements EventHandler {
        private ServiceRegistration registration;

        protected Listener() {
        }

        public void register(BundleContext bundleContext, String...topics) {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            if ( topics != null ) {
                props.put("event.topics", topics);
            } else {
                props.put("event.topics", "*");
            }
            this.registration = bundleContext.registerService(EventHandler.class.getName(), this, props);
        }

        public void unregister() {
            registration.unregister();
        }
    }


    private static class DirectURLJUnitBundlesOption
            extends AbstractDelegateProvisionOption<DirectURLJUnitBundlesOption> {

        /**
         * Constructor.
         */
        public DirectURLJUnitBundlesOption(){
            super(
                    bundle("http://repository.springsource.com/ivy/bundles/external/org.junit/com.springsource.org.junit/4.9.0/com.springsource.org.junit-4.9.0.jar")
            );
            noUpdate();
            startLevel(START_LEVEL_SYSTEM_BUNDLES);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("DirectURLJUnitBundlesOption{url=%s}", getURL());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DirectURLJUnitBundlesOption itself() {
            return this;
        }

    }
}
