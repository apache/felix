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
package org.apache.felix.configurator.impl;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The {@code ServicesListener} listens for the required services
 * and starts the configurator when all services are available.
 * It also handles optional services
 */
public class ServicesListener {

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The listener for the config admin. */
    private final Listener caListener;

    /** The listener for the coordinator. */
    private final Listener coordinatorListener;

    /** The current configurator. */
    private volatile Configurator configurator;

    /**
     * Start listeners
     */
    public ServicesListener(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.caListener = new Listener(ConfigurationAdmin.class.getName());
        this.coordinatorListener = new Listener("org.osgi.service.coordinator.Coordinator");
        this.caListener.start();
        this.coordinatorListener.start();
        SystemLogger.debug("Started services listener for configurator.");
    }

    /**
     * Notify of service changes from the listeners.
     * If all services are available, start
     */
    public synchronized void notifyChange() {
        // check if all services are available
        final ConfigurationAdmin ca = (ConfigurationAdmin)this.caListener.getService();
        final Object coordinator = this.coordinatorListener.getService();
        SystemLogger.debug("Services updated for configurator: " + ca + " - " + coordinator);

        if ( ca != null ) {
            boolean isNew = configurator == null;
            if ( isNew ) {
                SystemLogger.debug("Starting new configurator");
                configurator = new Configurator(this.bundleContext, ca);
            }
            configurator.setCoordinator(coordinator);
            if ( isNew ) {
                configurator.start();
            }
        } else {
            if ( configurator != null ) {
                SystemLogger.debug("Stopping configurator");
                configurator.shutdown();
                configurator = null;
            }
        }
    }

    /**
     * Deactivate this listener.
     */
    public void deactivate() {
        this.caListener.deactivate();
        this.coordinatorListener.deactivate();
        if ( configurator != null ) {
            configurator.shutdown();
            configurator = null;
        }
    }

    /**
     * Helper class listening for service events for a defined service.
     */
    protected final class Listener implements ServiceListener {

        /** The name of the service. */
        private final String serviceName;

        /** The service reference. */
        private volatile ServiceReference<?> reference;

        /** The service. */
        private volatile Object service;

        /**
         * Constructor
         */
        public Listener(final String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * Start the listener.
         * First register a service listener and then check for the service.
         */
        public void start() {
            try {
                bundleContext.addServiceListener(this, "("
                        + Constants.OBJECTCLASS + "=" + serviceName + ")");
            } catch (final InvalidSyntaxException ise) {
                // this should really never happen
                throw new RuntimeException("Unexpected exception occured.", ise);
            }
            this.retainService();
        }

        /**
         * Unregister the listener.
         */
        public void deactivate() {
            bundleContext.removeServiceListener(this);
        }

        /**
         * Return the service (if available)
         */
        public synchronized Object getService() {
            return this.service;
        }

        /**
         * Try to get the service and notify the change.
         */
        private synchronized void retainService() {
            if ( this.reference == null ) {
                this.reference = bundleContext.getServiceReference(this.serviceName);
                if ( this.reference != null ) {
                    this.service = bundleContext.getService(this.reference);
                    if ( this.service == null ) {
                        this.reference = null;
                    } else {
                        notifyChange();
                    }
                }
            }
        }

        /**
         * Try to release the service and notify the change.
         */
        private synchronized void releaseService(final ServiceReference<?> ref) {
            if ( this.reference != null && this.reference.compareTo(ref) == 0 ) {
                this.service = null;
                bundleContext.ungetService(this.reference);
                this.reference = null;
                notifyChange();
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        @Override
        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                this.retainService();
            } else if ( event.getType() == ServiceEvent.UNREGISTERING ) {
                this.releaseService(event.getServiceReference());
                this.retainService();
            }
        }
    }
}
