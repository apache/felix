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
package org.apache.felix.connect.felix.framework;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.connect.felix.framework.util.MapToDictionary;
import org.apache.felix.connect.felix.framework.util.StringMap;
import org.apache.felix.connect.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;

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
        return true;
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
            catch (Throwable ex)
            {
                System.out.println("ServiceRegistrationImpl: Error ungetting service.");
                ex.printStackTrace();
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
        if (svcObj == null)
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
            return true;
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

        public Dictionary<String, Object> getProperties() {
            return new Hashtable<String, Object>(ServiceRegistrationImpl.this.m_propMap);
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