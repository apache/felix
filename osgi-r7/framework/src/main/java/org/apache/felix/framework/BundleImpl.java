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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.ShrinkableCollection;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.osgi.dto.DTO;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

class BundleImpl implements Bundle, BundleRevisions
{
    // No one should use this field directly, use getFramework() instead.
    private final Felix __m_felix;

    private final BundleArchive m_archive;
    private final List<BundleRevisionImpl> m_revisions = new ArrayList<BundleRevisionImpl>(0);
    private volatile int m_state;
    private boolean m_useDeclaredActivationPolicy;
    private BundleActivator m_activator = null;
    private volatile BundleContext m_context = null;
    private final Map m_cachedHeaders = new HashMap();
    private Map m_uninstalledHeaders = null;
    private long m_cachedHeadersTimestamp;
    private final Bundle m_installingBundle;

    // Indicates whether the bundle is stale, meaning that it has
    // been refreshed and completely removed from the framework.
    private boolean m_stale = false;
    // Used for bundle locking.
    private int m_lockCount = 0;
    private Thread m_lockThread = null;

    /**
     * This constructor is used by the system bundle (i.e., the framework),
     * since it needs a constructor that does not throw an exception.
    **/
    BundleImpl()
    {
        __m_felix = null;
        m_archive = null;
        m_state = Bundle.INSTALLED;
        m_useDeclaredActivationPolicy = false;
        m_stale = false;
        m_activator = null;
        m_context = null;
        m_installingBundle = null;
    }

    BundleImpl(Felix felix, Bundle installingBundle, BundleArchive archive) throws Exception
    {
        __m_felix = felix;
        m_archive = archive;
        m_state = Bundle.INSTALLED;
        m_useDeclaredActivationPolicy = false;
        m_stale = false;
        m_activator = null;
        m_context = null;
        m_installingBundle = installingBundle;

        BundleRevisionImpl revision = createRevision(false);
        addRevision(revision);
    }

    // This method exists because the system bundle extends BundleImpl
    // and cannot pass itself into the BundleImpl constructor. All methods
    // in BundleImpl should use this method to get the framework and should
    // not access the field directly.
    Felix getFramework()
    {
        return __m_felix;
    }

    BundleArchive getArchive()
    {
        return m_archive;
    }

// Only called when the framework is stopping. Don't need to clean up dependencies.
    synchronized void close()
    {
        closeRevisions();
        try
        {
            m_archive.close();
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Unable to close archive revisions.", ex);
        }
    }

// Called when install fails, when stopping framework with uninstalled bundles,
// and when refreshing an uninstalled bundle. Only need to clear up dependencies
// for last case.
    synchronized void closeAndDelete() throws Exception
    {
        if (!m_stale)
        {
            // Mark the bundle as stale, since it is being deleted.
            m_stale = true;
            // Close all revisions.
            closeRevisions();
            // Delete bundle archive, which will close revisions.
            m_archive.closeAndDelete();
        }
    }

// Called from BundleImpl.close(), BundleImpl.closeAndDelete(), and BundleImpl.refresh()
    private void closeRevisions()
    {
        // Remove the bundle's associated revisions from the resolver state
        // and close them.
        for (BundleRevisionImpl br : m_revisions)
        {
            // Remove the revision from the resolver state.
            getFramework().getResolver().removeRevision(br);

            // Close the revision's content.
            br.close();
        }
    }

// Called when refreshing a bundle. Must clean up dependencies beforehand.
    synchronized void refresh() throws Exception
    {
        if (isExtension() && (getFramework().getState() != Bundle.STOPPING))
        {
            getFramework().getLogger().log(this, Logger.LOG_WARNING,
                "Framework restart on extension bundle refresh not implemented.");
        }
        else
        {
            // Get current revision, since we can reuse it.
            BundleRevisionImpl current = adapt(BundleRevisionImpl.class);
            // Close all existing revisions.
            closeRevisions();
            // Clear all revisions.
            m_revisions.clear();

            // Purge all old archive revisions, only keeping the newest one.
            m_archive.purge();

            // Reset the content of the current bundle revision.
            current.resetContent(m_archive.getCurrentRevision().getContent());
            // Re-add the revision to the bundle.
            addRevision(current);

            // Reset the bundle state.
            m_state = Bundle.INSTALLED;
            m_stale = false;

            synchronized (m_cachedHeaders)
            {
                m_cachedHeaders.clear();
                m_cachedHeadersTimestamp = 0;
            }
        }
    }

    synchronized boolean isDeclaredActivationPolicyUsed()
    {
        return m_useDeclaredActivationPolicy;
    }

    synchronized void setDeclaredActivationPolicyUsed(boolean b)
    {
        m_useDeclaredActivationPolicy = b;
    }

    synchronized BundleActivator getActivator()
    {
        return m_activator;
    }

    synchronized void setActivator(BundleActivator activator)
    {
        m_activator = activator;
    }

    @Override
    public BundleContext getBundleContext()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
           ((SecurityManager) sm).checkPermission(
               new AdminPermission(this, AdminPermission.CONTEXT));
        }

        return m_context;
    }

    void setBundleContext(BundleContext context)
    {
        m_context = context;
    }

    @Override
    public long getBundleId()
    {
        try
        {
            return m_archive.getId();
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error getting the identifier from bundle archive.",
                ex);
            return -1;
        }
    }

    @Override
    public URL getEntry(String name)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getFramework().getBundleEntry(this, name);
    }

    @Override
    public Enumeration getEntryPaths(String path)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getFramework().getBundleEntryPaths(this, path);
    }

    @Override
    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getFramework().findBundleEntries(
                this, path, filePattern, recurse);
    }

    @Override
    public Dictionary getHeaders()
    {
        return getHeaders(Locale.getDefault().toString());
    }

    @Override
    public Dictionary getHeaders(String locale)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }

        if (locale == null)
        {
            locale = Locale.getDefault().toString();
        }

        return getFramework().getBundleHeaders(this, locale);
    }

    Map getCurrentLocalizedHeader(String locale)
    {
        Map result = null;

        // Spec says empty local returns raw headers.
        if (locale.length() == 0)
        {
            result = new StringMap(adapt(BundleRevisionImpl.class).getHeaders());
        }

        // If we have no result, try to get it from the cached headers.
        if (result == null)
        {
            synchronized (m_cachedHeaders)
            {
                // If the bundle is uninstalled, then we should always return
                // the uninstalled headers, which are the default locale as per
                // the spec.
                if (m_uninstalledHeaders != null)
                {
                    result = m_uninstalledHeaders;
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
                    if (m_cachedHeaders.containsKey(locale))
                    {
                        result = (Map) m_cachedHeaders.get(locale);
                    }
                }
            }
        }

        // If the requested locale is not cached, then try to create it.
        if (result == null)
        {
            // Get a modifiable copy of the raw headers.
            Map headers = new StringMap(adapt(BundleRevisionImpl.class).getHeaders());
            // Assume for now that this will be the result.
            result = headers;

            // Check to see if we actually need to localize anything
            boolean localize = false;
            for (Iterator it = headers.values().iterator(); !localize && it.hasNext(); )
            {
                if (((String) it.next()).startsWith("%"))
                {
                    localize = true;
                }
            }

            if (!localize)
            {
                // If localization is not needed, just cache the headers and return
                // them as-is. Not sure if this is useful
                updateHeaderCache(locale, headers);
            }
            else
            {
                // Do localization here and return the localized headers
                String basename = (String) headers.get(Constants.BUNDLE_LOCALIZATION);
                if (basename == null)
                {
                    basename = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
                }

                // Create ordered list of revisions to search for localization
                // property resources.
                List<BundleRevision> revisionList = createLocalizationRevisionList(
                    adapt(BundleRevisionImpl.class));

                // Create ordered list of files to load properties from
                List<String> resourceList = createLocalizationResourceList(basename, locale);

                // Create a merged props file with all available props for this locale
                boolean found = false;
                Properties mergedProperties = new Properties();
                for (BundleRevision br : revisionList)
                {
                    for (String res : resourceList)
                    {
                        URL temp = ((BundleRevisionImpl) br).getEntry(res + ".properties");
                        if (temp != null)
                        {
                            found = true;
                            try
                            {
                                mergedProperties.load(
                                    temp.openConnection().getInputStream());
                            }
                            catch (IOException ex)
                            {
                                // File doesn't exist, just continue loop
                            }
                        }
                    }
                }

                // If the specified locale was not found, then the spec says we should
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
                    for (Iterator it = headers.entrySet().iterator(); it.hasNext(); )
                    {
                        Map.Entry entry = (Map.Entry) it.next();
                        String value = (String) entry.getValue();
                        if (value.startsWith("%"))
                        {
                            String newvalue;
                            String key = value.substring(value.indexOf("%") + 1);
                            newvalue = mergedProperties.getProperty(key);
                            if (newvalue==null)
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

    private void updateHeaderCache(String locale, Map localizedHeaders)
    {
        synchronized (m_cachedHeaders)
        {
            if (m_uninstalledHeaders == null)
            {
                m_cachedHeaders.put(locale, localizedHeaders);
                m_cachedHeadersTimestamp = System.currentTimeMillis();
            }
        }
    }

    private static List<BundleRevision> createLocalizationRevisionList(BundleRevision br)
    {
        // If the revision is a fragment, then we actually need
        // to search its host and associated fragments for its
        // localization information. So, check to see if there
        // are any hosts and then use the one with the highest
        // version instead of the fragment itself. If there are
        // no hosts, but the revision is a fragment, then just
        // search the revision itself.
        if (Util.isFragment(br))
        {
            if (br.getWiring() != null)
            {
                List<BundleWire> hostWires = br.getWiring().getRequiredWires(null);
                if ((hostWires != null) && (hostWires.size() > 0))
                {
                    br = hostWires.get(0).getProviderWiring().getRevision();
                    for (int hostIdx = 1; hostIdx < hostWires.size(); hostIdx++)
                    {
                        if (br.getVersion().compareTo(
                            hostWires.get(hostIdx).getProviderWiring().getRevision().getVersion()) < 0)
                        {
                            br = hostWires.get(hostIdx).getProviderWiring().getRevision();
                        }
                    }
                }
            }
        }

        // Create a list of the revision and any attached fragment revisions.
        List<BundleRevision> result = new ArrayList<BundleRevision>();
        result.add(br);
        BundleWiring wiring = br.getWiring();
        if (wiring != null)
        {
            List<BundleRevision> fragments = Util.getFragments(wiring);
            if (fragments != null)
            {
                result.addAll(fragments);
            }
        }
        return result;
    }

    private static List<String> createLocalizationResourceList(String basename, String locale)
    {
        List<String> result = new ArrayList(4);

        StringTokenizer tokens;
        StringBuffer tempLocale = new StringBuffer(basename);

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

    @Override
    public long getLastModified()
    {
        try
        {
            return m_archive.getLastModified();
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error reading last modification time from bundle archive.",
                ex);
            return 0;
        }
    }

    void setLastModified(long l)
    {
        try
        {
            m_archive.setLastModified(l);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error writing last modification time to bundle archive.",
                ex);
        }
    }

    @Override
    public String getLocation()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }
        return _getLocation();
    }

    String _getLocation()
    {
        try
        {
            return m_archive.getLocation();
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error getting location from bundle archive.",
                ex);
            return null;
        }
    }

    /**
     * Returns a URL to a named resource in the bundle.
     *
     * @return a URL to named resource, or null if not found.
    **/
    @Override
    public URL getResource(String name)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(
                    new AdminPermission(this, AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getFramework().getBundleResource(this, name);
    }

    @Override
    public Enumeration getResources(String name) throws IOException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(
                    new AdminPermission(this, AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        // Spec says we should return null when resources not found,
        // even though ClassLoader.getResources() returns empty enumeration.
        Enumeration e = getFramework().getBundleResources(this, name);
        return ((e == null) || !e.hasMoreElements()) ? null : e;
    }

    /**
     * Returns an array of service references corresponding to
     * the bundle's registered services.
     *
     * @return an array of service references or null.
    **/
    @Override
    public ServiceReference[] getRegisteredServices()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = getFramework().getBundleRegisteredServices(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0; i < refs.length; i++)
            {
                try
                {
                    ((SecurityManager) sm).checkPermission(new ServicePermission(
                        refs[i], ServicePermission.GET));

                    result.add(refs[i]);
                }
                catch (Exception ex)
                {
                    // Silently ignore.
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
        else
        {
            return getFramework().getBundleRegisteredServices(this);
        }
    }

    @Override
    public ServiceReference[] getServicesInUse()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = getFramework().getBundleServicesInUse(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0; i < refs.length; i++)
            {
                try
                {
                    ((SecurityManager) sm).checkPermission(
                        new ServicePermission(refs[i], ServicePermission.GET));

                    result.add(refs[i]);
                }
                catch (Exception ex)
                {
                    // Silently ignore.
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }

        return getFramework().getBundleServicesInUse(this);
    }

    @Override
    public int getState()
    {
        return m_state;
    }

    // This method should not be called directly.
    void __setState(int i)
    {
        m_state = i;
    }

    int getPersistentState()
    {
        try
        {
            return m_archive.getPersistentState();
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error reading persistent state from bundle archive.",
                ex);
            return Bundle.INSTALLED;
        }
    }

    void setPersistentStateInactive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.INSTALLED);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(this, Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }

    void setPersistentStateActive()
    {
        try
        {
            m_archive.setPersistentState(Bundle.ACTIVE);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }

    void setPersistentStateStarting()
    {
        try
        {
            m_archive.setPersistentState(Bundle.STARTING);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }
    void setPersistentStateUninstalled()
    {
        try
        {
            m_archive.setPersistentState(Bundle.UNINSTALLED);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error writing persistent state to bundle archive.",
                ex);
        }
    }

    int getStartLevel(int defaultLevel)
    {
        try
        {
            int level = m_archive.getStartLevel();
            if ( level == -1 )
            {
                level = defaultLevel;
            }
            return level;
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error reading start level from bundle archive.",
                ex);
            return defaultLevel;
        }
    }

    void setStartLevel(int i)
    {
        try
        {
            m_archive.setStartLevel(i);
        }
        catch (Exception ex)
        {
            getFramework().getLogger().log(
                this,
                Logger.LOG_ERROR,
                "Error writing start level to bundle archive.",
                ex);
        }
    }

    synchronized boolean isStale()
    {
        return m_stale;
    }

    synchronized boolean isExtension()
    {
        for (BundleRevisionImpl revision : m_revisions)
        {
            if (revision.isExtension())
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSymbolicName()
    {
        return adapt(BundleRevisionImpl.class).getSymbolicName();
    }

    @Override
    public Version getVersion()
    {
        return adapt(BundleRevisionImpl.class).getVersion();
    }

    @Override
    public boolean hasPermission(Object obj)
    {
        return getFramework().bundleHasPermission(this, obj);
    }

    @Override
    public Map getSignerCertificates(int signersType)
    {
        // TODO: SECURITY - This needs to be adapted to our security mechanisms.
        return (Map) getFramework().getSignerMatcher(this, signersType);
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException
    {
        if (isExtension())
        {
            throw new ClassNotFoundException("Extension bundles cannot load classes.");
        }

        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.CLASS));
            }
            catch (Exception ex)
            {
                throw new ClassNotFoundException("No permission.", ex);
            }
        }

        return getFramework().loadBundleClass(this, name);
    }

    @Override
    public void start() throws BundleException
    {
        start(0);
    }

    @Override
    public void start(int options) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        getFramework().startBundle(this, options);
    }

    @Override
    public void update() throws BundleException
    {
        update(null);
    }

    @Override
    public void update(InputStream is) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.LIFECYCLE));
        }

        getFramework().updateBundle(this, is);
    }

    @Override
    public void stop() throws BundleException
    {
        stop(0);
    }

    @Override
    public void stop(int options) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        getFramework().stopBundle(this, ((options & Bundle.STOP_TRANSIENT) == 0));
    }

    @Override
    public void uninstall() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.LIFECYCLE));
        }

        Map headers = getCurrentLocalizedHeader(Locale.getDefault().toString());

        // Uninstall the bundle.
        getFramework().uninstallBundle(this);

        // After a bundle is uninstalled, the spec says getHeaders() should
        // return the localized headers for the default locale at the time of
        // of uninstall. So, let's clear the existing header cache to throw
        // away any other locales and populate it with the headers for the
        // default locale. We only do this if there are multiple cached locales
        // and if the default locale is not cached. If this method is called
        // more than once, then subsequent calls will do nothing here since
        // only the default locale will be left in the header cache.
        synchronized (m_cachedHeaders)
        {
            if (m_uninstalledHeaders == null)
            {
                m_uninstalledHeaders = headers;
                m_cachedHeaders.clear();
            }
        }
    }

    private static final SecurityManagerEx m_smEx = new SecurityManagerEx();
    private static final ClassLoader m_classloader = Felix.class.getClassLoader();

    <A> void checkAdapt(Class<A> type)
    {
        Object sm = System.getSecurityManager();
        if ((sm != null) && (getFramework().getSecurityProvider() != null))
        {
            Class caller = m_smEx.getClassContext()[3];
            if (((Felix.m_secureAction.getClassLoader(caller) != m_classloader) ||
                !caller.getName().startsWith("org.apache.felix.framework.")))
            {
                ((SecurityManager) sm).checkPermission(
                    new AdaptPermission(type.getName(), this, AdaptPermission.ADAPT));
            }
        }
    }

    @Override
    public synchronized <A> A adapt(Class<A> type)
    {
        checkAdapt(type);
        if (type == BundleContext.class)
        {
            return (A) m_context;
        }
        else if (type == BundleStartLevel.class)
        {
            return (A) getFramework().adapt(FrameworkStartLevelImpl.class)
                .createBundleStartLevel(this);
        }
        else if (type == BundleRevision.class)
        {
            if (m_state == Bundle.UNINSTALLED)
            {
                return null;
            }
            return (A) m_revisions.get(0);
        }
        // We need some way to get the current revision even if
        // the associated bundle is uninstalled, so we use the
        // impl revision class for this purpose.
        else if (type == BundleRevisionImpl.class)
        {
            return (A) m_revisions.get(0);
        }
        else if (type == BundleRevisions.class)
        {
            return (A) this;
        }
        else if (type == BundleWiring.class)
        {
            if (m_state == Bundle.UNINSTALLED)
            {
                return null;
            }
            return (A) m_revisions.get(0).getWiring();
        }
        else if ( type == AccessControlContext.class)
        {
            if (m_state == Bundle.UNINSTALLED)
            {
                return null;
            }
            final ProtectionDomain pd = this.getProtectionDomain();
            if (pd == null)
            {
                return null;
            }
            return (A) new AccessControlContext(new ProtectionDomain[] {pd});

        }
        else if (DTO.class.isAssignableFrom(type) ||
                DTO[].class.isAssignableFrom(type))
        {
            return DTOFactory.createDTO(this, type);
        }
        return null;
    }

    @Override
    public File getDataFile(String filename)
    {
        return getFramework().getDataFile(this, filename);
    }

    @Override
    public int compareTo(Bundle t)
    {
        long thisBundleId = this.getBundleId();
        long thatBundleId = t.getBundleId();
        return (thisBundleId < thatBundleId ? -1 : (thisBundleId == thatBundleId ? 0 : 1));
    }

    @Override
    public String toString()
    {
        String sym = getSymbolicName();
        if (sym != null)
        {
            return sym + " [" + getBundleId() +"]";
        }
        return "[" + getBundleId() +"]";
    }

    synchronized boolean isRemovalPending()
    {
        return (m_state == Bundle.UNINSTALLED) || (m_revisions.size() > 1)  || m_stale;
    }

    //
    // Revision management.
    //

    @Override
    public Bundle getBundle()
    {
        return this;
    }

    @Override
    public synchronized List<BundleRevision> getRevisions()
    {
        return new ArrayList<BundleRevision>(m_revisions);
    }

    /**
     * Determines if the specified module is associated with this bundle.
     * @param revision the module to determine if it is associate with this bundle.
     * @return <tt>true</tt> if the specified module is in the array of modules
     *         associated with this bundle, <tt>false</tt> otherwise.
    **/
    synchronized boolean hasRevision(BundleRevision revision)
    {
        return m_revisions.contains(revision);
    }

    synchronized void revise(String location, InputStream is)
        throws Exception
    {
        // This operation will increase the revision count for the bundle.
        m_archive.revise(location, is);
        try
        {
            BundleRevisionImpl revision = createRevision(true);
            addRevision(revision);
        }
        catch (Exception ex)
        {
            m_archive.rollbackRevise();
            throw ex;
        }
    }

    synchronized boolean rollbackRevise() throws Exception
    {
        boolean isExtension = isExtension();
        BundleRevision br = m_revisions.remove(0);
        if (!isExtension)
        {
            // Since revising a bundle adds a revision to the global
            // state, we must remove it from the global state on rollback.
            getFramework().getResolver().removeRevision(br);
        }
        return m_archive.rollbackRevise();
    }

    // This method should be private, but is visible because the
    // system bundle needs to add its revision directly to the bundle,
    // since it doesn't have an archive from which it will be created,
    // which is the normal case.
    synchronized void addRevision(BundleRevisionImpl revision) throws Exception
    {
        m_revisions.add(0, revision);

        try
        {
            getFramework().setBundleProtectionDomain(revision);
        }
        catch (Exception ex)
        {
            m_revisions.remove(0);
            throw ex;
        }

        // TODO: REFACTOR - consider nulling capabilities for extension bundles
        // so we don't need this check anymore.
        if (!isExtension())
        {
            // Now that the revision is added to the bundle, we can update
            // the resolver's state to be aware of any new capabilities.
            getFramework().getResolver().addRevision(revision);
        }
    }

    private BundleRevisionImpl createRevision(boolean isUpdate) throws Exception
    {
        // Get and parse the manifest from the most recent revision and
        // create an associated revision object for it.
        Map headerMap = m_archive.getCurrentRevision().getManifestHeader();

        // Create the bundle revision instance.
        BundleRevisionImpl revision = new BundleRevisionImpl(
            this,
            Long.toString(getBundleId())
                + "." + m_archive.getCurrentRevisionNumber().toString(),
            headerMap,
            m_archive.getCurrentRevision().getContent());

        // For R4 bundles, verify that the bundle symbolic name + version
        // is unique unless this check has been disabled.
        String allowMultiple =
            (String) getFramework().getConfig().get(Constants.FRAMEWORK_BSNVERSION);
        allowMultiple = (allowMultiple == null)
            ? Constants.FRAMEWORK_BSNVERSION_MANAGED
            : allowMultiple;
        if (revision.getManifestVersion().equals("2")
            && !allowMultiple.equals(Constants.FRAMEWORK_BSNVERSION_MULTIPLE))
        {
            Version bundleVersion = revision.getVersion();
            bundleVersion = (bundleVersion == null) ? Version.emptyVersion : bundleVersion;
            String symName = revision.getSymbolicName();

            List<Bundle> collisionCanditates = new ArrayList<Bundle>();
            Bundle[] bundles = getFramework().getBundles();
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                long id = ((BundleImpl) bundles[i]).getBundleId();
                if (id != getBundleId())
                {
                    if (symName.equals(bundles[i].getSymbolicName())
                        && bundleVersion.equals(bundles[i].getVersion()))
                    {
                        collisionCanditates.add(bundles[i]);
                    }
                }
            }
            if (!collisionCanditates.isEmpty() && allowMultiple.equals(Constants.FRAMEWORK_BSNVERSION_MANAGED))
            {
                Set<ServiceReference<CollisionHook>> hooks = getFramework().getHookRegistry().getHooks(CollisionHook.class);
                if (!hooks.isEmpty())
                {
                    Collection<Bundle> shrinkableCollisionCandidates = new ShrinkableCollection<Bundle>(collisionCanditates);
                    for (ServiceReference<CollisionHook> hook : hooks)
                    {
                        CollisionHook ch = getFramework().getService(getFramework(), hook, false);
                        if (ch != null)
                        {
                            int operationType;
                            Bundle target;
                            if (isUpdate)
                            {
                                operationType = CollisionHook.UPDATING;
                                target = this;
                            }
                            else
                            {
                                operationType = CollisionHook.INSTALLING;
                                target = m_installingBundle == null ? this : m_installingBundle;
                            }

                            Felix.m_secureAction.invokeBundleCollisionHook(ch, operationType, target,
                                    shrinkableCollisionCandidates);
                        }
                    }
                }
            }
            if (!collisionCanditates.isEmpty())
            {
                throw new BundleException(
                    "Bundle symbolic name and version are not unique: "
                    + symName + ':' + bundleVersion, BundleException.DUPLICATE_BUNDLE_ERROR);
            }
        }

        return revision;
    }

    synchronized ProtectionDomain getProtectionDomain()
    {
        ProtectionDomain pd = null;

        for (int i = m_revisions.size() - 1; (i >= 0) && (pd == null); i--)
        {
            pd = m_revisions.get(i).getProtectionDomain();
        }

        return pd;
    }

    //
    // Locking related methods.
    //

    synchronized boolean isLockable()
    {
        return (m_lockCount == 0) || (m_lockThread == Thread.currentThread());
    }

    synchronized Thread getLockingThread()
    {
        return m_lockThread;
    }

    synchronized void lock()
    {
        if ((m_lockCount > 0) && (m_lockThread != Thread.currentThread()))
        {
            throw new IllegalStateException("Bundle is locked by another thread.");
        }
        m_lockCount++;
        m_lockThread = Thread.currentThread();
    }

    synchronized void unlock()
    {
        if (m_lockCount == 0)
        {
            throw new IllegalStateException("Bundle is not locked.");
        }
        if ((m_lockCount > 0) && (m_lockThread != Thread.currentThread()))
        {
            throw new IllegalStateException("Bundle is locked by another thread.");
        }
        m_lockCount--;
        if (m_lockCount == 0)
        {
            m_lockThread = null;
        }
    }

    BundleContext _getBundleContext()
    {
        return m_context;
    }
}
