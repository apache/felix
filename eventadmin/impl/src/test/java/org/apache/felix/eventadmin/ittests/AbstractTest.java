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
package org.apache.felix.eventadmin.ittests;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.AbstractDelegateProvisionOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(PaxExam.class)
public abstract class AbstractTest implements Runnable {

    // the name of the system property providing the bundle file to be installed and tested
    private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    /** The logger. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    protected BundleContext bundleContext;

    /** Event admin. */
    private EventAdmin eventAdmin;

    /** Event admin reference. */
    private ServiceReference eventAdminReference;

    private volatile boolean running = false;

    private volatile int nrEvents;

    private volatile int eventCount = 0;

    private volatile int finishEventCount;

    private volatile String prefix;

    private volatile long startTime;
    
    private final Queue<Event> eventRecievedList = new ConcurrentLinkedQueue();

    /** Wait lock for syncing. */
    private final Object waitLock = new Object();

    /**
     * Handle an event from the listener.
     * @param event
     * @param payload
     */
    public void handleEvent(final Event event, final Object payload) {
        if ( this.running ) {
            if ( prefix == null || event.getTopic().startsWith(prefix) ) {
                incCount();
                eventRecievedList.offer(event);
            }
        }
    }

    /**
     * Send an event
     * @param topic
     * @param properties
     * @param sync
     */
    protected void send(String topic, Dictionary<String, Object> properties, int index, boolean sync) {
        
    	if(properties == null)
    	{
    		properties = new Hashtable();
    	}
        properties.put("thread", Thread.currentThread().getId());
        properties.put("index", index);
    	final Event event = new Event(topic, properties);
        if ( sync ) {
            getEventAdmin().sendEvent(event);
        } else {
            getEventAdmin().postEvent(event);
        }
    }

    private synchronized void incCount() {
        eventCount++;
        if ( eventCount >= finishEventCount) {
            final long duration = this.startTime == -1 ? -1 : System.currentTimeMillis() - this.startTime;
            logger.info("Finished tests, received {} events in {}ms", eventCount, duration);
        }
    }

    private synchronized int getCount() {
        return eventCount;
    }

    protected void start(final String prefix, final int nrThreads, final int nrEvents, final int finishCount) {
        logger.info("Starting eventing test {}", this.getClass().getSimpleName());

        this.startTime = -1;
        this.prefix = prefix;
        this.nrEvents = nrEvents;
        finishEventCount = finishCount;
        eventCount = 0;
        logger.info("Preparing test with {} threads and {} events per thread.", nrThreads, nrEvents);
        logger.info("Expecting {} events.", finishCount);
        this.running = true;
        final Thread[] threads = new Thread[nrThreads];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(this);
        }
        for(int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        final Thread infoT = new Thread(new Runnable() {
            @Override
            public void run() {
                while ( running ) {
                    logger.info("Received {} events so far.", getCount());
                    try {
                        Thread.sleep(1000 * 5);
                    } catch (final InterruptedException ignore) {
                        // ignore
                    }
                    if ( getCount() >= finishCount) {
                        running = false;
                        synchronized ( waitLock ) {
                            waitLock.notify();
                        }
                    }
                }
            }
        });
        infoT.start();
        logger.info("Started test with {} threads and {} events.", nrThreads, nrEvents);
        this.execute();
     }

    private void waitForFinish() {
        while ( this.running ) {
            synchronized ( this.waitLock ) {
                try {
                    this.waitLock.wait();
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    protected abstract void sendEvent(final int index);

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            Thread.sleep(1000 * 10);
        } catch (final InterruptedException ignore) {
            // ignore
        }
        synchronized ( this ) {
            if ( this.startTime == -1 ) {
                this.startTime = System.currentTimeMillis();
            }
        }
        logger.info("Started thread");
        int index = 0;
        while ( this.running && index < nrEvents ) {
            this.sendEvent(index);

            index++;
        }
        logger.info("Send {} events.", index);
    }

    private final List<Listener> listeners = new ArrayList<Listener>();

    public void addListener(final String topic, final Object payload) {
        this.listeners.add(new Listener(this.bundleContext, this, topic, payload));
    }

    private void stop() {
        for(final Listener l : listeners) {
            l.dispose();
        }
        listeners.clear();
    }

    /**
     * Helper method to get a service of the given type
     */
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> clazz) {
    	final ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
    	assertNotNull("getService(" + clazz.getName() + ") must find ServiceReference", ref);
    	final T result = (T)(bundleContext.getService(ref));
    	assertNotNull("getService(" + clazz.getName() + ") must find service", result);
    	return result;
    }

    protected EventAdmin getEventAdmin() {
        if ( eventAdminReference == null || eventAdminReference.getBundle() == null ) {
            eventAdmin = null;
            eventAdminReference = bundleContext.getServiceReference(EventAdmin.class.getName());
        }
        if ( eventAdmin == null && eventAdminReference != null ) {
            eventAdmin = (EventAdmin) bundleContext.getService(eventAdminReference);
        }
        return eventAdmin;
    }

    @Configuration
    public static Option[] configuration() {
        final String bundleFileName = System.getProperty( BUNDLE_JAR_SYS_PROP );
        final File bundleFile = new File( bundleFileName );
        if ( !bundleFile.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from bundle file " + bundleFileName + " specified in the "
                + BUNDLE_JAR_SYS_PROP + " system property" );
        }

        return options(
            provision(
                mavenBundle( "org.ops4j.pax.tinybundles", "tinybundles", "1.0.0" ),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "2.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.0.4"),
                CoreOptions.bundle( bundleFile.toURI().toString() ),
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.3.5")
             ),
             // below is instead of normal Pax Exam junitBundles() to deal
             // with build server issue
             new DirectURLJUnitBundlesOption(),
             systemProperty("pax.exam.invoker").value("junit"),
             //vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"), 
             bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link")
        );
    }

    /**
     * Execute a single test.
     * @param test
     */
    private void execute() {
        this.waitForFinish();
        this.stop();
        logger.info("Finished eventing test {}", this.getClass().getSimpleName());
        Event currentEvent = null;
        Map<Long, Integer> orderVerifyMap = new HashMap<Long, Integer>();
        while((currentEvent =eventRecievedList.poll()) != null)
        {
        	Integer index = (Integer)currentEvent.getProperty("index");
        	Long threadId = (Long)currentEvent.getProperty("thread");
        	
        	if(index != null && threadId != null){
        		Integer previousIndex = orderVerifyMap.get(threadId);
        		if(previousIndex == null)
        		{
        			if(index != 0)
        			{
        				System.out.println("Event " + index + " recieved first for thread " + threadId);
        			}
        			orderVerifyMap.put(threadId, index);
        		}
        		else
        		{
        			if(previousIndex > index)
        			{
        				System.out.println("Events for thread " + threadId + " out of order.  Event " + previousIndex + " recieved before " + index);
        			}
        			else
        			{
        				orderVerifyMap.put(threadId, index);
        			}
        		}
        	}
        }
        try {
            Thread.sleep(15 * 1000);
        } catch (final InterruptedException ie) {
            // ignore
        }
    }


    /**
     * Clone of Pax Exam's JunitBundlesOption which uses a direct
     * URL to the SpringSource JUnit bundle to avoid some weird
     * repository issues on the Apache build server.
     */
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