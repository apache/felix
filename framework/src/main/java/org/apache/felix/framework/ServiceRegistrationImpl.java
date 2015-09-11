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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.framework.util.MapToDictionary;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

class ServiceRegistrationImpl implements ServiceRegistration
{
    // Service registry.
    private final ServiceRegistry m_registry;
    // Bundle providing the service.
    private final Bundle m_bundle;
    // Interfaces associated with the service object.
    private final String[] m_classes;
    // Service Id associated with the service object.
    private final Long m_serviceId;
    // Service object.
    private volatile Object m_svcObj;
    // Service factory interface.
    private volatile ServiceFactory m_factory;
    // Associated property dictionary.
    private volatile Map<String, Object> m_propMap = new StringMap();
    // Re-usable service reference.
    private final ServiceReferenceImpl m_ref;
    // Flag indicating that we are unregistering.
    private volatile boolean m_isUnregistering = false;
    // This threadlocal is used to detect cycles.
    private final ThreadLocal<Boolean> m_threadLoopDetection = new ThreadLocal<Boolean>();

    private final Object syncObject = new Object();

    public ServiceRegistrationImpl(
        ServiceRegistry registry, Bundle bundle,
        String[] classes, Long serviceId,
        Object svcObj, Dictionary dict)
    {
        m_registry = registry;
        m_bundle = bundle;
        m_classes = classes;
        m_serviceId = serviceId;
        m_svcObj = svcObj;
        m_factory = (m_svcObj instanceof ServiceFactory)
            ? (ServiceFactory) m_svcObj : null;

        initializeProperties(dict);

        // This reference is the "standard" reference for this
        // service and will always be returned by getReference().
        m_ref = new ServiceReferenceImpl();
    }

    protected boolean isValid()
    {
        return (m_svcObj != null);
    }

    protected synchronized void invalidate()
    {
        m_svcObj = null;
    }

    public synchronized ServiceReference getReference()
    {
        // Make sure registration is valid.
        if (!isValid())
        {
            throw new IllegalStateException(
                "The service registration is no longer valid.");
        }
        return m_ref;
    }

    public void setProperties(Dictionary dict)
    {
        Map oldProps;
        synchronized (this)
        {
            // Make sure registration is valid.
            if (!isValid())
            {
                throw new IllegalStateException(
                    "The service registration is no longer valid.");
            }
            // Remember old properties.
            oldProps = m_propMap;
            // Set the properties.
            initializeProperties(dict);
        }
        // Tell registry about it.
        m_registry.servicePropertiesModified(this, new MapToDictionary(oldProps));
    }

    public void unregister()
    {
        synchronized (this)
        {
            if (!isValid() || m_isUnregistering)
            {
                throw new IllegalStateException("Service already unregistered.");
            }
            m_isUnregistering = true;
        }
        m_registry.unregisterService(m_bundle, this);
        synchronized (this)
        {
            m_svcObj = null;
            m_factory = null;
        }
    }

    //
    // Utility methods.
    //

    /**
     * This method determines if the class loader of the service object
     * has access to the specified class.
     * @param clazz the class to test for reachability.
     * @return <tt>true</tt> if the specified class is reachable from the
     *         service object's class loader, <tt>false</tt> otherwise.
    **/
    private boolean isClassAccessible(Class clazz)
    {
        // We need to see if the class loader of the service object has
        // access to the specified class; however, we may not have a service
        // object. If we only have service factory, then we will assume two
        // different scenarios:
        // 1. The service factory is provided by the bundle providing the
        //    service.
        // 2. The service factory is NOT provided by the bundle providing
        //    the service.
        // For case 1, we will use the class loader of the service factory
        // to find the class. For case 2, we will assume we have an extender
        // at work here and always return true, since we have no real way of
        // knowing the wiring of the provider unless we actually get the
        // service object, which defeats the lazy aspect of service factories.

        // Case 2.
        if ((m_factory != null)
            && (Felix.m_secureAction.getClassLoader(m_factory.getClass()) instanceof BundleReference)
            && !((BundleReference) Felix.m_secureAction.getClassLoader(m_factory.getClass())).getBundle().equals(m_bundle))
        {
            try
            {
                Class providedClazz = m_bundle.loadClass(clazz.getName());
                if (providedClazz != null)
                {
                    return providedClazz == clazz;
                }
            }
            catch (ClassNotFoundException ex)
            {
                // Ignore and try interface class loaders.
            }
            return true;
        }

        // Case 1.
        Class sourceClass = (m_factory != null) ? m_factory.getClass() : m_svcObj.getClass();
        return Util.loadClassUsingClass(sourceClass, clazz.getName(), Felix.m_secureAction) == clazz;
    }

    Object getProperty(String key)
    {
        return m_propMap.get(key);
    }

    private String[] getPropertyKeys()
    {
        Set s = m_propMap.keySet();
        return (String[]) s.toArray(new String[s.size()]);
    }

    private Bundle[] getUsingBundles()
    {
        return m_registry.getUsingBundles(m_ref);
    }

    /**
     * This method provides direct access to the associated service object;
     * it generally should not be used by anyone other than the service registry
     * itself.
     * @return The service object associated with the registration.
    **/
    Object getService()
    {
        return m_svcObj;
    }

    Object getService(Bundle acqBundle)
    {
        // If the service object is a service factory, then
        // let it create the service object.
        if (m_factory != null)
        {
            Object svcObj = null;
            try
            {
                if (System.getSecurityManager() != null)
                {
                    svcObj = AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(acqBundle, null));
                }
                else
                {
                    svcObj = getFactoryUnchecked(acqBundle);
                }
            }
            catch (PrivilegedActionException ex)
            {
                if (ex.getException() instanceof ServiceException)
                {
                    throw (ServiceException) ex.getException();
                }
                else
                {
                    throw new ServiceException(
                        "Service factory exception: " + ex.getException().getMessage(),
                        ServiceException.FACTORY_EXCEPTION, ex.getException());
                }
            }
            return svcObj;
        }
        else
        {
            return m_svcObj;
        }
    }

    void ungetService(Bundle relBundle, Object svcObj)
    {
        // If the service object is a service factory, then
        // let it release the service object.
        if (m_factory != null)
        {
            try
            {
                if (System.getSecurityManager() != null)
                {
                    AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(relBundle, svcObj));
                }
                else
                {
                    ungetFactoryUnchecked(relBundle, svcObj);
                }
            }
            catch (Exception ex)
            {
                m_registry.getLogger().log(
                    m_bundle,
                    Logger.LOG_ERROR,
                    "ServiceRegistrationImpl: Error ungetting service.",
                    ex);
            }
        }
    }

    private void initializeProperties(Dictionary<String, Object> dict)
    {
        // Create a case-insensitive map for the properties.
        Map<String, Object> props = new StringMap();

        if (dict != null)
        {
            // Make sure there are no duplicate keys.
            Enumeration<String> keys = dict.keys();
            while (keys.hasMoreElements())
            {
                String key = keys.nextElement();
                if (props.get(key) == null)
                {
                    props.put(key, dict.get(key));
                }
                else
                {
                    throw new IllegalArgumentException("Duplicate service property: " + key);
                }
            }
        }

        // Add the framework assigned properties.
        props.put(Constants.OBJECTCLASS, m_classes);
        props.put(Constants.SERVICE_ID, m_serviceId);
        props.put(Constants.SERVICE_BUNDLEID, m_bundle.getBundleId());
        if ( m_factory != null )
        {
            props.put(Constants.SERVICE_SCOPE,
                      (m_factory instanceof PrototypeServiceFactory
                       ? Constants.SCOPE_PROTOTYPE : Constants.SCOPE_BUNDLE));
        }
        else
        {
            props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
        }

        // Update the service property map.
        m_propMap = props;
    }

    private Object getFactoryUnchecked(Bundle bundle)
    {
        Object svcObj = null;
        try
        {
            svcObj = m_factory.getService(bundle, this);
        }
        catch (Throwable th)
        {
            throw new ServiceException(
                "Service factory exception: " + th.getMessage(),
                ServiceException.FACTORY_EXCEPTION, th);
        }
        if (svcObj != null)
        {
            for (int i = 0; i < m_classes.length; i++)
            {
                Class clazz = Util.loadClassUsingClass(
                    svcObj.getClass(), m_classes[i], Felix.m_secureAction);
                if ((clazz == null) || !clazz.isAssignableFrom(svcObj.getClass()))
                {
                    if (clazz == null)
                    {
                        throw new ServiceException(
                            "Service cannot be cast due to missing class: " + m_classes[i],
                            ServiceException.FACTORY_ERROR);
                    }
                    else
                    {
                        throw new ServiceException(
                            "Service cannot be cast: " + m_classes[i],
                            ServiceException.FACTORY_ERROR);
                    }
                }
            }
        }
        else
        {
            throw new ServiceException(
                "Service factory returned null. (" + m_factory + ")", ServiceException.FACTORY_ERROR);
        }
        return svcObj;
    }

    private void ungetFactoryUnchecked(Bundle bundle, Object svcObj)
    {
        m_factory.ungetService(bundle, this, svcObj);
    }

    /**
     * This simple class is used to ensure that when a service factory
     * is called, that no other classes on the call stack interferes
     * with the permissions of the factory itself.
    **/
    private class ServiceFactoryPrivileged implements PrivilegedExceptionAction
    {
        private Bundle m_bundle = null;
        private Object m_svcObj = null;

        public ServiceFactoryPrivileged(Bundle bundle, Object svcObj)
        {
            m_bundle = bundle;
            m_svcObj = svcObj;
        }

        public Object run() throws Exception
        {
            if (m_svcObj == null)
            {
                return getFactoryUnchecked(m_bundle);
            }
            else
            {
                ungetFactoryUnchecked(m_bundle, m_svcObj);
            }
            return null;
        }
    }

    //
    // ServiceReference implementation
    //

    class ServiceReferenceImpl extends BundleCapabilityImpl implements ServiceReference
    {
        private final ServiceReferenceMap m_map;

        private ServiceReferenceImpl()
        {
            super(null, null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
            m_map = new ServiceReferenceMap();
        }

        ServiceRegistrationImpl getRegistration()
        {
            return ServiceRegistrationImpl.this;
        }

        //
        // Capability methods.
        //

        @Override
        public BundleRevision getRevision()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNamespace()
        {
            return "service-reference";
        }

        @Override
        public Map<String, String> getDirectives()
        {
            return Collections.EMPTY_MAP;
        }

        @Override
        public Map<String, Object> getAttributes()
        {
            return m_map;
        }

        @Override
        public List<String> getUses()
        {
            return Collections.EMPTY_LIST;
        }

        //
        // ServiceReference methods.
        //

        public Object getProperty(String s)
        {
            return ServiceRegistrationImpl.this.getProperty(s);
        }

        public String[] getPropertyKeys()
        {
            return ServiceRegistrationImpl.this.getPropertyKeys();
        }

        public Bundle getBundle()
        {
            // The spec says that this should return null if
            // the service is unregistered.
            return (isValid()) ? m_bundle : null;
        }

        public Bundle[] getUsingBundles()
        {
            return ServiceRegistrationImpl.this.getUsingBundles();
        }

        @Override
        public String toString()
        {
            String[] ocs = (String[]) getProperty("objectClass");
            String oc = "[";
            for(int i = 0; i < ocs.length; i++)
            {
                oc = oc + ocs[i];
                if (i < ocs.length - 1)
                    oc = oc + ", ";
            }
            oc = oc + "]";
            return oc;
        }

        public boolean isAssignableTo(Bundle requester, String className)
        {
            // Always return true if the requester is the same as the provider.
            if (requester == m_bundle)
            {
                return true;
            }

            // Boolean flag.
            boolean allow = true;
            // Get the package.
            String pkgName =
                Util.getClassPackage(className);
            // Get package wiring from service requester.
            BundleRevision requesterRevision = requester.adapt(BundleRevision.class);
            BundleWire requesterWire = Util.getWire(requesterRevision, pkgName);
            BundleCapability requesterCap = Util.getPackageCapability(requesterRevision, pkgName);
            // Get package wiring from service provider.
            BundleRevision providerRevision = m_bundle.adapt(BundleRevision.class);
            BundleWire providerWire = Util.getWire(providerRevision, pkgName);
            BundleCapability providerCap = Util.getPackageCapability(providerRevision, pkgName);

            // There are four situations that may occur here:
            //   1. Neither the requester, nor provider have wires for the package.
            //   2. The requester does not have a wire for the package.
            //   3. The provider does not have a wire for the package.
            //   4. Both the requester and provider have a wire for the package.
            // For case 1, if the requester does not have access to the class at
            // all, we assume it is using reflection and do not filter. If the
            // requester does have access to the class, then we make sure it is
            // the same class as the service. For case 2, we do not filter if the
            // requester is the exporter of the package to which the provider of
            // the service is wired. Otherwise, as in case 1, if the requester
            // does not have access to the class at all, we do not filter, but if
            // it does have access we check if it is the same class accessible to
            // the providing revision. For case 3, the provider will not have a wire
            // if it is exporting the package, so we determine if the requester
            // is wired to it or somehow using the same class. For case 4, we
            // simply compare the exporting revisions from the package wiring to
            // determine if we need to filter the service reference.

            // Case 1: Both requester and provider have no wire.
            if ((requesterWire == null) && (providerWire == null))
            {
                // If requester has no access then true, otherwise service
                // registration must have same class as requester.
                try
                {
                    Class requestClass =
                        ((BundleWiringImpl) requesterRevision.getWiring())
                            .getClassByDelegation(className);
                    allow = getRegistration().isClassAccessible(requestClass);
                }
                catch (Exception ex)
                {
                    // Requester has no access to the class, so allow it, since
                    // we assume the requester is using reflection.
                    allow = true;
                }
            }
            // Case 2: Requester has no wire, but provider does.
            else if ((requesterWire == null) && (providerWire != null))
            {
                // If the requester exports the package, then the provider must
                // be wired to it.
                if (requesterCap != null)
                {
                    allow = providerWire.getProviderWiring().getRevision().equals(requesterRevision);
                }
                // Otherwise, check if the requester has access to the class and,
                // if so, if it is the same class as the provider.
                else
                {
                    try
                    {
                        // Try to load class from requester.
                        Class requestClass =((BundleWiringImpl)
                            requesterRevision.getWiring()).getClassByDelegation(className);
                        try
                        {
                            // If requester has access to the class, verify it is the
                            // same class as the provider.
                            allow = (((BundleWiringImpl)
                                providerRevision.getWiring())
                                    .getClassByDelegation(className) == requestClass);
                        }
                        catch (Exception ex)
                        {
                            allow = false;
                        }
                    }
                    catch (Exception ex)
                    {
                        // Requester has no access to the class, so allow it, since
                        // we assume the requester is using reflection.
                        allow = true;
                    }
                }
            }
            // Case 3: Requester has a wire, but provider does not.
            else if ((requesterWire != null) && (providerWire == null))
            {
                // If the provider exports the package, then the requester must
                // be wired to it.
                if (providerCap != null)
                {
                    allow = requesterWire.getProviderWiring().getRevision().equals(providerRevision);
                }
                // If the provider is not the exporter of the requester's package,
                // then try to use the service registration to see if the requester's
                // class is accessible.
                else
                {
                    try
                    {
                        // Load the class from the requesting bundle.
                        Class requestClass = ((BundleWiringImpl)
                            requesterRevision.getWiring())
                                .getClassByDelegation(className);
                        // Get the service registration and ask it to check
                        // if the service object is assignable to the requesting
                        // bundle's class.
                        allow = getRegistration().isClassAccessible(requestClass);
                    }
                    catch (Exception ex)
                    {
                        // Filter to be safe.
                        allow = false;
                    }
                }
            }
            // Case 4: Both requester and provider have a wire.
            else
            {
                // Include service reference if the wires have the
                // same source revision.
                allow = providerWire.getProviderWiring().getRevision()
                    .equals(requesterWire.getProviderWiring().getRevision());
            }

            return allow;
        }

        public int compareTo(Object reference)
        {
            ServiceReference other = (ServiceReference) reference;

            Long id = (Long) getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId))
            {
                return 0; // same service
            }

            Object rankObj = getProperty(Constants.SERVICE_RANKING);
            Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = (rankObj instanceof Integer)
                ? (Integer) rankObj : new Integer(0);
            Integer otherRank = (otherRankObj instanceof Integer)
                ? (Integer) otherRankObj : new Integer(0);

            // Sort by rank in ascending order.
            if (rank.compareTo(otherRank) < 0)
            {
                return -1; // lower rank
            }
            else if (rank.compareTo(otherRank) > 0)
            {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }
    }

    private class ServiceReferenceMap implements Map
    {
        public int size()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isEmpty()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsKey(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsValue(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object get(Object o)
        {
            return ServiceRegistrationImpl.this.getProperty((String) o);
        }

        public Object put(Object k, Object v)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object remove(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void putAll(Map map)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Set<Object> keySet()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Collection<Object> values()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Set<Entry<Object, Object>> entrySet()
        {
            return Collections.EMPTY_SET;
        }
    }

    boolean currentThreadMarked()
    {
        return m_threadLoopDetection.get() != null;
    }

    void markCurrentThread()
    {
        m_threadLoopDetection.set(Boolean.TRUE);
    }

    void unmarkCurrentThread()
    {
        m_threadLoopDetection.set(null);
    }
}