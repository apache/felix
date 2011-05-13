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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.ext.SecurityProvider;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

class BundleImpl implements Bundle
{
    // No one should use this field directly, use getFramework() instead.
    private final Felix __m_felix;

    private final BundleArchive m_archive;
    private final List<BundleRevision> m_revisions = new ArrayList<BundleRevision>(0);
    private volatile int m_state;
    private boolean m_useDeclaredActivationPolicy;
    private BundleActivator m_activator = null;
    private volatile BundleContext m_context = null;
    private final Map m_cachedHeaders = new HashMap();
    private long m_cachedHeadersTimestamp;

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
    }

    BundleImpl(Felix felix, BundleArchive archive) throws Exception
    {
        __m_felix = felix;
        m_archive = archive;
        m_state = Bundle.INSTALLED;
        m_useDeclaredActivationPolicy = false;
        m_stale = false;
        m_activator = null;
        m_context = null;

        BundleRevision revision = createRevision();
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

    synchronized void closeAndDelete() throws Exception
    {
        // Mark the bundle as stale, since it is being deleted.
        m_stale = true;
        // Close all revisions.
        closeRevisions();
        // Delete bundle archive, which will close revisions.
        m_archive.closeAndDelete();
    }

    private void closeRevisions()
    {
        // Remove the bundle's associated revisions from the resolver state
        // and close them.
        for (BundleRevision br : m_revisions)
        {
            // Remove the revision from the resolver state.
            getFramework().getResolver().removeRevision(br);

            // Set fragments to null, which will remove the revision from all
            // of its dependent fragment revisions.
            try
            {
                ((BundleRevisionImpl) br).attachFragments(null);
            }
            catch (Exception ex)
            {
                getFramework().getLogger().log(
                    br.getBundle(), Logger.LOG_ERROR, "Error detaching fragments.", ex);
            }
            // Set wires to null, which will remove the revision from all
            // of its dependent revisions.
            ((BundleRevisionImpl) br).setWires(null, null);

            // Close the revision's content.
            ((BundleRevisionImpl) br).close();
        }
    }

    synchronized void refresh() throws Exception
    {
        if (isExtension() && (getFramework().getState() != Bundle.STOPPING))
        {
            getFramework().getLogger().log(this, Logger.LOG_WARNING,
                "Framework restart on extension bundle refresh not implemented.");
        }
        else
        {
            // Dispose of the current revisions.
            closeRevisions();

            // Now we will purge all old revisions, only keeping the newest one.
            m_archive.purge();

            // Lastly, we want to reset our bundle be reinitializing our state
            // and recreating a revision object for the newest revision.
            m_revisions.clear();
            final BundleRevision br = createRevision();
            addRevision(br);
            m_state = Bundle.INSTALLED;
            m_stale = false;
            m_cachedHeaders.clear();
            m_cachedHeadersTimestamp = 0;
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

        return getFramework().findBundleEntries(this, path, filePattern, recurse);
    }

    public Dictionary getHeaders()
    {
        return getHeaders(Locale.getDefault().toString());
    }

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
            result = new StringMap(
                ((BundleRevisionImpl) getCurrentRevision()).getHeaders(), false);
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
                    result = (Map) m_cachedHeaders.values().iterator().next();
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
            Map headers = new StringMap(
                ((BundleRevisionImpl) getCurrentRevision()).getHeaders(), false);
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
                    (BundleRevisionImpl) getCurrentRevision());

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
            m_cachedHeaders.put(locale, localizedHeaders);
            m_cachedHeadersTimestamp = System.currentTimeMillis();
        }
    }

    private static List<BundleRevision> createLocalizationRevisionList(
        BundleRevisionImpl bri)
    {
        // If the revision is a fragment, then we actually need
        // to search its host and associated fragments for its
        // localization information. So, check to see if there
        // are any hosts and then use the one with the highest
        // version instead of the fragment itself. If there are
        // no hosts, but the revision is a fragment, then just
        // search the revision itself.
        if (Util.isFragment(bri))
        {
            List<BundleWire> hostWires = bri.getWires();
            if ((hostWires != null) && (hostWires.size() > 0))
            {
                bri = (BundleRevisionImpl) hostWires.get(0).getProviderWiring().getRevision();
                for (int hostIdx = 1; hostIdx < hostWires.size(); hostIdx++)
                {
                    if (bri.getVersion().compareTo(
                        hostWires.get(hostIdx).getProviderWiring().getRevision().getVersion()) < 0)
                    {
                        bri = (BundleRevisionImpl)
                            hostWires.get(hostIdx).getProviderWiring().getRevision();
                    }
                }
            }
        }

        // Create a list of the revision and any attached fragment revisions.
        List<BundleRevision> result = new ArrayList<BundleRevision>();
        result.add(bri);
        List<BundleRevision> fragments = bri.getFragments();
        if (fragments != null)
        {
            result.addAll(fragments);
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
            return m_archive.getStartLevel();
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
        for (int i = (m_revisions.size() - 1); i > -1; i--)
        {
            if (((BundleRevisionImpl) m_revisions.get(i)).isExtension())
            {
                return true;
            }
        }
        return false;
    }

    public String getSymbolicName()
    {
        return getCurrentRevision().getSymbolicName();
    }

    public Version getVersion()
    {
        return getCurrentRevision().getVersion();
    }

    public boolean hasPermission(Object obj)
    {
        return getFramework().bundleHasPermission(this, obj);
    }

    public Map getSignerCertificates(int signersType)
    {
        // TODO: SECURITY - This needs to be adapted to our security mechanisms.
        return (Map) getFramework().getSignerMatcher(this, signersType);
    }

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

    public void start() throws BundleException
    {
        start(0);
    }

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

    public void update() throws BundleException
    {
        update(null);
    }

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

    public void stop() throws BundleException
    {
        stop(0);
    }

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

    public void uninstall() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.LIFECYCLE));
        }

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
            if ((m_cachedHeaders.size() > 1)
                || !m_cachedHeaders.containsKey(Locale.getDefault().toString()))
            {
                m_cachedHeaders.clear();
                Map map = getCurrentLocalizedHeader(Locale.getDefault().toString());
            }
        }

        // Uninstall the bundle.
        getFramework().uninstallBundle(this);
    }

    public String toString()
    {
        String sym = getCurrentRevision().getSymbolicName();
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

    /**
     * Returns an array of all modules associated with the bundle represented by
     * this <tt>BundleInfo</tt> object. A module in the array corresponds to a
     * revision of the bundle's JAR file and is ordered from oldest to newest.
     * Multiple revisions of a bundle JAR file might exist if a bundle is
     * updated, without refreshing the framework. In this case, exports from
     * the prior revisions of the bundle JAR file are still offered; the
     * current revision will be bound to packages from the prior revision,
     * unless the packages were not offered by the prior revision. There is
     * no limit on the potential number of bundle JAR file revisions.
     * @return array of modules corresponding to the bundle JAR file revisions.
    **/
    synchronized List<BundleRevision> getRevisions()
    {
        return m_revisions;
    }

    /**
     * Determines if the specified module is associated with this bundle.
     * @param revision the module to determine if it is associate with this bundle.
     * @return <tt>true</tt> if the specified module is in the array of modules
     *         associated with this bundle, <tt>false</tt> otherwise.
    **/
    synchronized boolean hasRevision(BundleRevision revision)
    {
        for (int i = 0; i < m_revisions.size(); i++)
        {
            if (m_revisions.get(i) == revision)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the newest module, which corresponds to the last module
     * in the module array.
     * @return the newest module.
    **/
    synchronized BundleRevision getCurrentRevision()
    {
        return m_revisions.get(m_revisions.size() - 1);
    }

    synchronized boolean isUsed()
    {
        boolean unresolved = true;
        for (int i = 0; unresolved && (i < m_revisions.size()); i++)
        {
            if (m_revisions.get(i).getWiring() != null)
            {
                unresolved = false;
            }
        }
        boolean used = false;
        for (int i = 0; !unresolved && !used && (i < m_revisions.size()); i++)
        {
            List<BundleRevision> dependents =
                ((BundleRevisionImpl) m_revisions.get(i)).getDependents();
            for (int j = 0; (dependents != null) && (j < dependents.size()) && !used; j++)
            {
                if (dependents.get(j) != m_revisions.get(i))
                {
                    used = true;
                }
            }
        }
        return used;
    }

    synchronized void revise(String location, InputStream is)
        throws Exception
    {
        // This operation will increase the revision count for the bundle.
        m_archive.revise(location, is);
        try
        {
            BundleRevision revision = createRevision();
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
        BundleRevision br = m_revisions.remove(m_revisions.size() - 1);
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
    synchronized void addRevision(BundleRevision revision) throws Exception
    {
        m_revisions.add(revision);

        // Set protection domain after adding the revision to the bundle,
        // since this requires that the bundle has a revision.
        ((BundleRevisionImpl) revision).setSecurityContext(
            new BundleProtectionDomain(getFramework(), this));

        SecurityProvider sp = getFramework().getSecurityProvider();
        if ((sp != null) && (System.getSecurityManager() != null))
        {
            try
            {
                sp.checkBundle(this);
            }
            catch (Exception ex) 
            {
                m_revisions.remove(m_revisions.size() - 1);
                throw ex;
            }
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

    private BundleRevision createRevision() throws Exception
    {
        // Get and parse the manifest from the most recent revision and
        // create an associated revision object for it.
        Map headerMap = m_archive.getCurrentRevision().getManifestHeader();

        // Create the bundle revision instance.
        BundleRevisionImpl revision = new BundleRevisionImpl(
            getFramework().getLogger(),
            getFramework().getConfig(),
            getFramework().getResolver(),
            this,
            Long.toString(getBundleId())
                + "." + m_archive.getCurrentRevisionNumber().toString(),
            headerMap,
            m_archive.getCurrentRevision().getContent(),
            getFramework().getBundleStreamHandler(),
            getFramework().getBootPackages(),
            getFramework().getBootPackageWildcards());

        // Verify that the bundle symbolic name + version is unique.
        if (revision.getManifestVersion().equals("2"))
        {
            Version bundleVersion = revision.getVersion();
            bundleVersion = (bundleVersion == null) ? Version.emptyVersion : bundleVersion;
            String symName = revision.getSymbolicName();

            Bundle[] bundles = getFramework().getBundles();
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                long id = ((BundleImpl) bundles[i]).getBundleId();
                if (id != getBundleId())
                {
                    String sym = bundles[i].getSymbolicName();
                    Version ver = ((BundleRevisionImpl)
                        ((BundleImpl) bundles[i]).getCurrentRevision()).getVersion();
                    if ((symName != null) && (sym != null) && symName.equals(sym) && bundleVersion.equals(ver))
                    {
                        throw new BundleException(
                            "Bundle symbolic name and version are not unique: "
                            + sym + ':' + ver, BundleException.DUPLICATE_BUNDLE_ERROR);
                    }
                }
            }
        }

        return revision;
    }

    synchronized ProtectionDomain getProtectionDomain()
    {
        ProtectionDomain pd = null;

        for (int i = m_revisions.size() - 1; (i >= 0) && (pd == null); i--)
        {
            pd = (ProtectionDomain)
                ((BundleRevisionImpl) m_revisions.get(i)).getSecurityContext();
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