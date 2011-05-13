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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.felix.framework.Felix.StatefulResolver;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.resolver.Content;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.ResolverWire;
import org.apache.felix.framework.resolver.ResourceNotFoundException;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.apache.felix.framework.wiring.BundleWireImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleRevisionImpl implements BundleRevision
{
    public final static int EAGER_ACTIVATION = 0;
    public final static int LAZY_ACTIVATION = 1;

    private final Logger m_logger;
    private final Map m_configMap;
    private final StatefulResolver m_resolver;
    private final String m_id;
    private final Content m_content;
    private final Map m_headerMap;
    private final URLStreamHandler m_streamHandler;

    private final String m_manifestVersion;
    private final boolean m_isExtension;
    private final String m_symbolicName;
    private final Version m_version;

    private final List<BundleCapability> m_declaredCaps;
    private final List<BundleRequirement> m_declaredReqs;
    private final List<BundleRequirement> m_declaredDynamicReqs;
    private final List<R4Library> m_declaredNativeLibs;
    private final int m_declaredActivationPolicy;
    private final List<String> m_activationIncludes;
    private final List<String> m_activationExcludes;

    private final Bundle m_bundle;

    private List<BundleRevision> m_dependentImporters = new ArrayList<BundleRevision>(0);
    private List<BundleRevision> m_dependentRequirers = new ArrayList<BundleRevision>(0);

    private Content[] m_contentPath;
    private boolean m_isActivationTriggered = false;
    private ProtectionDomain m_protectionDomain = null;
    private final static SecureAction m_secureAction = new SecureAction();

    // Bundle wiring when resolved.
    private volatile BundleWiringImpl m_wiring = null;

    // Boot delegation packages.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    /**
     * This constructor is used by the extension manager, since it needs
     * a constructor that does not throw an exception.
     * @param logger
     * @param bundle
     * @param id
     * @param bootPkgs
     * @param bootPkgWildcards
     * @throws org.osgi.framework.BundleException
     */
    public BundleRevisionImpl(
        Logger logger, Map configMap, Bundle bundle, String id,
        String[] bootPkgs, boolean[] bootPkgWildcards)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = null;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = null;
        m_content = null;
        m_streamHandler = null;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;
        m_manifestVersion = null;
        m_symbolicName = null;
        m_isExtension = false;
        m_version = null;
        m_declaredCaps = Collections.EMPTY_LIST;
        m_declaredReqs = Collections.EMPTY_LIST;
        m_declaredDynamicReqs = Collections.EMPTY_LIST;
        m_declaredNativeLibs = null;
        m_declaredActivationPolicy = EAGER_ACTIVATION;
        m_activationExcludes = null;
        m_activationIncludes = null;
    }

    BundleRevisionImpl(
        Logger logger, Map configMap, StatefulResolver resolver,
        Bundle bundle, String id, Map headerMap, Content content,
        URLStreamHandler streamHandler, String[] bootPkgs,
        boolean[] bootPkgWildcards)
        throws BundleException
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;
        m_bundle = bundle;
        m_id = id;
        m_headerMap = headerMap;
        m_content = content;
        m_streamHandler = streamHandler;
        m_bootPkgs = bootPkgs;
        m_bootPkgWildcards = bootPkgWildcards;

        ManifestParser mp = new ManifestParser(m_logger, m_configMap, this, m_headerMap);

        // Record some of the parsed metadata. Note, if this is an extension
        // bundle it's exports are removed, since they will be added to the
        // system bundle directly later on.
        m_manifestVersion = mp.getManifestVersion();
        m_version = mp.getBundleVersion();
        m_declaredCaps = mp.isExtension() ? null : mp.getCapabilities();
        m_declaredReqs = mp.getRequirements();
        m_declaredDynamicReqs = mp.getDynamicRequirements();
        m_declaredNativeLibs = mp.getLibraries();
        m_declaredActivationPolicy = mp.getActivationPolicy();
        m_activationExcludes = (mp.getActivationExcludeDirective() == null)
            ? null
            : ManifestParser.parseDelimitedString(mp.getActivationExcludeDirective(), ",");
        m_activationIncludes = (mp.getActivationIncludeDirective() == null)
            ? null
            : ManifestParser.parseDelimitedString(mp.getActivationIncludeDirective(), ",");
        m_symbolicName = mp.getSymbolicName();
        m_isExtension = mp.isExtension();
    }

    int getDeclaredActivationPolicy()
    {
        return m_declaredActivationPolicy;
    }

    List<String> getActivationExcludes()
    {
        return m_activationExcludes;
    }

    List<String> getActivationIncludes()
    {
        return m_activationIncludes;
    }

    URLStreamHandler getURLStreamHandler()
    {
        return m_streamHandler;
    }

    // TODO: OSGi R4.3 - Figure out how to handle this. Here we provide access
    //       needed for BundleWiringImpl, but for implicit boot delegation property
    //       we store it in BundleWiringImpl.
    String[] getBootDelegationPackages()
    {
        return m_bootPkgs;
    }

    // TODO: OSGi R4.3 - Figure out how to handle this. Here we provide access
    //       needed for BundleWiringImpl, but for implicit boot delegation property
    //       we store it in BundleWiringImpl.
    boolean[] getBootDelegationPackageWildcards()
    {
        return m_bootPkgWildcards;
    }

    //
    // BundleRevision methods.
    //

    public String getSymbolicName()
    {
        return m_symbolicName;
    }

    public Version getVersion()
    {
        return m_version;
    }

    public List<BundleCapability> getDeclaredCapabilities(String namespace)
    {
        List<BundleCapability> result = m_declaredCaps;
        if (namespace != null)
        {
            result = new ArrayList<BundleCapability>();
            for (BundleCapability cap : m_declaredCaps)
            {
                if (cap.getNamespace().equals(namespace))
                {
                    result.add(cap);
                }
            }
        }
        return result;
    }

    public List<BundleRequirement> getDeclaredRequirements(String namespace)
    {
        List<BundleRequirement> result = m_declaredReqs;
        if (namespace != null)
        {
            result = new ArrayList<BundleRequirement>();
            for (BundleRequirement req : m_declaredReqs)
            {
                if (req.getNamespace().equals(namespace))
                {
                    result.add(req);
                }
            }
        }
        return result;
    }

    public int getTypes()
    {
        if (getHeaders().containsKey(Constants.FRAGMENT_HOST))
        {
            return BundleRevision.TYPE_FRAGMENT;
        }
        return 0;
    }

    public BundleWiring getWiring()
    {
        return m_wiring;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    //
    // Implementating details.
    //

    public Map getHeaders()
    {
        return m_headerMap;
    }

    public boolean isExtension()
    {
        return m_isExtension;
    }

    public String getManifestVersion()
    {
        return m_manifestVersion;
    }

    public List<BundleRequirement> getDeclaredDynamicRequirements()
    {
        return m_declaredDynamicReqs;
    }

    public List<R4Library> getDeclaredNativeLibraries()
    {
        return m_declaredNativeLibs;
    }

    synchronized boolean isActivationTriggered()
    {
        return m_isActivationTriggered;
    }

    boolean isActivationTrigger(String pkgName)
    {
        if ((m_activationIncludes == null) && (m_activationExcludes == null))
        {
            return true;
        }

        // If there are no include filters then all classes are included
        // by default, otherwise try to find one match.
        boolean included = (m_activationIncludes == null);
        for (int i = 0;
            (!included) && (m_activationIncludes != null) && (i < m_activationIncludes.size());
            i++)
        {
            included = m_activationIncludes.get(i).equals(pkgName);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_activationExcludes != null) && (i < m_activationExcludes.size());
            i++)
        {
            excluded = m_activationExcludes.get(i).equals(pkgName);
        }
        return included && !excluded;
    }

    public String getId()
    {
        return m_id;
    }

    public synchronized void resolve(
        List<BundleRevision> fragments,
        List<ResolverWire> rws,
        Map<ResolverWire, Set<String>> requiredPkgWires)
        throws Exception
    {
        // This not only sets the wires for the module, but it also records
        // the dependencies this module has on other modules (i.e., the provider
        // end of the wire) to simplify bookkeeping.

        // If there is an existing wiring, then dispose of it, which will
        // remove any dependencies on other wirings.
        if (m_wiring != null)
        {
            m_wiring.dispose();
            m_wiring = null;
        }

        if (rws != null)
        {
            m_wiring = new BundleWiringImpl(
                m_logger, m_configMap, m_resolver, this, fragments, rws, requiredPkgWires);
        }
    }

    public synchronized void setSecurityContext(Object securityContext)
    {
        m_protectionDomain = (ProtectionDomain) securityContext;
    }

    public synchronized Object getSecurityContext()
    {
        return m_protectionDomain;
    }

    // TODO: FRAGMENT RESOLVER - Technically, this is only necessary for fragments.
    //       When we refactoring for the new R4.3 framework API, we'll have to see
    //       if this is still necessary, since the new BundleWirings API will give
    //       us another way to detect it.
    public boolean isRemovalPending()
    {
        return (m_bundle.getState() == Bundle.UNINSTALLED)
            || (this != ((BundleImpl) m_bundle).getCurrentRevision());
    }

    //
    // Content access methods.
    //

    public Content getContent()
    {
        return m_content;
    }

    private synchronized Content[] getContentPath()
    {
        if (m_contentPath == null)
        {
            try
            {
                m_contentPath = initializeContentPath();
            }
            catch (Exception ex)
            {
                m_logger.log(
                    m_bundle, Logger.LOG_ERROR, "Unable to get module class path.", ex);
            }
        }
        return m_contentPath;
    }

    private Content[] initializeContentPath() throws Exception
    {
        List contentList = new ArrayList();
        calculateContentPath(this, m_content, contentList, true);
        return (Content[]) contentList.toArray(new Content[contentList.size()]);
    }

    private List calculateContentPath(
        BundleRevision revision, Content content, List contentList, boolean searchFragments)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Create a list to contain the content path for the specified content.
        List localContentList = new ArrayList();

        // Find class path meta-data.
        String classPath = (String) ((BundleRevisionImpl) revision)
            .getHeaders().get(FelixConstants.BUNDLE_CLASSPATH);
        // Parse the class path into strings.
        List<String> classPathStrings = ManifestParser.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new ArrayList<String>(0);
        }

        // Create the bundles class path.
        for (int i = 0; i < classPathStrings.size(); i++)
        {
            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            classPathStrings.set(i, (classPathStrings.get(i).startsWith("/"))
                ? classPathStrings.get(i).substring(1)
                : classPathStrings.get(i));

            // Check for the bundle itself on the class path.
            if (classPathStrings.get(i).equals(FelixConstants.CLASS_PATH_DOT))
            {
                localContentList.add(content);
            }
            else
            {
                // Try to find the embedded class path entry in the current
                // content.
                Content embeddedContent = content.getEntryAsContent(classPathStrings.get(i));
                // If we found the embedded content, then add it to the
                // class path content list.
                if (embeddedContent != null)
                {
                    localContentList.add(embeddedContent);
                }
                else
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    m_logger.log(m_bundle, Logger.LOG_INFO,
                        "Class path entry not found: "
                        + classPathStrings.get(i));
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (localContentList.isEmpty())
        {
            localContentList.add(content);
        }

        // Now add the local contents to the global content list and return it.
        contentList.addAll(localContentList);
        return contentList;
    }

    URL getResourceLocal(String name)
    {
        URL url = null;

        // Remove leading slash, if present, but special case
        // "/" so that it returns a root URL...this isn't very
        // clean or meaninful, but the Spring guys want it.
        if (name.equals("/"))
        {
            // Just pick a class path index since it doesn't really matter.
            url = createURL(1, name);
        }
        else if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Check the module class path.
        Content[] contentPath = getContentPath();
        for (int i = 0;
            (url == null) &&
            (i < contentPath.length); i++)
        {
            if (contentPath[i].hasEntry(name))
            {
                url = createURL(i + 1, name);
            }
        }

        return url;
    }

    Enumeration getResourcesLocal(String name)
    {
        List l = new ArrayList();

        // Special case "/" so that it returns a root URLs for
        // each bundle class path entry...this isn't very
        // clean or meaningful, but the Spring guys want it.
        final Content[] contentPath = getContentPath();
        if (name.equals("/"))
        {
            for (int i = 0; i < contentPath.length; i++)
            {
                l.add(createURL(i + 1, name));
            }
        }
        else
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module class path.
            for (int i = 0; i < contentPath.length; i++)
            {
                if (contentPath[i].hasEntry(name))
                {
                    // Use the class path index + 1 for creating the path so
                    // that we can differentiate between module content URLs
                    // (where the path will start with 0) and module class
                    // path URLs.
                    l.add(createURL(i + 1, name));
                }
            }
        }

        return Collections.enumeration(l);
    }

    // TODO: API: Investigate how to handle this better, perhaps we need
    // multiple URL policies, one for content -- one for class path.
    public URL getEntry(String name)
    {
        URL url = null;

        // Check for the special case of "/", which represents
        // the root of the bundle according to the spec.
        if (name.equals("/"))
        {
            url = createURL(0, "/");
        }

        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module content.
            if (getContent().hasEntry(name))
            {
                // Module content URLs start with 0, whereas module
                // class path URLs start with the index into the class
                // path + 1.
                url = createURL(0, name);
            }
        }

        return url;
    }

    public boolean hasInputStream(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.hasEntry(urlPath);
        }
        return getContentPath()[index - 1].hasEntry(urlPath);
    }

    public InputStream getInputStream(int index, String urlPath)
        throws IOException
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsStream(urlPath);
        }
        return getContentPath()[index - 1].getEntryAsStream(urlPath);
    }

    public URL getLocalURL(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsURL(urlPath);
        }
        return getContentPath()[index - 1].getEntryAsURL(urlPath);
    }

    private URL createURL(int port, String path)
    {
        // Add a slash if there is one already, otherwise
        // the is no slash separating the host from the file
        // in the resulting URL.
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }

        try
        {
            return m_secureAction.createURL(null,
                FelixConstants.BUNDLE_URL_PROTOCOL + "://" +
                m_id + ":" + port + path, m_streamHandler);
        }
        catch (MalformedURLException ex)
        {
            m_logger.log(m_bundle,
                Logger.LOG_ERROR,
                "Unable to create resource URL.",
                ex);
        }
        return null;
    }

    public synchronized List<BundleRevision> getDependentImporters()
    {
        return m_dependentImporters;
    }

    public synchronized void addDependentImporter(BundleRevision br)
    {
        if (!m_dependentImporters.contains(br))
        {
            m_dependentImporters.add(br);
        }
    }

    public synchronized void removeDependentImporter(BundleRevision br)
    {
        m_dependentImporters.remove(br);
    }

    public synchronized List<BundleRevision> getDependentRequirers()
    {
        return m_dependentRequirers;
    }

    public synchronized void addDependentRequirer(BundleRevision br)
    {
        if (!m_dependentRequirers.contains(br))
        {
            m_dependentRequirers.add(br);
        }
    }

    public synchronized void removeDependentRequirer(BundleRevision br)
    {
        m_dependentRequirers.remove(br);
    }

    public synchronized List<BundleRevision> getDependents()
    {
        List<BundleRevision> dependents;
        if (Util.isFragment(this))
        {
            dependents = new ArrayList<BundleRevision>();
            List<BundleWire> wires = (m_wiring == null)
                ? null : m_wiring.getRequiredWires(null);
            for (int i = 0; (wires != null) && (i < wires.size()); i++)
            {
                dependents.add(wires.get(i).getProviderWiring().getRevision());
            }
        }
        else
        {
            dependents = new ArrayList<BundleRevision>
                (m_dependentImporters.size() + m_dependentRequirers.size());
            dependents.addAll(m_dependentImporters);
            dependents.addAll(m_dependentRequirers);
        }
        return dependents;
    }

    public synchronized void close()
    {
        try
        {
            resolve(null, null, null);
        }
        catch (Exception ex)
        {
            m_logger.log(Logger.LOG_ERROR, "Error releasing revision: " + ex.getMessage(), ex);
        }
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
        m_contentPath = null;
    }

    @Override
    public String toString()
    {
        return m_id;
    }

    static URL convertToLocalUrl(URL url)
    {
        if (url.getProtocol().equals("bundle"))
        {
            try
            {
                url = ((URLHandlersBundleURLConnection)
                    url.openConnection()).getLocalURL();
            }
            catch (IOException ex)
            {
                // Ignore and add original url.
            }
        }
        return url;
    }
}
