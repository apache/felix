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
package org.apache.felix.framework;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;

import org.apache.felix.framework.ext.FelixBundleContext;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

class BundleContextImpl implements FelixBundleContext
{
    private Logger m_logger = null;
    private Felix m_felix = null;
    private BundleImpl m_bundle = null;
    private boolean m_valid = true;

    protected BundleContextImpl(Logger logger, Felix felix, BundleImpl bundle)
    {
        m_logger = logger;
        m_felix = felix;
        m_bundle = bundle;
    }

    protected void invalidate()
    {
        m_valid = false;
    }

    public void addRequirement(String s) throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void removeRequirement() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void addCapability() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public void removeCapability() throws BundleException
    {
        throw new BundleException("Not implemented yet.");
    }

    public String getProperty(String name)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (!(Constants.FRAMEWORK_VERSION.equals(name) ||
                Constants.FRAMEWORK_VENDOR.equals(name) ||
                Constants.FRAMEWORK_LANGUAGE.equals(name)||
                Constants.FRAMEWORK_OS_NAME.equals(name) ||
                Constants.FRAMEWORK_OS_VERSION.equals(name) ||
                Constants.FRAMEWORK_PROCESSOR.equals(name)))
            {
                ((SecurityManager) sm).checkPermission(
                    new java.util.PropertyPermission(name, "read"));
            }
        }

        return m_felix.getProperty(name);
    }

    public Bundle getBundle()
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_bundle;
    }

    public Filter createFilter(String expr)
        throws InvalidSyntaxException
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return new FilterImpl(expr);
    }

    public Bundle installBundle(String location)
        throws BundleException
    {
        return installBundle(location, null);
    }

    public Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        Bundle result = null;

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            result = m_felix.installBundle(m_bundle, location, is);
            // Do check the bundle again in case that is was installed
            // already.
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(result, AdminPermission.LIFECYCLE));
        }
        else
        {
            result = m_felix.installBundle(m_bundle, location, is);
        }

        return result;
    }

    public Bundle getBundle(long id)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getBundle(this, id);
    }

    public Bundle getBundle(String location)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getBundle(location);
    }

    public Bundle[] getBundles()
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getBundles(this);
    }

    public void addBundleListener(BundleListener l)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation, but
        // internally the event dispatcher double checks whether or not
        // the bundle context is valid before adding the service listener
        // while holding the event queue lock, so it will either succeed
        // or fail.

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (l instanceof SynchronousBundleListener)
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(m_bundle,
                    AdminPermission.LISTENER));
            }
        }

        m_felix.addBundleListener(m_bundle, l);
    }

    public void removeBundleListener(BundleListener l)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (l instanceof SynchronousBundleListener)
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(m_bundle,
                    AdminPermission.LISTENER));
            }
        }

        m_felix.removeBundleListener(m_bundle, l);
    }

    public void addServiceListener(ServiceListener l)
    {
        try
        {
            addServiceListener(l, null);
        }
        catch (InvalidSyntaxException ex)
        {
            // This will not happen since the filter is null.
        }
    }

    public void addServiceListener(ServiceListener l, String s)
        throws InvalidSyntaxException
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation, but
        // internally the event dispatcher double checks whether or not
        // the bundle context is valid before adding the service listener
        // while holding the event queue lock, so it will either succeed
        // or fail.

        m_felix.addServiceListener(m_bundle, l, s);
    }

    public void removeServiceListener(ServiceListener l)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        m_felix.removeServiceListener(m_bundle, l);
    }

    public void addFrameworkListener(FrameworkListener l)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation, but
        // internally the event dispatcher double checks whether or not
        // the bundle context is valid before adding the service listener
        // while holding the event queue lock, so it will either succeed
        // or fail.

        m_felix.addFrameworkListener(m_bundle, l);
    }

    public void removeFrameworkListener(FrameworkListener l)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        m_felix.removeFrameworkListener(m_bundle, l);
    }

    public ServiceRegistration<?> registerService(
        String clazz, Object svcObj, Dictionary<String, ? > dict)
    {
        return registerService(new String[] { clazz }, svcObj, dict);
    }

    public ServiceRegistration<?> registerService(
        String[] clazzes, Object svcObj, Dictionary<String, ? > dict)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a NOT a check-then-act situation,
        // because internally the framework acquires the bundle state
        // lock to ensure state consistency.

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            if (clazzes != null)
            {
                for (int i = 0;i < clazzes.length;i++)
                {
                    ((SecurityManager) sm).checkPermission(
                        new ServicePermission(clazzes[i], ServicePermission.REGISTER));
                }
            }
        }

        return m_felix.registerService(this, clazzes, svcObj, dict);
    }

    public <S> ServiceRegistration<S> registerService(
        Class<S> clazz, S svcObj, Dictionary<String, ? > dict)
    {
        return (ServiceRegistration<S>)
            registerService(new String[] { clazz.getName() }, svcObj, dict);
    }

    public ServiceReference<?> getServiceReference(String clazz)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        try
        {
            ServiceReference[] refs = getServiceReferences(clazz, null);
            return getBestServiceReference(refs);
        }
        catch (InvalidSyntaxException ex)
        {
            m_logger.log(m_bundle, Logger.LOG_ERROR, "BundleContextImpl: " + ex);
        }
        return null;
    }

    public <S> ServiceReference<S> getServiceReference(Class<S> clazz)
    {
        return (ServiceReference<S>) getServiceReference(clazz.getName());
    }

    private ServiceReference getBestServiceReference(ServiceReference[] refs)
    {
        if (refs == null)
        {
            return null;
        }

        if (refs.length == 1)
        {
            return refs[0];
        }

        // Loop through all service references and return
        // the "best" one according to its rank and ID.
        ServiceReference bestRef = refs[0];
        for (int i = 1; i < refs.length; i++)
        {
            if (bestRef.compareTo(refs[i]) < 0)
            {
                bestRef = refs[i];
            }
        }

        return bestRef;
    }

    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter)
        throws InvalidSyntaxException
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getAllowedServiceReferences(m_bundle, clazz, filter, false);

    }

    public ServiceReference<?>[] getServiceReferences(String clazz, String filter)
        throws InvalidSyntaxException
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getAllowedServiceReferences(m_bundle, clazz, filter, true);

    }

    public <S> Collection<ServiceReference<S>> getServiceReferences(
        Class<S> clazz, String filter)
        throws InvalidSyntaxException
    {
        ServiceReference<S>[] refs =
            (ServiceReference<S>[]) getServiceReferences(clazz.getName(), filter);
        return (refs == null)
            ? Collections.EMPTY_LIST
            : (Collection<ServiceReference<S>>) Arrays.asList(refs);
    }

    public <S> S getService(ServiceReference<S> ref)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
           ((SecurityManager) sm).checkPermission(new ServicePermission(ref, ServicePermission.GET));
        }

        return m_felix.getService(m_bundle, ref, false);
    }

    public boolean ungetService(ServiceReference<?> ref)
    {
        checkValidity();

        if (ref == null)
        {
            throw new NullPointerException("Specified service reference cannot be null.");
        }

        // Unget the specified service.
        return m_felix.ungetService(m_bundle, ref, null);
    }

    public File getDataFile(String s)
    {
        checkValidity();

        // CONCURRENCY NOTE: This is a check-then-act situation,
        // but we ignore it since the time window is small and
        // the result is the same as if the calling thread had
        // won the race condition.

        return m_felix.getDataFile(m_bundle, s);
    }

    private void checkValidity()
    {
        if (m_valid)
        {
            switch (m_bundle.getState())
            {
                case Bundle.ACTIVE:
                case Bundle.STARTING:
                case Bundle.STOPPING:
                    return;
            }
        }

        throw new IllegalStateException("Invalid BundleContext.");
    }

    /**
     * @see org.osgi.framework.BundleContext#registerService(java.lang.Class, org.osgi.framework.ServiceFactory, java.util.Dictionary)
     */
    public <S> ServiceRegistration<S> registerService(Class<S> clazz,
            ServiceFactory<S> factory, Dictionary<String, ?> properties)
    {
        return (ServiceRegistration<S>)
                registerService(new String[] { clazz.getName() }, factory, properties);
    }

    /**
     * @see org.osgi.framework.BundleContext#getServiceObjects(org.osgi.framework.ServiceReference)
     */
    public <S> ServiceObjects<S> getServiceObjects(final ServiceReference<S> ref)
    {
    	checkValidity();

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
           ((SecurityManager) sm).checkPermission(new ServicePermission(ref, ServicePermission.GET));
        }

        ServiceRegistrationImpl reg =
                ((ServiceRegistrationImpl.ServiceReferenceImpl) ref).getRegistration();
        if ( reg.isValid() )
        {
        	return new ServiceObjectsImpl(ref);
        }
        return null;
    }

    //
    // ServiceObjects implementation
    //
    class ServiceObjectsImpl<S> implements ServiceObjects<S>
    {
        private final ServiceReference<S> m_ref;

        public ServiceObjectsImpl(final ServiceReference<S> ref)
        {
            this.m_ref = ref;
        }

        public S getService() {
            checkValidity();

            // CONCURRENCY NOTE: This is a check-then-act situation,
            // but we ignore it since the time window is small and
            // the result is the same as if the calling thread had
            // won the race condition.

            final Object sm = System.getSecurityManager();

            if (sm != null)
            {
               ((SecurityManager) sm).checkPermission(new ServicePermission(m_ref, ServicePermission.GET));
            }

            return m_felix.getService(m_bundle, m_ref, true);
        }

        public void ungetService(final S srvObj)
        {
            checkValidity();

            // Unget the specified service.
            if ( !m_felix.ungetService(m_bundle, m_ref, srvObj) )
            {
            	throw new IllegalArgumentException();
            }
        }

        public ServiceReference<S> getServiceReference()
        {
            return m_ref;
        }
    }

}