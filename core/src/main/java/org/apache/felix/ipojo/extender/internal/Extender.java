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

package org.apache.felix.ipojo.extender.internal;

import org.apache.felix.ipojo.EventDispatcher;
import org.apache.felix.ipojo.extender.internal.linker.DeclarationLinker;
import org.apache.felix.ipojo.extender.internal.processor.ChainedBundleProcessor;
import org.apache.felix.ipojo.extender.internal.processor.ComponentsBundleProcessor;
import org.apache.felix.ipojo.extender.internal.processor.ExtensionBundleProcessor;
import org.apache.felix.ipojo.extender.internal.processor.QueuingActivationProcessor;
import org.apache.felix.ipojo.extender.internal.queue.ExecutorQueueService;
import org.apache.felix.ipojo.extender.internal.queue.PrefixedThreadFactory;
import org.apache.felix.ipojo.extender.internal.queue.SynchronousQueueService;
import org.apache.felix.ipojo.extender.internal.queue.pref.HeaderPreferenceSelection;
import org.apache.felix.ipojo.extender.internal.queue.pref.Preference;
import org.apache.felix.ipojo.extender.internal.queue.pref.PreferenceQueueService;
import org.apache.felix.ipojo.extender.internal.queue.pref.enforce.EnforcedQueueService;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.*;

/**
 * iPOJO main activator.
 */
public class Extender implements BundleActivator, SynchronousBundleListener {
    /**
     * Enables the iPOJO internal dispatcher.
     * This internal dispatcher helps the OSGi framework to support large
     * scale applications. The internal dispatcher is disabled by default.
     */
    static boolean DISPATCHER_ENABLED = true;

    /**
     * Disables the iPOJO asynchronous processing.
     * When set to false, the bundles are processed in the listener thread
     * making iPOJO usable on Google App Engine. By default, the processing
     * is asynchronous.
     */
    static boolean SYNCHRONOUS_PROCESSING_ENABLED = false;

    /**
     * Property allowing to set if the internal dispatcher is enabled or disabled.
     * Possible value are either {@literal true} or {@literal false}.
     */
    private static final String ENABLING_DISPATCHER = "ipojo.internal.dispatcher";

    /**
     * Property allowing to disable the asynchronous process (and so enables the
     * synchronous processing).
     * Possible value are either {@literal true} or {@literal false}.
     */
    private static final String SYNCHRONOUS_PROCESSING = "ipojo.processing.synchronous";

    /**
     * The Bundle Context of the iPOJO Core bundle.
     */
    private static BundleContext m_context;

    /**
     * The iPOJO Extender logger.
     */
    private Logger m_logger;

    /**
     * The iPOJO Bundle.
     */
    private Bundle m_bundle;

    /**
     * The chained processor containing all the true bundle processor.
     */
    private ChainedBundleProcessor m_processor;

    /**
     * Binds Instances to Factories to Extensions.
     */
    private DeclarationLinker m_linker;

    private LifecycleQueueService m_queueService;

    /**
     * The iPOJO bundle is starting.
     * This method configures the iPOJO system (internal dispatcher and bundle processing). Then it initiates the
     * bundle processing.
     * <p/>
     * To optimize the processing, we process the iPOJO bundle first.
     *
     * @param context the iPOJO's bundle bundle context
     * @throws Exception something terrible happen during startup
     */
    public void start(BundleContext context) throws Exception {
        m_context = context;
        m_bundle = context.getBundle();

        m_logger = new Logger(m_context, "IPOJO-Main-Extender");

        enablingDispatcher(context, m_logger);
        enablingSynchronousProcessing(context, m_logger);

        // Create the dispatcher only if required.
        if (DISPATCHER_ENABLED) {
            EventDispatcher.create(context);
        }

        BundleProcessor extensionBundleProcessor = new ExtensionBundleProcessor(m_logger);
        BundleProcessor componentsProcessor = new ComponentsBundleProcessor(m_logger);
        if (SYNCHRONOUS_PROCESSING_ENABLED) {
            m_queueService = new EnforcedQueueService(
                    new HeaderPreferenceSelection(),
                    new SynchronousQueueService(context),
                    Preference.SYNC,
                    m_logger);
        } else {
            SynchronousQueueService sync = new SynchronousQueueService(context);
            ExecutorQueueService async = new ExecutorQueueService(context, 1, new PrefixedThreadFactory("[iPOJO] "));
            m_queueService = new PreferenceQueueService(new HeaderPreferenceSelection(), sync, async);

            extensionBundleProcessor = new QueuingActivationProcessor(extensionBundleProcessor, m_queueService);
            componentsProcessor = new QueuingActivationProcessor(componentsProcessor, m_queueService);
        }
        m_queueService.start();

        // Start linking
        m_linker = new DeclarationLinker(context, m_queueService);
        m_linker.start();

        m_processor = ChainedBundleProcessor.create(extensionBundleProcessor, componentsProcessor);

        m_processor.start();

        // Begin by initializing core handlers
        m_processor.activate(m_bundle);

        synchronized (this) {
            // listen to any changes in bundles.
            m_context.addBundleListener(this);
            // compute already started bundles.
            for (int i = 0; i < context.getBundles().length; i++) {
                if (context.getBundles()[i].getState() == Bundle.ACTIVE) {
                    m_processor.activate(context.getBundles()[i]);
                }
            }
        }

        m_logger.log(Logger.INFO, "iPOJO Main Extender started");
    }

    /**
     * The iPOJO bundle is stopping.
     *
     * @param context the bundle context
     * @throws Exception something terrible happen
     */
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);

        m_processor.stop();

        if (DISPATCHER_ENABLED) {
            EventDispatcher.dispose();
        }

        m_linker.stop();
        m_queueService.stop();

        m_logger.log(Logger.INFO, "iPOJO Main Extender stopped");
        m_context = null;
    }

    /**
     * A bundle event was caught.
     *
     * @param event the event
     */
    public void bundleChanged(BundleEvent event) {
        if (m_bundle.getBundleId() != (event.getBundle().getBundleId())) {
            // Do not process our-self (already done)
            switch (event.getType()) {
                case BundleEvent.STARTED:
                    // Put the bundle in the queue
                    m_processor.activate(event.getBundle());
                    break;
                case BundleEvent.STOPPING:
                    m_processor.deactivate(event.getBundle());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Gets iPOJO bundle context.
     *
     * @return the iPOJO Bundle Context
     */
    public static BundleContext getIPOJOBundleContext() {
        return m_context;
    }

    /**
     * Enables or disables the internal dispatcher, so sets the
     * {@link Extender#DISPATCHER_ENABLED} flag.
     * This method checks if the {@link Extender#ENABLING_DISPATCHER}
     * property is set to {@literal true}. Otherwise, the internal
     * dispatcher is disabled. The property can be set as a system
     * property ({@literal ipojo.internal.dispatcher}) or inside the
     * iPOJO bundle manifest ({@literal ipojo-internal-dispatcher}).
     *
     * @param context the bundle context.
     * @param logger  the logger to indicates if the internal dispatcher is set.
     */
    private static void enablingDispatcher(BundleContext context, Logger logger) {
        // First check in the framework and in the system properties
        String flag = context.getProperty(ENABLING_DISPATCHER);

        // If null, look in bundle manifest
        if (flag == null) {
            String key = ENABLING_DISPATCHER.replace('.', '-');
            flag = (String) context.getBundle().getHeaders().get(key);
        }

        if (flag != null) {
            if (flag.equalsIgnoreCase("true")) {
                Extender.DISPATCHER_ENABLED = true;
                logger.log(Logger.INFO, "iPOJO Internal Event Dispatcher enables");
                return;
            }
        }

        // Either l is null, or the specified value was false
        Extender.DISPATCHER_ENABLED = false;
        logger.log(Logger.INFO, "iPOJO Internal Event Dispatcher disables");

    }

    /**
     * Enables or disables the asynchronous processing, so sets the
     * {@link Extender#SYNCHRONOUS_PROCESSING_ENABLED} flag.
     * Disabling asynchronous processing avoids iPOJO to create a new
     * thread to process bundles. So, iPOJO can be used on the
     * Google App Engine.
     * This method checks if the {@link Extender#SYNCHRONOUS_PROCESSING}
     * property is set to {@literal true}. Otherwise, asynchronous processing
     * is used (default). The property can be set as a system
     * property ({@literal ipojo.processing.synchronous}) or inside the
     * iPOJO bundle manifest.
     *
     * @param context the bundle context.
     * @param logger  the logger to indicates if the internal dispatcher is set.
     */
    private static void enablingSynchronousProcessing(BundleContext context, Logger logger) {
        String flag = context.getProperty(SYNCHRONOUS_PROCESSING);

        // If null, look in bundle manifest
        if (flag == null) {
            String key = SYNCHRONOUS_PROCESSING.replace('.', '-');
            flag = (String) context.getBundle().getHeaders().get(key);
        }

        if (flag != null) {
            if (flag.equalsIgnoreCase("true")) {
                Extender.SYNCHRONOUS_PROCESSING_ENABLED = true;
                logger.log(Logger.INFO, "iPOJO Asynchronous processing disabled");
                return;
            }
        }

        // Either l is null, or the specified value was false
        Extender.SYNCHRONOUS_PROCESSING_ENABLED = false;
        logger.log(Logger.INFO, "iPOJO synchronous processing disabled");

    }


}
