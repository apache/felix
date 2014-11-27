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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;

import org.apache.felix.connect.felix.framework.ServiceRegistry;
import org.apache.felix.connect.felix.framework.util.EventDispatcher;
import org.apache.felix.connect.felix.framework.util.MapToDictionary;
import org.apache.felix.connect.felix.framework.util.StringMap;

class PojoSRBundle implements Bundle, BundleRevisions
{
    private final Revision m_revision;
    private final Map<String, String> m_headers;
    private final Version m_version;
    private final String m_location;
    private final Map<Long, Bundle> m_bundles;
    private final ServiceRegistry m_registry;
    private final String m_activatorClass;
    private final long m_id;
    private final String m_symbolicName;
    private volatile BundleActivator m_activator = null;
    volatile int m_state = Bundle.RESOLVED;
    volatile BundleContext m_context = null;
    private final EventDispatcher m_dispatcher;
    private final ClassLoader m_classLoader;
    private final Map<Class, Object> m_services;
    private final Map m_config;

    public PojoSRBundle(ServiceRegistry registry,
                        EventDispatcher dispatcher,
                        Map<Long, Bundle> bundles,
                        String location,
                        long id,
                        String symbolicName,
                        Version version,
                        Revision revision,
                        ClassLoader classLoader,
                        Map<String, String> headers,
                        Map<Class, Object> services,
                        Map<? extends Object, ? extends Object> config)
    {
        m_revision = revision;
        m_headers = headers;
        m_version = version;
        m_location = location;
        m_registry = registry;
        m_dispatcher = dispatcher;
        m_activatorClass = headers.get(Constants.BUNDLE_ACTIVATOR);
        m_id = id;
        m_symbolicName = symbolicName;
        m_bundles = bundles;
        m_classLoader = classLoader;
        m_services = services;
        m_config = config;
        if (classLoader instanceof BundleAware) {
            ((BundleAware) classLoader).setBundle(this);
        }
        if (services != null) {
            for (Object o : services.values()) {
                if (o instanceof BundleAware) {
                    ((BundleAware) o).setBundle(this);
                }
            }
        }
    }

    @Override
    public int getState()
    {
        return m_state;
    }

    @Override
    public void start(int options) throws BundleException
    {
        // TODO: lifecycle - fix this
        start();
    }

    @Override
    public synchronized void start() throws BundleException
    {
        if (m_state != Bundle.RESOLVED)
        {
            if (m_state == Bundle.ACTIVE)
            {
                return;
            }
            throw new BundleException("Bundle is in wrong state for start");
        }
        try
        {
            m_state = Bundle.STARTING;

            m_context = new PojoSRBundleContext(this, m_registry, m_dispatcher, m_bundles, m_config);
            m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTING, this));
            if (m_activatorClass != null)
            {
                m_activator = (BundleActivator) m_classLoader.loadClass(m_activatorClass).newInstance();
                m_activator.start(m_context);
            }
            m_state = Bundle.ACTIVE;
            m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STARTED, this));
        }
        catch (Throwable ex)
        {
            m_state = Bundle.RESOLVED;
            m_activator = null;
            m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPED, this));
            throw new BundleException("Unable to start bundle", ex);
        }
    }

    @Override
    public void stop(int options) throws BundleException
    {
        // TODO: lifecycle - fix this
        stop();
    }

    @Override
    public synchronized void stop() throws BundleException
    {
        if (m_state != Bundle.ACTIVE)
        {
            if (m_state == Bundle.RESOLVED)
            {
                return;
            }
            throw new BundleException("Bundle is in wrong state for stop");
        }
        try
        {
            m_state = Bundle.STOPPING;
            m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPING,
                    this));
            if (m_activator != null)
            {
                m_activator.stop(m_context);
            }
        }
        catch (Throwable ex)
        {
            throw new BundleException("Error while stopping bundle", ex);
        }
        finally
        {
            m_registry.unregisterServices(this);
            m_dispatcher.removeListeners(m_context);
            m_activator = null;
            m_context = null;
            m_state = Bundle.RESOLVED;
            m_dispatcher.fireBundleEvent(new BundleEvent(BundleEvent.STOPPED,
                    this));
        }
    }

    @Override
    public void update(InputStream input) throws BundleException
    {
        throw new BundleException("pojosr bundles can't be updated");
    }

    @Override
    public void update() throws BundleException
    {
        throw new BundleException("pojosr bundles can't be updated");
    }

    @Override
    public void uninstall() throws BundleException
    {
        throw new BundleException("pojosr bundles can't be uninstalled");
    }

    @Override
    public Dictionary<String, String> getHeaders()
    {
        return getHeaders(Locale.getDefault().toString());
    }

    @Override
    public long getBundleId()
    {
        return m_id;
    }

    @Override
    public String getLocation()
    {
        return m_location;
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices()
    {
        return m_registry.getRegisteredServices(this);
    }

    @Override
    public ServiceReference<?>[] getServicesInUse()
    {
        return m_registry.getServicesInUse(this);
    }

    @Override
    public boolean hasPermission(Object permission)
    {
        // TODO: security - fix this
        return true;
    }

    @Override
    public URL getResource(String name)
    {
        // TODO: module - implement this based on the revision
        URL result = m_classLoader.getResource(name);
        return result;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale)
    {
        return new MapToDictionary<String, String>(getCurrentLocalizedHeader(locale));
    }

    Map<String, String> getCurrentLocalizedHeader(String locale)
    {
        Map<String, String> result = null;

        // Spec says empty local returns raw headers.
        if ((locale == null) || (locale.length() == 0))
        {
            result = new StringMap<String>(m_headers, false);
        }

        // If we have no result, try to get it from the cached headers.
        if (result == null)
        {
            synchronized (m_cachedHeaders)
            {
                // If the bundle is uninstalled, then the cached headers should
                // only contain the localized headers for the default locale at
                // the time of uninstall, so just return that.
                if (getState() == Bundle.UNINSTALLED)
                {
                    result = m_cachedHeaders.values().iterator().next();
                }
                // If the bundle has been updated, clear the cached headers.
                else if (getLastModified() > m_cachedHeadersTimestamp)
                {
                    m_cachedHeaders.clear();
                }
                // Otherwise, returned the cached headers if they exist.
                else
                {
                    // Check if headers for this locale have already been resolved
                    result = m_cachedHeaders.get(locale);
                }
            }
        }

        // If the requested locale is not cached, then try to create it.
        if (result == null)
        {
            // Get a modifiable copy of the raw headers.
            Map<String, String> headers = new StringMap<String>(m_headers, false);
            // Assume for now that this will be the result.
            result = headers;

            // Check to see if we actually need to localize anything
            boolean localize = false;
            for (String s : headers.values())
            {
                if ((s).startsWith("%"))
                {
                    localize = true;
                    break;
                }
            }

            if (!localize)
            {
                // If localization is not needed, just cache the headers and
                // return
                // them as-is. Not sure if this is useful
                updateHeaderCache(locale, headers);
            }
            else
            {
                // Do localization here and return the localized headers
                String basename = headers.get(Constants.BUNDLE_LOCALIZATION);
                if (basename == null)
                {
                    basename = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
                }

                // Create ordered list of files to load properties from
                List<String> resourceList = createLocalizationResourceList(basename, locale);

                // Create a merged props file with all available props for this
                // locale
                boolean found = false;
                Properties mergedProperties = new Properties();
                for (String aResourceList : resourceList)
                {
                    URL temp = m_revision.getEntry(aResourceList + ".properties");
                    if (temp != null)
                    {
                        found = true;
                        try
                        {
                            mergedProperties.load(temp.openConnection().getInputStream());
                        }
                        catch (IOException ex)
                        {
                            // File doesn't exist, just continue loop
                        }
                    }
                }

                // If the specified locale was not found, then the spec says we
                // should
                // return the default localization.
                if (!found && !locale.equals(Locale.getDefault().toString()))
                {
                    result = getCurrentLocalizedHeader(Locale.getDefault().toString());
                }
                // Otherwise, perform the localization based on the discovered
                // properties and cache the result.
                else
                {
                    // Resolve all localized header entries
                    for (Map.Entry<String, String> entry : headers.entrySet())
                    {
                        String value = entry.getValue();
                        if (value.startsWith("%"))
                        {
                            String newvalue;
                            String key = value.substring(value.indexOf("%") + 1);
                            newvalue = mergedProperties.getProperty(key);
                            if (newvalue == null)
                            {
                                newvalue = key;
                            }
                            entry.setValue(newvalue);
                        }
                    }

                    updateHeaderCache(locale, headers);
                }
            }
        }

        return result;
    }

    private void updateHeaderCache(String locale, Map<String, String> localizedHeaders)
    {
        synchronized (m_cachedHeaders)
        {
            m_cachedHeaders.put(locale, localizedHeaders);
            m_cachedHeadersTimestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, Map<String, String>> m_cachedHeaders = new HashMap<String, Map<String, String>>();
    private long m_cachedHeadersTimestamp;

    private static List<String> createLocalizationResourceList(String basename, String locale)
    {
        List<String> result = new ArrayList<String>(4);

        StringTokenizer tokens;
        StringBuilder tempLocale = new StringBuilder(basename);

        result.add(tempLocale.toString());

        if (locale.length() > 0)
        {
            tokens = new StringTokenizer(locale, "_");
            while (tokens.hasMoreTokens())
            {
                tempLocale.append("_").append(tokens.nextToken());
                result.add(tempLocale.toString());
            }
        }
        return result;
    }

    public String getSymbolicName()
    {
        return m_symbolicName;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return m_classLoader.loadClass(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        // TODO: module - implement this based on the revision
        return m_classLoader.getResources(name);
    }

    @Override
    public Enumeration<String> getEntryPaths(String path)
    {
        return new EntryFilterEnumeration<String>(m_revision, false, path, null, false,
                false);
    }

    @Override
    public URL getEntry(String path)
    {
        URL result = m_revision.getEntry(path);
        return result;
    }

    @Override
    public long getLastModified()
    {
        return m_revision.getLastModified();
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse)
    {
        // TODO: module - implement this based on the revision
        return new EntryFilterEnumeration<URL>(m_revision, true, path, filePattern, recurse, true);
    }

    @Override
    public BundleContext getBundleContext()
    {
        return m_context;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType)
    {
        // TODO: security - fix this
        return new HashMap<X509Certificate, List<X509Certificate>>();
    }

    @Override
    public Version getVersion()
    {
        return m_version;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof PojoSRBundle)
        {
            return ((PojoSRBundle) o).m_id == m_id;
        }
        return false;
    }

    @Override
    public int compareTo(Bundle o)
    {
        long thisBundleId = this.getBundleId();
        long thatBundleId = o.getBundleId();
        return (thisBundleId < thatBundleId ? -1 : (thisBundleId == thatBundleId ? 0 : 1));
    }

    @SuppressWarnings("unchecked")
    public <A> A adapt(Class<A> type)
    {
        if (m_services != null && m_services.containsKey(type))
        {
            return (A) m_services.get(type);
        }
        if (type.isInstance(this))
        {
            return (A) this;
        }
        if (type == BundleWiring.class)
        {
            return (A) new BundleWiringImpl(this, m_classLoader);
        }
        if (type == BundleRevision.class)
        {
            return (A) new BundleRevisionImpl(this);
        }
        if (type == BundleStartLevel.class)
        {
            return (A) new BundleStartLevelImpl(this);
        }
        return null;
    }

    public File getDataFile(String filename)
    {
        return m_context.getDataFile(filename);
    }

    public String toString()
    {
        String sym = getSymbolicName();
        if (sym != null)
        {
            return sym + " [" + getBundleId() + "]";
        }
        return "[" + getBundleId() + "]";
    }

    @Override
    public List<BundleRevision> getRevisions()
    {
        return Arrays.asList(adapt(BundleRevision.class));
    }

    @Override
    public Bundle getBundle()
    {
        return this;
    }


    public static class BundleStartLevelImpl implements BundleStartLevel
    {
        private final Bundle bundle;

        public BundleStartLevelImpl(Bundle bundle)
        {
            this.bundle = bundle;
        }

        public int getStartLevel()
        {
            // TODO Implement this?
            return 1;
        }

        public void setStartLevel(int startlevel)
        {
            // TODO Implement this?
        }

        public boolean isPersistentlyStarted()
        {
            return true;
        }

        public boolean isActivationPolicyUsed()
        {
            return false;
        }

        @Override
        public Bundle getBundle()
        {
            return bundle;
        }
    }

    public static class BundleRevisionImpl implements BundleRevision
    {
        private final Bundle bundle;

        public BundleRevisionImpl(Bundle bundle)
        {
            this.bundle = bundle;
        }

        @Override
        public String getSymbolicName()
        {
            return bundle.getSymbolicName();
        }

        @Override
        public Version getVersion()
        {
            return bundle.getVersion();
        }

        @Override
        public List<BundleCapability> getDeclaredCapabilities(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<BundleRequirement> getDeclaredRequirements(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public int getTypes()
        {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null)
            {
                return BundleRevision.TYPE_FRAGMENT;
            }
            return 0;
        }

        @Override
        public BundleWiring getWiring()
        {
            return bundle.adapt(BundleWiring.class);
        }

        @Override
        public List<Capability> getCapabilities(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Requirement> getRequirements(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public Bundle getBundle()
        {
            return bundle;
        }
    }

    public static class BundleWiringImpl implements BundleWiring
    {

        private final Bundle bundle;
        private final ClassLoader classLoader;

        public BundleWiringImpl(Bundle bundle, ClassLoader classLoader)
        {
            this.bundle = bundle;
            this.classLoader = classLoader;
        }

        @Override
        public boolean isInUse()
        {
            return true;
        }

        @Override
        public boolean isCurrent()
        {
            return true;
        }

        @Override
        public BundleRevision getRevision()
        {
            return bundle.adapt(BundleRevision.class);
        }

        @Override
        public List<BundleRequirement> getRequirements(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<BundleWire> getRequiredWires(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<BundleWire> getProvidedWires(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public ClassLoader getClassLoader()
        {
            return classLoader;
        }

        @Override
        public List<BundleCapability> getCapabilities(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Capability> getResourceCapabilities(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Requirement> getResourceRequirements(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Wire> getProvidedResourceWires(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public List<Wire> getRequiredResourceWires(String namespace)
        {
            return Collections.emptyList();
        }

        @Override
        public BundleRevision getResource()
        {
            return getRevision();
        }

        @Override
        public Bundle getBundle()
        {
            return bundle;
        }

        @Override
        public List<URL> findEntries(String path, String filePattern, int options)
        {
            List<URL> result = new ArrayList<URL>();
            for (Enumeration<URL> e = bundle.findEntries(path, filePattern, options == BundleWiring.FINDENTRIES_RECURSE); e.hasMoreElements(); )
            {
                result.add(e.nextElement());
            }
            return result;
        }

        @Override
        public Collection<String> listResources(String path, String filePattern, int options)
        {
            // TODO: this is wrong, we should return the resource names
            Collection<String> result = new ArrayList<String>();
            for (URL u : findEntries(path, filePattern, options))
            {
                result.add(u.toString());
            }
            return result;
        }

    }

}
