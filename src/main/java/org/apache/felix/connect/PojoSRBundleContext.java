/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;

import org.apache.felix.connect.felix.framework.ServiceRegistry;
import org.apache.felix.connect.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.connect.felix.framework.util.EventDispatcher;
import org.apache.felix.connect.felix.framework.util.ShrinkableCollection;
import org.apache.felix.connect.felix.framework.util.Util;

class PojoSRBundleContext implements BundleContext
{
    private final Bundle m_bundle;
    private final ServiceRegistry m_reg;
    private final EventDispatcher m_dispatcher;
    private final Map<Long, Bundle> m_bundles;
    private final Map<String, Object> m_config;

    public PojoSRBundleContext(Bundle bundle, ServiceRegistry reg,
                               EventDispatcher dispatcher, Map<Long, Bundle> bundles, Map<String, Object> config)
    {
        m_bundle = bundle;
        m_reg = reg;
        m_dispatcher = dispatcher;
        m_bundles = bundles;
        m_config = config;
    }

    public boolean ungetService(ServiceReference reference)
    {
        return m_reg.ungetService(m_bundle, reference);
    }

    public void removeServiceListener(ServiceListener listener)
    {
        m_dispatcher.removeListener(this, ServiceListener.class,
                listener);
    }

    public void removeFrameworkListener(FrameworkListener listener)
    {
        m_dispatcher
                .removeListener(this, FrameworkListener.class, listener);
    }

    public void removeBundleListener(BundleListener listener)
    {
        m_dispatcher.removeListener(this, BundleListener.class, listener);
    }

    public ServiceRegistration registerService(String clazz, Object service,
                                               Dictionary properties)
    {
        return m_reg.registerService(m_bundle, new String[]{clazz}, service,
                properties);
    }

    public ServiceRegistration registerService(String[] clazzes,
                                               Object service, Dictionary properties)
    {
        return m_reg.registerService(m_bundle, clazzes, service, properties);
    }

    public Bundle installBundle(String location) throws BundleException
    {
        throw new BundleException("pojosr can't do that");
    }

    public Bundle installBundle(String location, InputStream input)
            throws BundleException
    {

        throw new BundleException("pojosr can't do that");
    }

    public ServiceReference<?>[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException
    {
        return getServiceReferences(clazz, filter, true);
    }

    public ServiceReference<?> getServiceReference(String clazz)
    {
        try
        {
            return getBestServiceReference(getServiceReferences(clazz, null));
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private ServiceReference<?> getBestServiceReference(ServiceReference<?>[] refs)
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

    public <S> S getService(ServiceReference<S> reference)
    {
        return m_reg.getService(m_bundle, reference);
    }

    public String getProperty(String key)
    {
        Object result = m_config.get(key);

        return result == null ? System.getProperty(key) : result.toString();
    }

    public File getDataFile(String filename)
    {
        File root = new File("bundle" + m_bundle.getBundleId());
        String storage = getProperty("org.osgi.framework.storage");
        if (storage != null)
        {
            root = new File(new File(storage), root.getName());
        }
        root.mkdirs();
        return filename.trim().length() > 0 ? new File(root, filename) : root;
    }

    public Bundle[] getBundles()
    {
        Bundle[] result = m_bundles.values().toArray(
                new Bundle[m_bundles.size()]);
        Arrays.sort(result, new Comparator<Bundle>()
        {

            public int compare(Bundle o1, Bundle o2)
            {
                return (int) (o1.getBundleId() - o2.getBundleId());
            }
        });
        return result;
    }

    public Bundle getBundle(long id)
    {
        return m_bundles.get(id);
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public ServiceReference[] getAllServiceReferences(String clazz,
                                                      String filter) throws InvalidSyntaxException
    {
        return getServiceReferences(clazz, filter, false);
    }

    /**
     * Retrieves an array of {@link ServiceReference} objects based on calling bundle,
     * service class name, and filter expression.  Optionally checks for isAssignable to
     * make sure that the service can be cast to the
     * @param className Service Classname or <code>null</code> for all
     * @param expr Filter Criteria or <code>null</code>
     * @return Array of ServiceReference objects that meet the criteria
     * @throws InvalidSyntaxException
     */
    ServiceReference[] getServiceReferences(
            final String className,
            final String expr, final boolean checkAssignable)
            throws InvalidSyntaxException
    {
        // Define filter if expression is not null.
        SimpleFilter filter = null;
        if (expr != null)
        {
            try
            {
                filter = SimpleFilter.parse(expr);
            }
            catch (Exception ex)
            {
                throw new InvalidSyntaxException(ex.getMessage(), expr);
            }
        }

        // Ask the service registry for all matching service references.
        final Collection<ServiceReference<?>> refList = m_reg.getServiceReferences(className, filter);

        // Filter on assignable references
        if (checkAssignable)
        {
            for (Iterator<ServiceReference<?>> it = refList.iterator(); it.hasNext();)
            {
                // Get the current service reference.
                ServiceReference ref = it.next();
                // Now check for castability.
                if (!Util.isServiceAssignable(m_bundle, ref))
                {
                    it.remove();
                }
            }
        }

        // activate findhooks
        Set<ServiceReference<FindHook>> findHooks = m_reg.getHooks(org.osgi.framework.hooks.service.FindHook.class);
        for (ServiceReference<org.osgi.framework.hooks.service.FindHook> sr : findHooks)
        {
            org.osgi.framework.hooks.service.FindHook fh = m_reg.getService(getBundle(0), sr);
            if (fh != null)
            {
                try
                {
                    fh.find(this,
                            className,
                            expr,
                            !checkAssignable,
                            new ShrinkableCollection<ServiceReference<?>>(refList));
                }
                catch (Throwable th)
                {
                    System.err.println("Problem invoking service registry hook");
                    th.printStackTrace();
                }
                finally
                {
                    m_reg.ungetService(getBundle(0), sr);
                }
            }
        }

        if (refList.size() > 0)
        {
            return refList.toArray(new ServiceReference[refList.size()]);
        }

        return null;
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException
    {
        return FrameworkUtil.createFilter(filter);
    }

    public void addServiceListener(ServiceListener listener)
    {
        try
        {
            addServiceListener(listener, null);
        }
        catch (InvalidSyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void addServiceListener(final ServiceListener listener, String filter)
            throws InvalidSyntaxException
    {
        m_dispatcher.addListener(this, ServiceListener.class, listener,
                filter == null ? null : FrameworkUtil.createFilter(filter));
    }

    public void addFrameworkListener(FrameworkListener listener)
    {
        m_dispatcher.addListener(this, FrameworkListener.class, listener,
                null);
    }

    public void addBundleListener(BundleListener listener)
    {
        m_dispatcher
                .addListener(this, BundleListener.class, listener, null);
    }

    @SuppressWarnings("unchecked")
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties)
    {
        return (ServiceRegistration<S>) registerService(clazz.getName(), service, properties);
    }

    @SuppressWarnings("unchecked")
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz)
    {
        return (ServiceReference<S>) getServiceReference(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
            throws InvalidSyntaxException
    {
        ServiceReference<S>[] refs = (ServiceReference<S>[]) getServiceReferences(clazz.getName(), filter);
        if (refs == null)
        {
            return Collections.emptyList();
        }
        return Arrays.asList(refs);
    }

    public Bundle getBundle(String location)
    {
        for (Bundle bundle : m_bundles.values())
        {
            if (location.equals(bundle.getLocation()))
            {
                return bundle;
            }
        }
        return null;
    }
}
