/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.jmx.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor;
import org.apache.felix.hc.core.impl.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates an {@link HealthCheckMBean} for every {@link HealthCheckMBean} service */
@Component
public class HealthCheckMBeanCreator {

    private static final String JMX_TYPE_NAME = "HealthCheck";
    private static final String JMX_DOMAIN = "org.apache.felix.healthcheck";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<ServiceReference<HealthCheck>, Registration> registeredServices = new HashMap<>();

    private final Map<String, List<ServiceReference<HealthCheck>>> sortedRegistrations = new HashMap<>();

    private ServiceTracker<HealthCheck, ?> hcTracker;

    @Reference
    private ExtendedHealthCheckExecutor executor;

    @Activate
    protected void activate(final BundleContext btx) {
        try {
            Filter filter = btx.createFilter("(&"+HealthCheckFilter.HC_FILTER_OBJECT_CLASS+"("+HealthCheck.MBEAN_NAME+"=*))");
            this.hcTracker = new HealthCheckServiceTracker(btx, filter);
            this.hcTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.warn("Could not create service tracker for filter "+HealthCheckFilter.HC_FILTER_OBJECT_CLASS, e);
        }
        
    }

    @Deactivate
    protected void deactivate() {
        if (this.hcTracker != null) {
            this.hcTracker.close();
            this.hcTracker = null;
        }
    }

    /** Register an mbean for a health check service. The mbean is only registered if - the service has an mbean registration property - if
     * there is no other service with the same name but a higher service ranking
     *
     * @param bundleContext The bundle context
     * @param reference The service reference to the health check service
     * @return The registered mbean or <code>null</code> */
    private synchronized Object registerHCMBean(final BundleContext bundleContext, final ServiceReference<HealthCheck> reference) {
        final Registration reg = getRegistration(reference);
        if (reg != null) {
            this.registeredServices.put(reference, reg);

            List<ServiceReference<HealthCheck>> registered = this.sortedRegistrations.get(reg.name);
            if (registered == null) {
                registered = new ArrayList<>();
                this.sortedRegistrations.put(reg.name, registered);
            }
            registered.add(reference);
            // sort orders the references with lowest ranking first
            // we want the highest!
            Collections.sort(registered);
            final int lastIndex = registered.size() - 1;
            if (registered.get(lastIndex).equals(reference)) {
                if (registered.size() > 1) {
                    final ServiceReference<HealthCheck> prevRef = registered.get(lastIndex - 1);
                    final Registration prevReg = this.registeredServices.get(prevRef);
                    prevReg.unregister();
                }
                reg.register(bundleContext);
            }
        }
        return reg;
    }

    private synchronized void unregisterHCMBean(final BundleContext bundleContext, final ServiceReference<HealthCheck> ref) {
        final Registration reg = registeredServices.remove(ref);
        if (reg != null) {
            final boolean registerFirst = reg.unregister();
            final List<ServiceReference<HealthCheck>> registered = this.sortedRegistrations.get(reg.name);
            registered.remove(ref);
            if (registered.size() == 0) {
                this.sortedRegistrations.remove(reg.name);
            } else if (registerFirst) {
                final ServiceReference<HealthCheck> newRef = registered.get(0);
                final Registration newReg = this.registeredServices.get(newRef);
                newReg.register(bundleContext);
            }
            bundleContext.ungetService(ref);
        }
    }

    private final class HealthCheckServiceTracker extends ServiceTracker<HealthCheck, Object> {
        private final BundleContext bundleContext;

        private HealthCheckServiceTracker(BundleContext bundleContext, Filter filter) {
            super(bundleContext, filter, null);
            this.bundleContext = bundleContext;
        }

        @Override
        public Object addingService(final ServiceReference<HealthCheck> reference) {
            return registerHCMBean(bundleContext, reference);
        }

        @Override
        public void modifiedService(final ServiceReference<HealthCheck> reference,
                final Object service) {
            unregisterHCMBean(bundleContext, reference);
            registerHCMBean(bundleContext, reference);
        }

        @Override
        public void removedService(final ServiceReference<HealthCheck> reference,
                final Object service) {
            unregisterHCMBean(bundleContext, reference);
        }
    }

    private final class Registration {
        private final String name;
        private final HealthCheckMBean mbean;

        private final String objectName;

        private ServiceRegistration<DynamicMBean> registration;

        Registration(final String name, final HealthCheckMBean mbean) {
            this.name = name;
            this.mbean = mbean;
            objectName = String.format("%s:type=%s,name=%s", JMX_DOMAIN, JMX_TYPE_NAME, name);
        }

        void register(final BundleContext btx) {
            logger.debug("Registering health check mbean {} with name {}", mbean, objectName);
            final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
            mbeanProps.put("jmx.objectname", objectName);
            this.registration = btx.registerService(DynamicMBean.class, this.mbean, mbeanProps);
        }

        boolean unregister() {
            if (this.registration != null) {
                logger.debug("Unregistering health check mbean {} with name {}", mbean, objectName);
                this.registration.unregister();
                this.registration = null;
                return true;
            }
            return false;
        }
    }

    private Registration getRegistration(final ServiceReference<HealthCheck> ref) {
        final String hcMBeanName = (String) ref.getProperty(HealthCheck.MBEAN_NAME);
        if (StringUtils.isNotBlank(hcMBeanName)) {
            final HealthCheckMBean mbean = new HealthCheckMBean(ref, executor);
            return new Registration(hcMBeanName.replace(',', '.'), mbean);
        } else {
            logger.warn("Service reference {} unexpectedly has property {} not set", ref, HealthCheck.MBEAN_NAME);
            return null;
        }
    }

}
