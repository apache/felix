/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl.dependencies;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyService;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.TemporalServiceDependency;
import org.apache.felix.dm.impl.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Temporal Service dependency implementation, used to hide temporary service dependency "outage".
 * Only works with a required dependency.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TemporalServiceDependencyImpl extends ServiceDependencyImpl implements TemporalServiceDependency, InvocationHandler {
    // Max millis to wait for service availability.
    private long m_timeout = 30000;
    
    // Dependency service currently used (with highest rank or highest service id).
    private volatile Object m_cachedService;
    
    // Framework bundle (we use it to detect if the framework is stopping)
    private final Bundle m_frameworkBundle;

    // The service proxy, which blocks when service is not available.
    private Object m_serviceInstance;

    /**
     * Creates a new Temporal Service Dependency.
     * 
     * @param context The bundle context of the bundle which is instantiating this dependency object
     * @param logger the logger our Internal logger for logging events.
     * @see DependencyActivatorBase#createTemporalServiceDependency()
     */
    public TemporalServiceDependencyImpl(BundleContext context, Logger logger) {
        super(context, logger);
        super.setRequired(true);
        m_frameworkBundle = context.getBundle(0);
    }

    /**
     * Sets the timeout for this temporal dependency. Specifying a timeout value of zero means that there is no timeout period,
     * and an invocation on a missing service will fail immediately.
     * 
     * @param timeout the dependency timeout value greater or equals to 0
     * @throws IllegalArgumentException if the timeout is negative
     * @return this temporal dependency
     */
    public synchronized TemporalServiceDependency setTimeout(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout value: " + timeout);
        }
        m_timeout = timeout;
        return this;
    }

    /**
     * Sets the required flag which determines if this service is required or not. This method
     * just override the superclass method in order to check if the required flag is true 
     * (optional dependency is not supported by this class).
     * 
     * @param required the required flag, which must be set to true
     * @return this service dependency
     * @throws IllegalArgumentException if the "required" parameter is not true.
     */
    //@Override
    public ServiceDependency setRequired(boolean required) {
        if (! required) {
            throw new IllegalArgumentException("A Temporal Service dependency can't be optional");
        }
        super.setRequired(required);
        return this;
    }

    /**
     * The ServiceTracker calls us here in order to inform about a service arrival.
     */
    //@Override
    public synchronized void addedService(ServiceReference ref, Object service) {
        // Update our service cache, using the tracker. We do this because the
        // just added service might not be the service with the highest rank ...
        m_cachedService = m_tracker.getService(); 
        boolean makeAvailable = makeAvailable();
        if (makeAvailable) {
            m_serviceInstance = Proxy.newProxyInstance(m_trackedServiceName.getClassLoader(), new Class[] { m_trackedServiceName }, this);
        }
        Object[] services = m_services.toArray();
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            if (makeAvailable) {
                ds.dependencyAvailable(this);
            }
        }
        if (!makeAvailable) {
            notifyAll();
        }
    }

    /**
     * The ServiceTracker calls us here when a tracked service properties are modified.
     */
    //@Override
    public void modifiedService(ServiceReference ref, Object service) {
        // We don't care.
    }

    /**
     * The ServiceTracker calls us here when a tracked service is lost.
     */
    //@Override
    public synchronized void removedService(ServiceReference ref, Object service) {
        // If we detect that the fwk is stopping, we behave as our superclass. That is:
        // the lost dependency has to trigger our service deactivation, since the fwk is stopping
        // and the lost dependency won't come up anymore.
        if (m_frameworkBundle.getState() == Bundle.STOPPING) {
            // Important: Notice that calling "super.removedService() might invoke our service "stop" 
            // callback, which in turn might invoke the just removed service dependency. In this case, 
            // our "invoke" method won't use the tracker to get the service dependency (because at this point, 
            // the tracker has withdrawn its reference to the lost service). So, you will see that the "invoke" 
            // method will use the "m_cachedService" instead ...
            super.removedService(ref, service);
        } else {
            // Unget what we got in addingService (see ServiceTracker 701.4.1)
            m_context.ungetService(ref);
            // Now, ask the service tracker if there is another available service (with a lower rank).
            // If no more service dependencies are available, the tracker will then return null;
            // and our invoke method will block the service method invocation, until another service
            // becomes available.
            m_cachedService = m_tracker.getService();
        }
    }

    /**
     * @returns our dependency instance. Unlike in ServiceDependency, we always returns our proxy.
     */
    //@Override
    protected synchronized Object getService() {
        return m_serviceInstance;
    }

    //@Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object service = m_cachedService;
        if (service == null) {
            synchronized (this) {
                long start = System.currentTimeMillis();
                long waitTime = m_timeout;
                while (service == null) {
                    if (waitTime <= 0) {
                        throw new IllegalStateException("Service unavailable: " + m_trackedServiceName.getName());
                    }
                    try {
                        wait(waitTime);
                    }
                    catch (InterruptedException e) {
                        throw new IllegalStateException("Service unavailable: " + m_trackedServiceName.getName());
                    }
                    waitTime = m_timeout - (System.currentTimeMillis() - start);
                    service = m_cachedService;
                }
                
            }
        }
        try {
            return method.invoke(service, args);
        }
        catch (IllegalAccessException iae) {
            method.setAccessible(true);
            return method.invoke(service, args);
        }
    }
}
