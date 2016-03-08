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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.TreeSet;

import org.apache.felix.framework.cache.Content;
import org.apache.felix.framework.cache.JarContent;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.ResourceNotFoundException;
import org.apache.felix.framework.util.CompoundEnumeration;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.ImmutableList;
import org.apache.felix.framework.util.SecurityManagerEx;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.NativeLibrary;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

public class BundleWiringImpl implements BundleWiring
{
    public final static int LISTRESOURCES_DEBUG = 1048576;

    public final static int EAGER_ACTIVATION = 0;
    public final static int LAZY_ACTIVATION = 1;

    private final Logger m_logger;
    private final Map m_configMap;
    private final StatefulResolver m_resolver;
    private final BundleRevisionImpl m_revision;
    private final List<BundleRevision> m_fragments;
    // Wire list is copy-on-write since it may change due to
    // dynamic imports.
    private volatile List<BundleWire> m_wires;
    // Imported package map is copy-on-write since it may change
    // due to dynamic imports.
    private volatile Map<String, BundleRevision> m_importedPkgs;
    private final Map<String, List<BundleRevision>> m_requiredPkgs;
    private final List<BundleCapability> m_resolvedCaps;
    private final Map<String, List<List<String>>> m_includedPkgFilters;
    private final Map<String, List<List<String>>> m_excludedPkgFilters;
    private final List<BundleRequirement> m_resolvedReqs;
    private final List<NativeLibrary> m_resolvedNativeLibs;
    private final List<Content> m_fragmentContents;

    private volatile List<BundleRequirement> m_wovenReqs = null;

    private BundleClassLoader m_classLoader;

    // Bundle-specific class loader for boot delegation.
    private final ClassLoader m_bootClassLoader;
    // Default class loader for boot delegation.
    private final static ClassLoader m_defBootClassLoader;

    // Statically define the default class loader for boot delegation.
    static
    {
        ClassLoader cl = null;
        try
        {
            Constructor ctor = BundleRevisionImpl.getSecureAction().getDeclaredConstructor(
                SecureClassLoader.class, new Class[] { ClassLoader.class });
            BundleRevisionImpl.getSecureAction().setAccesssible(ctor);
            cl = (ClassLoader) BundleRevisionImpl.getSecureAction().invoke(
                ctor, new Object[] { null });
        }
        catch (Throwable ex)
        {
            // On Android we get an exception if we set the parent class loader
            // to null, so we will work around that case by setting the parent
            // class loader to the system class loader in getClassLoader() below.
            cl = null;
            System.err.println("Problem creating boot delegation class loader: " + ex);
        }
        m_defBootClassLoader = cl;
    }

    // Boolean flag to enable/disable implicit boot delegation.
    private final boolean m_implicitBootDelegation;
    // Boolean flag to enable/disable local URLs.
    private final boolean m_useLocalURLs;

    // Re-usable security manager for accessing class context.
    private static SecurityManagerEx m_sm = new SecurityManagerEx();

    // Thread local to detect class loading cycles.
    private final ThreadLocal m_cycleCheck = new ThreadLocal();

    // Thread local to keep track of deferred activation.
    private static final ThreadLocal m_deferredActivation = new ThreadLocal();

    // Flag indicating whether we are on an old JVM or not.
    private volatile static boolean m_isPreJava5 = false;

    // Flag indicating whether this wiring has been disposed.
    private volatile boolean m_isDisposed = false;

    BundleWiringImpl(
        Logger logger, Map configMap, StatefulResolver resolver,
        BundleRevisionImpl revision, List<BundleRevision> fragments,
        List<BundleWire> wires,
        Map<String, BundleRevision> importedPkgs,
        Map<String, List<BundleRevision>> requiredPkgs)
        throws Exception
    {
        m_logger = logger;
        m_configMap = configMap;
        m_resolver = resolver;
        m_revision = revision;
        m_importedPkgs = importedPkgs;
        m_requiredPkgs = requiredPkgs;
        m_wires = ImmutableList.newInstance(wires);

        // We need to sort the fragments and add ourself as a dependent of each one.
        // We also need to create an array of fragment contents to attach to our
        // content path.
        List<Content> fragmentContents = null;
        if (fragments != null)
        {
            // Sort fragments according to ID order, if necessary.
            // Note that this sort order isn't 100% correct since
            // it uses a string, but it is likely close enough and
            // avoids having to create more objects.
            if (fragments.size() > 1)
            {
                SortedMap<String, BundleRevision> sorted = new TreeMap<String, BundleRevision>();
                for (BundleRevision f : fragments)
                {
                    sorted.put(((BundleRevisionImpl) f).getId(), f);
                }
                fragments = new ArrayList(sorted.values());
            }
            fragmentContents = new ArrayList<Content>(fragments.size());
            for (int i = 0; (fragments != null) && (i < fragments.size()); i++)
            {
                fragmentContents.add(
                    ((BundleRevisionImpl) fragments.get(i)).getContent()
                        .getEntryAsContent(FelixConstants.CLASS_PATH_DOT));
            }
        }
        m_fragments = fragments;
        m_fragmentContents = fragmentContents;

        // Calculate resolved list of requirements, which includes:
        // 1. All requirements for which we have a wire.
        // 2. All dynamic imports from the host and any fragments.
        // Also create set of imported packages so we can eliminate any
        // substituted exports from our resolved capabilities.
        Set<String> imports = new HashSet<String>();
        List<BundleRequirement> reqList = new ArrayList<BundleRequirement>();
        // First add resolved requirements from wires.
        for (BundleWire bw : wires)
        {
            // Fragments may have multiple wires for the same requirement, so we
            // need to check for and avoid duplicates in that case.
            if (!bw.getRequirement().getNamespace().equals(BundleRevision.HOST_NAMESPACE)
                || !reqList.contains(bw.getRequirement()))
            {
                reqList.add(bw.getRequirement());
                if (bw.getRequirement().getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    imports.add((String)
                        bw.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
                }
            }
        }
        // Next add dynamic requirements from host.
        for (BundleRequirement req : m_revision.getDeclaredRequirements(null))
        {
            if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
            {
                String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
                if ((resolution != null) && (resolution.equals("dynamic")))
                {
                    reqList.add(req);
                }
            }
        }
        // Finally, add dynamic requirements from fragments.
        if (m_fragments != null)
        {
            for (BundleRevision fragment : m_fragments)
            {
                for (BundleRequirement req : fragment.getDeclaredRequirements(null))
                {
                    if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
                        if ((resolution != null) && (resolution.equals("dynamic")))
                        {
                            reqList.add(req);
                        }
                    }
                }
            }
        }
        m_resolvedReqs = ImmutableList.newInstance(reqList);

        // Calculate resolved list of capabilities, which includes:
        // 1. All capabilities from host and any fragments except for exported
        //    packages that we have an import (i.e., the export was substituted).
        // 2. For fragments the identity capability only.
        // And nothing else at this time.
        boolean isFragment = Util.isFragment(revision);
        List<BundleCapability> capList = new ArrayList<BundleCapability>();
        // Also keep track of whether any resolved package capabilities are filtered.
        Map<String, List<List<String>>> includedPkgFilters =
            new HashMap<String, List<List<String>>>();
        Map<String, List<List<String>>> excludedPkgFilters =
            new HashMap<String, List<List<String>>>();

        if (isFragment)
        {
            // This is a fragment, add its identity capability
            for (BundleCapability cap : m_revision.getDeclaredCapabilities(null))
            {
                if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.getNamespace()))
                {
                    String effective = cap.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
                    if ((effective == null) || (effective.equals(Constants.EFFECTIVE_RESOLVE)))
                    {
                        capList.add(cap);
                    }
                }
            }
        }
        else
        {
            for (BundleCapability cap : m_revision.getDeclaredCapabilities(null))
            {
                if (!cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                    || (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                        && !imports.contains(cap.getAttributes()
                            .get(BundleRevision.PACKAGE_NAMESPACE).toString())))
                {
// TODO: OSGi R4.4 - We may need to make this more flexible since in the future it may
//       be possible to consider other effective values via OBR's Environment.isEffective().
                    String effective = cap.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
                    if ((effective == null) || (effective.equals(Constants.EFFECTIVE_RESOLVE)))
                    {
                        capList.add(cap);
                        if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                        {
                            List<List<String>> filters =
                                parsePkgFilters(cap, Constants.INCLUDE_DIRECTIVE);
                            if (filters != null)
                            {
                                includedPkgFilters.put((String)
                                    cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                                    filters);
                            }
                            filters = parsePkgFilters(cap, Constants.EXCLUDE_DIRECTIVE);
                            if (filters != null)
                            {
                                excludedPkgFilters.put((String)
                                    cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                                    filters);
                            }
                        }
                    }
                }
            }
            if (m_fragments != null)
            {
                for (BundleRevision fragment : m_fragments)
                {
                    for (BundleCapability cap : fragment.getDeclaredCapabilities(null))
                    {
                        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.getNamespace())) {
                            // The identity capability is not transferred from the fragment to the bundle
                            continue;
                        }

                        if (!cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                            || (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                                && !imports.contains(cap.getAttributes()
                                    .get(BundleRevision.PACKAGE_NAMESPACE).toString())))
                        {
// TODO: OSGi R4.4 - We may need to make this more flexible since in the future it may
//       be possible to consider other effective values via OBR's Environment.isEffective().
                            String effective = cap.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
                            if ((effective == null) || (effective.equals(Constants.EFFECTIVE_RESOLVE)))
                            {
                                capList.add(cap);
                                if (cap.getNamespace().equals(
                                    BundleRevision.PACKAGE_NAMESPACE))
                                {
                                    List<List<String>> filters =
                                        parsePkgFilters(
                                            cap, Constants.INCLUDE_DIRECTIVE);
                                    if (filters != null)
                                    {
                                        includedPkgFilters.put((String)
                                            cap.getAttributes()
                                                .get(BundleRevision.PACKAGE_NAMESPACE),
                                            filters);
                                    }
                                    filters = parsePkgFilters(cap, Constants.EXCLUDE_DIRECTIVE);
                                    if (filters != null)
                                    {
                                        excludedPkgFilters.put((String)
                                            cap.getAttributes()
                                                .get(BundleRevision.PACKAGE_NAMESPACE),
                                            filters);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (System.getSecurityManager() != null)
        {
            for (Iterator<BundleCapability> iter = capList.iterator();iter.hasNext();)
            {
                BundleCapability cap = iter.next();
                if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    if (!((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                        new PackagePermission((String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE), PackagePermission.EXPORTONLY)))
                    {
                        iter.remove();
                    }
                }
                else if (!cap.getNamespace().equals(BundleRevision.HOST_NAMESPACE) && !cap.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE) &&
                    !cap.getNamespace().equals("osgi.ee"))
                {
                    if (!((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                        new CapabilityPermission(cap.getNamespace(), CapabilityPermission.PROVIDE)))
                    {
                        iter.remove();
                    }
                }
            }
        }

        m_resolvedCaps = ImmutableList.newInstance(capList);
        m_includedPkgFilters = (includedPkgFilters.isEmpty())
            ? Collections.EMPTY_MAP : includedPkgFilters;
        m_excludedPkgFilters = (excludedPkgFilters.isEmpty())
            ? Collections.EMPTY_MAP : excludedPkgFilters;

        List<NativeLibrary> libList = (m_revision.getDeclaredNativeLibraries() == null)
            ? new ArrayList<NativeLibrary>()
            : new ArrayList<NativeLibrary>(m_revision.getDeclaredNativeLibraries());
        for (int fragIdx = 0;
            (m_fragments != null) && (fragIdx < m_fragments.size());
            fragIdx++)
        {
            List<NativeLibrary> libs =
                ((BundleRevisionImpl) m_fragments.get(fragIdx))
                    .getDeclaredNativeLibraries();
            for (int reqIdx = 0;
                (libs != null) && (reqIdx < libs.size());
                reqIdx++)
            {
                libList.add(libs.get(reqIdx));
            }
        }
        // We need to return null here if we don't have any libraries, since a
        // zero-length array is used to indicate that matching native libraries
        // could not be found when resolving the bundle.
        m_resolvedNativeLibs = (libList.isEmpty())
            ? null
            : ImmutableList.newInstance(libList);

        ClassLoader bootLoader = m_defBootClassLoader;
        if (revision.getBundle().getBundleId() != 0)
        {
            Object map = m_configMap.get(FelixConstants.BOOT_CLASSLOADERS_PROP);
            if (map instanceof Map)
            {
                Object l = ((Map) map).get(m_revision.getBundle());
                if (l instanceof ClassLoader)
                {
                    bootLoader = (ClassLoader) l;
                }
            }
        }
        m_bootClassLoader = bootLoader;

        m_implicitBootDelegation =
            (m_configMap.get(FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP) == null)
            || Boolean.valueOf(
                (String) m_configMap.get(
                    FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP)).booleanValue();

        m_useLocalURLs =
            (m_configMap.get(FelixConstants.USE_LOCALURLS_PROP) == null)
                ? false : true;
    }

    private static List<List<String>> parsePkgFilters(BundleCapability cap, String filtername)
    {
        List<List<String>> filters = null;
        String include = cap.getDirectives().get(filtername);
        if (include != null)
        {
            List<String> filterStrings = ManifestParser.parseDelimitedString(include, ",");
            filters = new ArrayList<List<String>>(filterStrings.size());

            for (int filterIdx = 0; filterIdx < filterStrings.size(); filterIdx++)
            {
                List<String> substrings =
                    SimpleFilter.parseSubstring(filterStrings.get(filterIdx));
                filters.add(substrings);
            }
        }
        return filters;
    }

    @Override
    public String toString()
    {
        return m_revision.getBundle().toString();
    }

    public synchronized void dispose()
    {
        if (m_fragmentContents != null)
        {
            for (Content content : m_fragmentContents)
            {
                content.close();
            }
        }
        m_classLoader = null;
        m_isDisposed = true;
    }

// TODO: OSGi R4.3 - This really shouldn't be public, but it is needed by the
//       resolver to determine if a bundle can dynamically import.
    public boolean hasPackageSource(String pkgName)
    {
        return (m_importedPkgs.containsKey(pkgName) || m_requiredPkgs.containsKey(pkgName));
    }

// TODO: OSGi R4.3 - This really shouldn't be public, but it is needed by the
//       to implement dynamic imports.
    public BundleRevision getImportedPackageSource(String pkgName)
    {
        return m_importedPkgs.get(pkgName);
    }

    List<BundleRevision> getFragments()
    {
        return m_fragments;
    }

    List<Content> getFragmentContents()
    {
        return m_fragmentContents;
    }

    @Override
    public boolean isCurrent()
    {
        BundleRevision current = getBundle().adapt(BundleRevision.class);
        return (current != null) && (current.getWiring() == this);
    }

    @Override
    public synchronized boolean isInUse()
    {
        return !m_isDisposed;
    }

    @Override
    public List<Capability> getResourceCapabilities(String namespace)
    {
        return BundleRevisionImpl.asCapabilityList(getCapabilities(namespace));
    }

    @Override
    public List<BundleCapability> getCapabilities(String namespace)
    {
        if (isInUse())
        {
            List<BundleCapability> result = m_resolvedCaps;
            if (namespace != null)
            {
                result = new ArrayList<BundleCapability>();
                for (BundleCapability cap : m_resolvedCaps)
                {
                    if (cap.getNamespace().equals(namespace))
                    {
                        result.add(cap);
                    }
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public List<Requirement> getResourceRequirements(String namespace)
    {
        return BundleRevisionImpl.asRequirementList(getRequirements(namespace));
    }

    @Override
    public List<BundleRequirement> getRequirements(String namespace)
    {
        if (isInUse())
        {
            List<BundleRequirement> searchReqs = m_resolvedReqs;
            List<BundleRequirement> wovenReqs = m_wovenReqs;
            List<BundleRequirement> result = m_resolvedReqs;

            if (wovenReqs != null)
            {
                searchReqs = new ArrayList<BundleRequirement>(m_resolvedReqs);
                searchReqs.addAll(wovenReqs);
                result = searchReqs;
            }

            if (namespace != null)
            {
                result = new ArrayList<BundleRequirement>();
                for (BundleRequirement req : searchReqs)
                {
                    if (req.getNamespace().equals(namespace))
                    {
                        result.add(req);
                    }
                }
            }
            return result;
        }
        return null;
    }

    public List<NativeLibrary> getNativeLibraries()
    {
        return m_resolvedNativeLibs;
    }

    private static List<Wire> asWireList(List wires)
    {
        return wires;
    }

    @Override
    public List<Wire> getProvidedResourceWires(String namespace)
    {
        return asWireList(getProvidedWires(namespace));
    }

    @Override
    public List<BundleWire> getProvidedWires(String namespace)
    {
        if (isInUse())
        {
            return m_revision.getBundle()
                .getFramework().getDependencies().getProvidedWires(m_revision, namespace);
        }
        return null;
    }

    @Override
    public List<Wire> getRequiredResourceWires(String namespace)
    {
        return asWireList(getRequiredWires(namespace));
    }

    @Override
    public List<BundleWire> getRequiredWires(String namespace)
    {
        if (isInUse())
        {
            List<BundleWire> result = m_wires;
            if (namespace != null)
            {
                result = new ArrayList<BundleWire>();
                for (BundleWire bw : m_wires)
                {
                    if (bw.getRequirement().getNamespace().equals(namespace))
                    {
                        result.add(bw);
                    }
                }
            }
            return result;
        }
        return null;
    }

    public synchronized void addDynamicWire(BundleWire wire)
    {
        // Make new wires list.
        List<BundleWire> wires = new ArrayList<BundleWire>(m_wires);
        wires.add(wire);
        // Make new imported package map.
        Map<String, BundleRevision> importedPkgs =
            new HashMap<String, BundleRevision>(m_importedPkgs);
        importedPkgs.put(
            (String) wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
            wire.getProviderWiring().getRevision());
        // Update associated member values.
        // Technically, there is a window here where readers won't see
        // both values updates at the same time, but it seems unlikely
        // to cause any issues.
        m_wires = ImmutableList.newInstance(wires);
        m_importedPkgs = importedPkgs;
    }

    @Override
    public BundleRevision getResource()
    {
        return m_revision;
    }

    @Override
    public BundleRevision getRevision()
    {
        return m_revision;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        if (m_isDisposed)
        {
            return null;
        }
        return getClassLoaderInternal();
    }

    private synchronized ClassLoader getClassLoaderInternal()
    {
        // Only try to create the class loader if the bundle
        // is not disposed.
        if (!m_isDisposed && (m_classLoader == null))
        {
            // Determine which class loader to use based on which
            // Java platform we are running on.
            Class clazz;
            if (m_isPreJava5)
            {
                clazz = BundleClassLoader.class;
            }
            else
            {
                try
                {
                    clazz = BundleClassLoaderJava5.class;
                }
                catch (Throwable th)
                {
                    // If we are on pre-Java5 then we will get a verify error
                    // here since we try to override a getResources() which is
                    // a final method in pre-Java5.
                    m_isPreJava5 = true;
                    clazz = BundleClassLoader.class;
                }
            }

            // Use SecureAction to create the class loader if security is
            // enabled; otherwise, create it directly.
            try
            {
                Constructor ctor = BundleRevisionImpl.getSecureAction()
                    .getConstructor(clazz, new Class[] { BundleWiringImpl.class, ClassLoader.class, Logger.class });
                m_classLoader = (BundleClassLoader)
                    BundleRevisionImpl.getSecureAction().invoke(ctor,
                    new Object[] { this, determineParentClassLoader(), m_logger });
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Unable to create module class loader: "
                    + ex.getMessage() + " [" + ex.getClass().getName() + "]");
            }
        }
        return m_classLoader;
    }

    @Override
    public List<URL> findEntries(String path, String filePattern, int options)
    {
        if (isInUse())
        {
            if (!Util.isFragment(m_revision))
            {
                Enumeration<URL> e =
                    m_revision.getBundle().getFramework()
                        .findBundleEntries(m_revision, path, filePattern,
                           (options & BundleWiring.FINDENTRIES_RECURSE) > 0);
                List<URL> entries = new ArrayList<URL>();
                while ((e != null) && e.hasMoreElements())
                {
                    entries.add(e.nextElement());
                }
                return ImmutableList.newInstance(entries);
            }
            return Collections.EMPTY_LIST;
        }
        return null;
    }

    // Thread local to detect class loading cycles.
    private final ThreadLocal m_listResourcesCycleCheck = new ThreadLocal();

// TODO: OSGi R4.3 - Should this be synchronized or should we take a snapshot?
    @Override
    public synchronized Collection<String> listResources(
        String path, String filePattern, int options)
    {
        // Implementation note: If you enable the DEBUG option for
        // listResources() to print from where each resource comes,
        // it will not give 100% accurate answers in the face of
        // Require-Bundle cycles with overlapping content since
        // the actual source will depend on who does the class load
        // first. Further, normal class loaders cache class load
        // results so it is always the same subsequently, but we
        // don't do that here so it will always return a different
        // result depending upon who is asking. Moral to the story:
        // don't do cycles and certainly don't do them with
        // overlapping content.

        Collection<String> resources = null;

        // Normalize path.
        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }
        if ((path.length() > 0) && (path.charAt(path.length() - 1) != '/'))
        {
            path = path + '/';
        }

        // Parse the file filter.
        filePattern = (filePattern == null) ? "*" : filePattern;
        List<String> pattern = SimpleFilter.parseSubstring(filePattern);

        // We build an internal collection of ResourceSources, since this
        // allows us to print out additional debug information.
        Collection<ResourceSource> sources = listResourcesInternal(path, pattern, options);
        if (sources != null)
        {
            boolean debug = (options & LISTRESOURCES_DEBUG) > 0;
            resources = new TreeSet<String>();
            for (ResourceSource source : sources)
            {
                if (debug)
                {
                    resources.add(source.toString());
                }
                else
                {
                    resources.add(source.m_resource);
                }
            }
        }
        return resources;
    }

    private Collection<ResourceSource> listResourcesInternal(
        String path, List<String> pattern, int options)
    {
        if (isInUse())
        {
            boolean recurse = (options & BundleWiring.LISTRESOURCES_RECURSE) > 0;
            boolean localOnly = (options & BundleWiring.LISTRESOURCES_LOCAL) > 0;

            // Check for cycles, which can happen with Require-Bundle.
            Set<String> cycles = (Set<String>) m_listResourcesCycleCheck.get();
            if (cycles == null)
            {
                cycles = new HashSet<String>();
                m_listResourcesCycleCheck.set(cycles);
            }
            if (cycles.contains(path))
            {
                return Collections.EMPTY_LIST;
            }
            cycles.add(path);

            try
            {
                // Calculate set of remote resources (i.e., those either
                // imported or required).
                Collection<ResourceSource> remoteResources = new TreeSet<ResourceSource>();
                // Imported packages cannot have merged content, so we need to
                // keep track of these packages.
                Set<String> noMerging = new HashSet<String>();
                // Loop through wires to compute remote resources.
                for (BundleWire bw : m_wires)
                {
                    if (bw.getCapability().getNamespace()
                        .equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        // For imported packages, we only need to calculate
                        // the remote resources of the specific imported package.
                        remoteResources.addAll(
                            calculateRemotePackageResources(
                                bw, bw.getCapability(), recurse,
                                    path, pattern, noMerging));
                    }
                    else if (bw.getCapability().getNamespace()
                        .equals(BundleRevision.BUNDLE_NAMESPACE))
                    {
                        // For required bundles, all declared package capabilities
                        // from the required bundle will be available to requirers,
                        // so get the target required bundle's declared packages
                        // and handle them in a similar fashion to a normal import
                        // except that their content can be merged with local
                        // packages.
                        List<BundleCapability> exports =
                            bw.getProviderWiring().getRevision()
                                .getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                        for (BundleCapability export : exports)
                        {
                            remoteResources.addAll(
                                calculateRemotePackageResources(
                                    bw, export, recurse, path, pattern, null));
                        }

                        // Since required bundle may reexport bundles it requires,
                        // check its wires for this case.
                        List<BundleWire> requiredBundles =
                            bw.getProviderWiring().getRequiredWires(
                                BundleRevision.BUNDLE_NAMESPACE);
                        for (BundleWire rbWire : requiredBundles)
                        {
                            String visibility =
                                rbWire.getRequirement().getDirectives()
                                    .get(Constants.VISIBILITY_DIRECTIVE);
                            if ((visibility != null)
                                && (visibility.equals(Constants.VISIBILITY_REEXPORT)))
                            {
                                // For each reexported required bundle, treat them
                                // in a similar fashion as a normal required bundle
                                // by including all of their declared package
                                // capabilities in the requiring bundle's class
                                // space.
                                List<BundleCapability> reexports =
                                    rbWire.getProviderWiring().getRevision()
                                        .getDeclaredCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                                for (BundleCapability reexport : reexports)
                                {
                                    remoteResources.addAll(
                                        calculateRemotePackageResources(
                                            bw, reexport, recurse, path, pattern, null));
                                }
                            }
                        }
                    }
                }

                // Calculate set of local resources (i.e., those contained
                // in the revision or its fragments).
                Collection<ResourceSource> localResources = new TreeSet<ResourceSource>();
                // Get the revision's content path, which includes contents
                // from fragments.
                List<Content> contentPath = m_revision.getContentPath();
                for (Content content : contentPath)
                {
                    Enumeration<String> e = content.getEntries();
                    if (e != null)
                    {
                        while (e.hasMoreElements())
                        {
                            String resource = e.nextElement();
                            String resourcePath = getTrailingPath(resource);
                            if (!noMerging.contains(resourcePath))
                            {
                                if ((!recurse && resourcePath.equals(path))
                                    || (recurse && resourcePath.startsWith(path)))
                                {
                                    if (matchesPattern(pattern, getPathHead(resource)))
                                    {
                                        localResources.add(
                                            new ResourceSource(resource, m_revision));
                                    }
                                }
                            }
                        }
                    }
                }

                if (localOnly)
                {
                    return localResources;
                }
                else
                {
                    remoteResources.addAll(localResources);
                    return remoteResources;
                }
            }
            finally
            {
                cycles.remove(path);
                if (cycles.isEmpty())
                {
                    m_listResourcesCycleCheck.set(null);
                }
            }
        }
        return null;
    }

    private Collection<ResourceSource> calculateRemotePackageResources(
        BundleWire bw, BundleCapability cap, boolean recurse,
        String path, List<String> pattern, Set<String> noMerging)
    {
        Collection<ResourceSource> resources = Collections.EMPTY_SET;

        // Convert package name to a path.
        String subpath = (String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
        subpath = subpath.replace('.', '/') + '/';
        // If necessary, record that this package should not be merged
        // with local content.
        if (noMerging != null)
        {
            noMerging.add(subpath);
        }

        // If we are not recuring, check for path equality or if
        // we are recursing, check that the subpath starts with
        // the target path.
        if ((!recurse && subpath.equals(path))
            || (recurse && subpath.startsWith(path)))
        {
            // Delegate to the original provider wiring to have it calculate
            // the list of resources in the package. In this case, we don't
            // want to recurse since we want the precise package.
            resources =
                ((BundleWiringImpl) bw.getProviderWiring()).listResourcesInternal(
                    subpath, pattern, 0);

            // The delegatedResources result will include subpackages
            // which need to be filtered out, since imported packages
            // do not give access to subpackages. If a subpackage is
            // imported, it will be added by its own wire.
            for (Iterator<ResourceSource> it = resources.iterator();
                it.hasNext(); )
            {
                ResourceSource reqResource = it.next();
                if (reqResource.m_resource.charAt(
                    reqResource.m_resource.length() - 1) == '/')
                {
                    it.remove();
                }
            }
        }
        // If we are not recursing, but the required package
        // is a child of the desired path, then include its
        // immediate child package. We do this so that it is
        // possible to use listResources() to walk the resource
        // tree similar to doing a directory walk one level
        // at a time.
        else if (!recurse && subpath.startsWith(path))
        {
            int idx = subpath.indexOf('/', path.length());
            if (idx >= 0)
            {
                subpath = subpath.substring(0, idx + 1);
            }
            if (matchesPattern(pattern, getPathHead(subpath)))
            {
                resources = Collections.singleton(
                    new ResourceSource(subpath, bw.getProviderWiring().getRevision()));
            }
        }

        return resources;
    }

    private static String getPathHead(String resource)
    {
        if (resource.length() == 0)
        {
            return resource;
        }
        int idx = (resource.charAt(resource.length() - 1) == '/')
            ? resource.lastIndexOf('/', resource.length() - 2)
            : resource.lastIndexOf('/');
        if (idx < 0)
        {
            return resource;
        }
        return resource.substring(idx + 1);
    }

    private static String getTrailingPath(String resource)
    {
        if (resource.length() == 0)
        {
            return null;
        }
        int idx = (resource.charAt(resource.length() - 1) == '/')
            ? resource.lastIndexOf('/', resource.length() - 2)
            : resource.lastIndexOf('/');
        if (idx < 0)
        {
            return "";
        }
        return resource.substring(0, idx + 1);
    }

    private static boolean matchesPattern(List<String> pattern, String resource)
    {
        if (resource.charAt(resource.length() - 1) == '/')
        {
            resource = resource.substring(0, resource.length() - 1);
        }
        return SimpleFilter.compareSubstring(pattern, resource);
    }

    @Override
    public BundleImpl getBundle()
    {
        return m_revision.getBundle();
    }

    //
    // Class loader implementation methods.
    //

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
            return BundleRevisionImpl.getSecureAction().createURL(null,
                FelixConstants.BUNDLE_URL_PROTOCOL + "://" +
                m_revision.getId() + ":" + port + path,
                getBundle().getFramework().getBundleStreamHandler());
        }
        catch (MalformedURLException ex)
        {
            m_logger.log(m_revision.getBundle(),
                Logger.LOG_ERROR,
                "Unable to create resource URL.",
                ex);
        }
        return null;
    }

    public Enumeration getResourcesByDelegation(String name)
    {
        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (!requestSet.contains(name))
        {
            requestSet.add(name);
            try
            {
                return findResourcesByDelegation(name);
            }
            finally
            {
                requestSet.remove(name);
            }
        }

        return null;
    }

    private Enumeration findResourcesByDelegation(String name)
    {
        Enumeration urls = null;
        List completeUrlList = new ArrayList();

        // Get the package of the target class/resource.
        String pkgName = Util.getResourcePackage(name);

        // Delegate any packages listed in the boot delegation
        // property to the parent class loader.
        if (shouldBootDelegate(pkgName))
        {
            try
            {
                // Get the appropriate class loader for delegation.
                ClassLoader bdcl = getBootDelegationClassLoader();
                urls = bdcl.getResources(name);
            }
            catch (IOException ex)
            {
                // This shouldn't happen and even if it does, there
                // is nothing we can do, so just ignore it.
            }
            // If this is a java.* package, then always terminate the
            // search; otherwise, continue to look locally.
            if (pkgName.startsWith("java."))
            {
                return urls;
            }

            completeUrlList.add(urls);
        }

        // Look in the revisions's imported packages. If the package is
        // imported, then we stop searching no matter the result since
        // imported packages cannot be split.
        BundleRevision provider = m_importedPkgs.get(pkgName);
        if (provider != null)
        {
            // Delegate to the provider revision.
            urls = ((BundleWiringImpl) provider.getWiring()).getResourcesByDelegation(name);

            // If we find any resources, then add them.
            if ((urls != null) && (urls.hasMoreElements()))
            {
                completeUrlList.add(urls);
            }

            // Always return here since imported packages cannot be split
            // across required bundles or the revision's content.
            return new CompoundEnumeration((Enumeration[])
                completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
        }

        // See whether we can get the resource from the required bundles and
        // regardless of whether or not this is the case continue to the next
        // step potentially passing on the result of this search (if any).
        List<BundleRevision> providers = m_requiredPkgs.get(pkgName);
        if (providers != null)
        {
            for (BundleRevision p : providers)
            {
                // Delegate to the provider revision.
                urls = ((BundleWiringImpl) p.getWiring()).getResourcesByDelegation(name);

                // If we find any resources, then add them.
                if ((urls != null) && (urls.hasMoreElements()))
                {
                    completeUrlList.add(urls);
                }

                // Do not return here, since required packages can be split
                // across the revision's content.
            }
        }

        // Try the module's own class path. If we can find the resource then
        // return it together with the results from the other searches else
        // try to look into the dynamic imports.
        urls = m_revision.getResourcesLocal(name);
        if ((urls != null) && (urls.hasMoreElements()))
        {
            completeUrlList.add(urls);
        }
        else
        {
            // If not found, then try the module's dynamic imports.
            // At this point, the module's imports were searched and so was the
            // the module's content. Now we make an attempt to load the
            // class/resource via a dynamic import, if possible.
            try
            {
                provider = m_resolver.resolve(m_revision, pkgName);
            }
            catch (ResolutionException ex)
            {
                // Ignore this since it is likely normal.
            }
            catch (BundleException ex)
            {
                // Ignore this since it is likely the result of a resolver hook.
            }
            if (provider != null)
            {
                // Delegate to the provider revision.
                urls = ((BundleWiringImpl) provider.getWiring()).getResourcesByDelegation(name);

                // If we find any resources, then add them.
                if ((urls != null) && (urls.hasMoreElements()))
                {
                    completeUrlList.add(urls);
                }
            }
        }

        return new CompoundEnumeration((Enumeration[])
            completeUrlList.toArray(new Enumeration[completeUrlList.size()]));
    }

    private ClassLoader determineParentClassLoader()
    {
        // Determine the class loader's parent based on the
        // configuration property; use boot class loader by
        // default.
        String cfg = (String) m_configMap.get(Constants.FRAMEWORK_BUNDLE_PARENT);
        cfg = (cfg == null) ? Constants.FRAMEWORK_BUNDLE_PARENT_BOOT : cfg;
        final ClassLoader parent;
        if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_APP))
        {
            parent = BundleRevisionImpl.getSecureAction().getSystemClassLoader();
        }
        else if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_EXT))
        {
            parent = BundleRevisionImpl.getSecureAction().getParentClassLoader(
                BundleRevisionImpl.getSecureAction().getSystemClassLoader());
        }
        else if (cfg.equalsIgnoreCase(Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK))
        {
            parent = BundleRevisionImpl.getSecureAction()
                .getClassLoader(BundleRevisionImpl.class);
        }
        // On Android we cannot set the parent class loader to be null, so
        // we special case that situation here and set it to the system
        // class loader by default instead, which is not really spec.
        else if (m_bootClassLoader == null)
        {
            parent = BundleRevisionImpl.getSecureAction().getSystemClassLoader();
        }
        else
        {
            parent = null;
        }
        return parent;
    }

    boolean shouldBootDelegate(String pkgName)
    {
        // Always boot delegate if the bundle has a configured
        // boot class loader.
        if (m_bootClassLoader != m_defBootClassLoader)
        {
            return true;
        }

        boolean result = false;

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if (pkgName.length() > 0)
        {
            for (int i = 0;
                !result
                    && (i < getBundle()
                        .getFramework().getBootPackages().length);
                i++)
            {
                // Check if the boot package is wildcarded.
                // A wildcarded boot package will be in the form "foo.",
                // so a matching subpackage will start with "foo.", e.g.,
                // "foo.bar".
                if (getBundle().getFramework().getBootPackageWildcards()[i]
                    && pkgName.startsWith(
                        getBundle().getFramework().getBootPackages()[i]))
                {
                    return true;
                }
                // If not wildcarded, then check for an exact match.
                else if (getBundle()
                    .getFramework().getBootPackages()[i].equals(pkgName))
                {
                    return true;
                }
            }
        }

        return result;
    }

    synchronized ClassLoader getBootDelegationClassLoader()
    {
        // Get the appropriate class loader for delegation.
        ClassLoader parent = (m_classLoader == null)
            ? determineParentClassLoader() :
            BundleRevisionImpl.getSecureAction().getParentClassLoader(m_classLoader);
        return (parent == null) ? m_bootClassLoader : parent;
    }

    private static final Constructor m_dexFileClassConstructor;
    private static final Method m_dexFileClassLoadDex;
    private static final Method m_dexFileClassLoadClass;

    static
    {
        Constructor dexFileClassConstructor = null;
        Method dexFileClassLoadDex = null;
        Method dexFileClassLoadClass = null;
        try
        {
            Class dexFileClass;
            try
            {
                dexFileClass = Class.forName("dalvik.system.DexFile");
            }
            catch (Exception ex)
            {
                dexFileClass = Class.forName("android.dalvik.DexFile");
            }

            try
            {
                dexFileClassLoadDex = dexFileClass.getMethod("loadDex",
                    new Class[]{String.class, String.class, Integer.TYPE});
            }
            catch (Exception ex)
            {
                // Nothing we need to do
            }
            dexFileClassConstructor = dexFileClass.getConstructor(
                new Class[] { java.io.File.class });
            dexFileClassLoadClass = dexFileClass.getMethod("loadClass",
                new Class[] { String.class, ClassLoader.class });
        }
        catch (Throwable ex)
        {
           dexFileClassConstructor = null;
           dexFileClassLoadDex = null;
           dexFileClassLoadClass = null;
        }
        m_dexFileClassConstructor = dexFileClassConstructor;
        m_dexFileClassLoadDex = dexFileClassLoadDex;
        m_dexFileClassLoadClass = dexFileClassLoadClass;
    }

    public Class getClassByDelegation(String name) throws ClassNotFoundException
    {
        // We do not call getClassLoader().loadClass() for arrays because
        // it does not correctly handle array types, which is necessary in
        // cases like deserialization using a wrapper class loader.
        if ((name != null) && (name.length() > 0) && (name.charAt(0) == '['))
        {
            return Class.forName(name, false, getClassLoader());
        }

        // Check to see if the requested class is filtered.
        if (isFiltered(name))
        {
            throw new ClassNotFoundException(name);
        }

        ClassLoader cl = getClassLoaderInternal();
        if (cl == null)
        {
            throw new ClassNotFoundException(
                "Unable to load class '"
                + name
                + "' because the bundle wiring for "
                + m_revision.getSymbolicName()
                + " is no longer valid.");
        }
        return cl.loadClass(name);
    }

    private boolean isFiltered(String name)
    {
        String pkgName = Util.getClassPackage(name);
        List<List<String>> includeFilters = m_includedPkgFilters.get(pkgName);
        List<List<String>> excludeFilters = m_excludedPkgFilters.get(pkgName);

        if ((includeFilters == null) && (excludeFilters == null))
        {
            return false;
        }

        // Get the class name portion of the target class.
        String className = Util.getClassName(name);

        // If there are no include filters then all classes are included
        // by default, otherwise try to find one match.
        boolean included = (includeFilters == null);
        for (int i = 0;
            (!included) && (includeFilters != null) && (i < includeFilters.size());
            i++)
        {
            included = SimpleFilter.compareSubstring(includeFilters.get(i), className);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (excludeFilters != null) && (i < excludeFilters.size());
            i++)
        {
            excluded = SimpleFilter.compareSubstring(excludeFilters.get(i), className);
        }
        return !included || excluded;
    }

    public URL getResourceByDelegation(String name)
    {
        try
        {
            return (URL) findClassOrResourceByDelegation(name, false);
        }
        catch (ClassNotFoundException ex)
        {
            // This should never be thrown because we are loading resources.
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(m_revision.getBundle(),
                Logger.LOG_DEBUG,
                ex.getMessage());
        }
        return null;
    }

    private Object findClassOrResourceByDelegation(String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        Object result = null;

        Set requestSet = (Set) m_cycleCheck.get();
        if (requestSet == null)
        {
            requestSet = new HashSet();
            m_cycleCheck.set(requestSet);
        }
        if (requestSet.add(name))
        {
            try
            {
                // Get the package of the target class/resource.
                String pkgName = (isClass)
                    ? Util.getClassPackage(name)
                    : Util.getResourcePackage(name);

                // Delegate any packages listed in the boot delegation
                // property to the parent class loader.
                if (shouldBootDelegate(pkgName))
                {
                    try
                    {
                        // Get the appropriate class loader for delegation.
                        ClassLoader bdcl = getBootDelegationClassLoader();
                        result = (isClass)
                            ? (Object) bdcl.loadClass(name)
                            : (Object) bdcl.getResource(name);
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java.") || (result != null))
                        {
                            return result;
                        }
                    }
                    catch (ClassNotFoundException ex)
                    {
                        // If this is a java.* package, then always terminate the
                        // search; otherwise, continue to look locally if not found.
                        if (pkgName.startsWith("java."))
                        {
                            throw ex;
                        }
                    }
                }

                // Look in the revision's imports. Note that the search may
                // be aborted if this method throws an exception, otherwise
                // it continues if a null is returned.
                result = searchImports(pkgName, name, isClass);

                // If not found, try the revision's own class path.
                if (result == null)
                {
                    if (isClass)
                    {
                        ClassLoader cl = getClassLoaderInternal();
                        if (cl == null)
                        {
                            throw new ClassNotFoundException(
                                "Unable to load class '"
                                + name
                                + "' because the bundle wiring for "
                                + m_revision.getSymbolicName()
                                + " is no longer valid.");
                        }
                        result = ((BundleClassLoader) cl).findClass(name);
                    }
                    else
                    {
                        result = m_revision.getResourceLocal(name);
                    }

                    // If still not found, then try the revision's dynamic imports.
                    if (result == null)
                    {
                        result = searchDynamicImports(pkgName, name, isClass);
                    }
                }
            }
            finally
            {
                requestSet.remove(name);
            }
        }
        else
        {
            // If a cycle is detected, we should return null to break the
            // cycle. This should only ever be return to internal class
            // loading code and not to the actual instigator of the class load.
            return null;
        }

        if (result == null)
        {
            if (isClass)
            {
                throw new ClassNotFoundException(
                    name + " not found by " + this.getBundle());
            }
            else
            {
                throw new ResourceNotFoundException(
                    name + " not found by " + this.getBundle());
            }
        }

        return result;
    }

    private Object searchImports(String pkgName, String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // Check if the package is imported.
        BundleRevision provider = m_importedPkgs.get(pkgName);
        if (provider != null)
        {
            // If we find the class or resource, then return it.
            Object result = (isClass)
                ? (Object) ((BundleWiringImpl) provider.getWiring()).getClassByDelegation(name)
                : (Object) ((BundleWiringImpl) provider.getWiring()).getResourceByDelegation(name);
            if (result != null)
            {
                return result;
            }

            // If no class or resource was found, then we must throw an exception
            // since the provider of this package did not contain the
            // requested class and imported packages are atomic.
            if (isClass)
            {
                throw new ClassNotFoundException(name);
            }
            throw new ResourceNotFoundException(name);
        }

        // Check if the package is required.
        List<BundleRevision> providers = m_requiredPkgs.get(pkgName);
        if (providers != null)
        {
            for (BundleRevision p : providers)
            {
                // If we find the class or resource, then return it.
                try
                {
                    Object result = (isClass)
                        ? (Object) ((BundleWiringImpl) p.getWiring()).getClassByDelegation(name)
                        : (Object) ((BundleWiringImpl) p.getWiring()).getResourceByDelegation(name);
                    if (result != null)
                    {
                        return result;
                    }
                }
                catch (ClassNotFoundException ex)
                {
                    // Since required packages can be split, don't throw an
                    // exception here if it is not found. Instead, we'll just
                    // continue searching other required bundles and the
                    // revision's local content.
                }
            }
        }

        return null;
    }

    private Object searchDynamicImports(
        final String pkgName, final String name, final boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // At this point, the module's imports were searched and so was the
        // the module's content. Now we make an attempt to load the
        // class/resource via a dynamic import, if possible.
        BundleRevision provider = null;
        try
        {
            provider = m_resolver.resolve(m_revision, pkgName);
        }
        catch (ResolutionException ex)
        {
            // Ignore this since it is likely normal.
        }
        catch (BundleException ex)
        {
            // Ignore this since it is likely the result of a resolver hook.
        }

        // If the dynamic import was successful, then this initial
        // time we must directly return the result from dynamically
        // created package sources, but subsequent requests for
        // classes/resources in the associated package will be
        // processed as part of normal static imports.
        if (provider != null)
        {
            // Return the class or resource.
            return (isClass)
                ? (Object) ((BundleWiringImpl) provider.getWiring()).getClassByDelegation(name)
                : (Object) ((BundleWiringImpl) provider.getWiring()).getResourceByDelegation(name);
        }

        // If implicit boot delegation is enabled, then try to guess whether
        // we should boot delegate.
        if (m_implicitBootDelegation)
        {
            // At this point, the class/resource could not be found by the bundle's
            // static or dynamic imports, nor its own content. Before we throw
            // an exception, we will try to determine if the instigator of the
            // class/resource load was a class from a bundle or not. This is necessary
            // because the specification mandates that classes on the class path
            // should be hidden (except for java.*), but it does allow for these
            // classes/resources to be exposed by the system bundle as an export.
            // However, in some situations classes on the class path make the faulty
            // assumption that they can access everything on the class path from
            // every other class loader that they come in contact with. This is
            // not true if the class loader in question is from a bundle. Thus,
            // this code tries to detect that situation. If the class instigating
            // the load request was NOT from a bundle, then we will make the
            // assumption that the caller actually wanted to use the parent class
            // loader and we will delegate to it. If the class was
            // from a bundle, then we will enforce strict class loading rules
            // for the bundle and throw an exception.

            // Get the class context to see the classes on the stack.
            final Class[] classes = m_sm.getClassContext();
            try
            {
                if (System.getSecurityManager() != null)
                {
                    return AccessController
                        .doPrivileged(new PrivilegedExceptionAction()
                        {
                            @Override
                            public Object run() throws Exception
                            {
                                return doImplicitBootDelegation(classes, name,
                                    isClass);
                            }
                        });
                }
                else
                {
                    return doImplicitBootDelegation(classes, name, isClass);
                }
            }
            catch (PrivilegedActionException ex)
            {
                Exception cause = ex.getException();
                if (cause instanceof ClassNotFoundException)
                {
                    throw (ClassNotFoundException) cause;
                }
                else
                {
                    throw (ResourceNotFoundException) cause;
                }
            }
        }
        return null;
    }

    private Object doImplicitBootDelegation(Class[] classes, String name, boolean isClass)
        throws ClassNotFoundException, ResourceNotFoundException
    {
        // Start from 1 to skip security manager class.
        for (int i = 1; i < classes.length; i++)
        {
            // Find the first class on the call stack that is not from
            // the class loader that loaded the Felix classes or is not
            // a class loader or class itself, because we want to ignore
            // calls to ClassLoader.loadClass() and Class.forName() since
            // we are trying to find out who instigated the class load.
            // Also ignore inner classes of class loaders, since we can
            // assume they are a class loader too.

            // TODO: FRAMEWORK - This check is a hack and we should see if we can think
            // of another way to do it, since it won't necessarily work in all situations.
            // Since Felix uses threads for changing the start level
            // and refreshing packages, it is possible that there are no
            // bundle classes on the call stack; therefore, as soon as we
            // see Thread on the call stack we exit this loop. Other cases
            // where bundles actually use threads are not an issue because
            // the bundle classes will be on the call stack before the
            // Thread class.
            if (Thread.class.equals(classes[i]))
            {
                break;
            }
            // Break if the current class came from a bundle, since we should
            // not implicitly boot delegate in that case.
            else if (isClassLoadedFromBundleRevision(classes[i]))
            {
                break;
            }
            // Break if this goes through BundleImpl because it must be a call
            // to Bundle.loadClass() which should not implicitly boot delegate.
            else if (BundleImpl.class.equals(classes[i]))
            {
                break;
            }
            else if (isClassExternal(classes[i]))
            {
                try
                {
                    // Return the class or resource from the parent class loader.
                    return (isClass)
                        ? (Object) BundleRevisionImpl.getSecureAction()
                            .getClassLoader(this.getClass()).loadClass(name)
                        : (Object) BundleRevisionImpl.getSecureAction()
                            .getClassLoader(this.getClass()).getResource(name);
                }
                catch (NoClassDefFoundError ex)
                {
                    // Ignore, will return null
                }
                break;
            }
        }

        return null;
    }

    private boolean isClassLoadedFromBundleRevision(Class clazz)
    {
        // The target class is loaded by a bundle class loader,
        // then return true.
        if (BundleClassLoader.class.isInstance(
            BundleRevisionImpl.getSecureAction().getClassLoader(clazz)))
        {
            return true;
        }

        // If the target class was loaded from a class loader that
        // came from a bundle, then return true.
        ClassLoader last = null;
        for (ClassLoader cl = BundleRevisionImpl.getSecureAction().getClassLoader(clazz);
            (cl != null) && (last != cl);
            cl = BundleRevisionImpl.getSecureAction().getClassLoader(cl.getClass()))
        {
            last = cl;
            if (BundleClassLoader.class.isInstance(cl))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Tries to determine whether the given class is part of the framework or not.
     * Framework classes include everything in org.apache.felix.framework.* and
     * org.osgi.framework.*. We also consider ClassLoader and Class to be internal
     * classes, because they are inserted into the stack trace as a result of
     * method overloading. Typically, ClassLoader or Class will be mixed in
     * between framework classes or will be at the point where the class loading
     * request enters the framework class loading mechanism, which will then be
     * followed by either bundle or external code, which will then exit our
     * attempt to determine if we should boot delegate or not. Other standard
     * class loaders, like URLClassLoader, are considered external classes and
     * should trigger boot delegation. This means that bundles can create standard
     * class loaders to get access to boot packages, but this is the standard
     * behavior of class loaders.
     * @param clazz the class to determine if it is external or not.
     * @return <tt>true</tt> if the class is external, otherwise <tt>false</tt>.
     */
    private boolean isClassExternal(Class clazz)
    {
        if (clazz.getName().startsWith("org.apache.felix.framework."))
        {
            return false;
        }
        else if (clazz.getName().startsWith("org.osgi.framework."))
        {
            return false;
        }
        else if (ClassLoader.class.equals(clazz))
        {
            return false;
        }
        else if (Class.class.equals(clazz))
        {
            return false;
        }
        return true;
    }

    static class ToLocalUrlEnumeration implements Enumeration
    {
        final Enumeration m_enumeration;

        ToLocalUrlEnumeration(Enumeration enumeration)
        {
            m_enumeration = enumeration;
        }

        @Override
        public boolean hasMoreElements()
        {
            return m_enumeration.hasMoreElements();
        }

        @Override
        public Object nextElement()
        {
            return convertToLocalUrl((URL) m_enumeration.nextElement());
        }
    }

    public static class BundleClassLoaderJava5 extends BundleClassLoader
    {
        static final boolean m_isParallel;
        static
        {
            boolean registered = false;
            try
            {
                Method method = BundleRevisionImpl.getSecureAction()
                    .getDeclaredMethod(ClassLoader.class, "registerAsParallelCapable", null);

                BundleRevisionImpl.getSecureAction().setAccesssible(method);

                registered = ((Boolean) method.invoke(null)).booleanValue();
            }
            catch (Throwable th)
            {
                // This is OK on older java versions
            }

            m_isParallel = registered;
        }

        @Override
        protected boolean isParallel()
        {
            return m_isParallel;
        }

        private final BundleWiringImpl m_wiring;

        public BundleClassLoaderJava5(BundleWiringImpl wiring, ClassLoader parent, Logger logger)
        {
            super(wiring, parent, logger);
            m_wiring = wiring;
        }

        @Override
        public Enumeration getResources(String name)
        {
            Enumeration urls = m_wiring.getResourcesByDelegation(name);
            if (m_wiring.m_useLocalURLs)
            {
                urls = new ToLocalUrlEnumeration(urls);
            }
            return urls;
        }

        @Override
        protected Enumeration findResources(String name)
        {
            return m_wiring.m_revision.getResourcesLocal(name);
        }
    }

    public static class BundleClassLoader extends SecureClassLoader implements BundleReference
    {
         static
         {
             try
             {
                 Method method = BundleRevisionImpl.getSecureAction()
                    .getDeclaredMethod(ClassLoader.class, "registerAsParallelCapable", null);

                 BundleRevisionImpl.getSecureAction().setAccesssible(method);

                 method.invoke(null);
             }
             catch (Throwable th)
             {
                 // This is OK on older java versions
             }
         }

        // Flag used to determine if a class has been loaded from this class
        // loader or not.
        private volatile boolean m_isActivationTriggered = false;

        private final Map m_jarContentToDexFile;
        private Object[][] m_cachedLibs = new Object[0][];
        private static final int LIBNAME_IDX = 0;
        private static final int LIBPATH_IDX = 1;
        private final Map<String, Thread> m_classLocks = new HashMap<String, Thread>();
        private final BundleWiringImpl m_wiring;
        private final Logger m_logger;

        public BundleClassLoader(BundleWiringImpl wiring, ClassLoader parent, Logger logger)
        {
            super(parent);
            if (m_dexFileClassLoadClass != null)
            {
                m_jarContentToDexFile = new HashMap();
            }
            else
            {
                m_jarContentToDexFile = null;
            }
            m_wiring = wiring;
            m_logger = logger;
        }

        protected boolean isParallel()
        {
            return false;
        }

        public boolean isActivationTriggered()
        {
            return m_isActivationTriggered;
        }

        @Override
        public BundleImpl getBundle()
        {
            return m_wiring.getBundle();
        }

        @Override
        protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
            Class clazz;

            // Make sure the class was not already loaded.
            Object lock = (isParallel()) ? m_classLocks : this;
            synchronized (lock)
            {
                clazz = findLoadedClass(name);
            }

            if (clazz == null)
            {
                try
                {
                    clazz = (Class) m_wiring.findClassOrResourceByDelegation(name, true);
                }
                catch (ResourceNotFoundException ex)
                {
                    // This should never happen since we are asking for a class,
                    // so just ignore it.
                }
                catch (ClassNotFoundException cnfe)
                {
                    ClassNotFoundException ex = cnfe;
                    if (m_logger.getLogLevel() >= Logger.LOG_DEBUG)
                    {
                        String msg = diagnoseClassLoadError(m_wiring.m_resolver, m_wiring.m_revision, name);
                        ex = (msg != null)
                            ? new ClassNotFoundException(msg, cnfe)
                            : ex;
                    }
                    throw ex;
                }
            }

            // Resolve the class and return it.
            if (resolve)
            {
                resolveClass(clazz);
            }
            return clazz;
        }

        @Override
        protected Class findClass(String name) throws ClassNotFoundException
        {
            Class clazz = null;

            // Do a quick check to try to avoid searching for classes on a
            // disposed class loader, which will avoid some odd exception.
            // This won't prevent all weird exception, since the wiring could
            // still get disposed of after this check, but it will prevent
            // some, perhaps.
            if (m_wiring.m_isDisposed)
            {
                throw new ClassNotFoundException(
                    "Unable to load class '"
                    + name
                    + "' because the bundle wiring for "
                    + m_wiring.m_revision.getSymbolicName()
                    + " is no longer valid.");
            }

            // Search for class in bundle revision.
            if (clazz == null)
            {
                String actual = name.replace('.', '/') + ".class";

                byte[] bytes = null;

                // Check the bundle class path.
                List<Content> contentPath = m_wiring.m_revision.getContentPath();
                Content content = null;
                for (int i = 0;
                    (bytes == null) &&
                    (i < contentPath.size()); i++)
                {
                    bytes = contentPath.get(i).getEntryAsBytes(actual);
                    content = contentPath.get(i);
                }

                if (bytes != null)
                {
                    // Get package name.
                    String pkgName = Util.getClassPackage(name);

                    // Get weaving hooks and invoke them to give them a
                    // chance to weave the class' byte code before we
                    // define it.
                    // NOTE: We don't try to dynamically track hook addition
                    // or removal, we just get a snapshot and leave any changes
                    // as a race condition, doing any necessary clean up in
                    // the error handling.
                    Felix felix = m_wiring.m_revision.getBundle().getFramework();

                    Set<ServiceReference<WeavingHook>> hooks =
                        felix.getHookRegistry().getHooks(WeavingHook.class);

                    Set<ServiceReference<WovenClassListener>> wovenClassListeners =
                        felix.getHookRegistry().getHooks(WovenClassListener.class);

                    WovenClassImpl wci = null;
                    if (!hooks.isEmpty())
                    {
                        // Create woven class to be used for hooks.
                        wci = new WovenClassImpl(name, m_wiring, bytes);
                        try
                        {
                            transformClass(felix, wci, hooks, wovenClassListeners,
                                    name, bytes);
                        }
                        catch (Error e)
                        {
                            // Mark the woven class as incomplete.
                            wci.complete(null, null, null);
                            wci.setState(WovenClass.TRANSFORMING_FAILED);
                            callWovenClassListeners(felix, wovenClassListeners, wci);
                            throw e;
                        }
                    }
                    // Before we actually attempt to define the class, grab
                    // the lock for this class loader and make sure than no
                    // other thread has defined this class in the meantime.
                    Object lock = (isParallel()) ? m_classLocks : this;
                    synchronized (lock)
                    {
                        Thread me = Thread.currentThread();
                        while (m_classLocks.containsKey(name) && (m_classLocks.get(name) != me))
                        {
                            try
                            {
                                lock.wait();
                            }
                            catch (InterruptedException e)
                            {
                                // TODO: WHAT TO DO HERE?
                                throw new RuntimeException(e);
                            }
                        }
                        // Lock released, try loading class.
                        clazz = findLoadedClass(name);
                        if (clazz == null)
                        {
                            // Not found, we should try load it.
                            m_classLocks.put(name, me);
                        }
                    }

                    try
                    {
                        clazz = defineClass(felix, wovenClassListeners, wci, name,
                                clazz, bytes, content, pkgName, lock);
                    }
                    catch (ClassFormatError e)
                    {
                        if(wci != null)
                        {
                            wci.setState(WovenClass.DEFINE_FAILED);
                            callWovenClassListeners(felix, wovenClassListeners, wci);
                        }
                        throw e;
                    }

                    // Perform deferred activation without holding the class loader lock,
                    // if the class we are returning is the instigating class.
                    List deferredList = (List) m_deferredActivation.get();
                    if ((deferredList != null)
                        && (deferredList.size() > 0)
                        && ((Object[]) deferredList.get(0))[0].equals(name))
                    {
                        // Null the deferred list.
                        m_deferredActivation.set(null);
                        while (!deferredList.isEmpty())
                        {
                            // Lazy bundles should be activated in the reverse order
                            // of when they were added to the deferred list, so grab
                            // them from the end of the deferred list.
                            Object[] lazy = (Object[]) deferredList.remove(deferredList.size() - 1);
                            try
                            {
                                felix.getFramework().activateBundle((BundleImpl) (lazy)[1], true);
                            }
                            catch (Throwable ex)
                            {
                                m_logger.log((BundleImpl) (lazy)[1],
                                    Logger.LOG_WARNING,
                                    "Unable to lazily start bundle.",
                                    ex);
                            }
                        }
                    }
                }
            }

            return clazz;
        }

        Class defineClass(Felix felix,
                Set<ServiceReference<WovenClassListener>> wovenClassListeners,
                WovenClassImpl wci, String name, Class clazz, byte[] bytes,
                Content content, String pkgName, Object lock)
                        throws ClassFormatError
        {

            try
            {
                if (clazz == null)
                {
                    // If we have a woven class then get the class bytes from
                    // it since they may have changed.
                    // NOTE: We are taking a snapshot of these values and
                    // are not preventing a malbehaving weaving hook from
                    // modifying them after the fact. The price of preventing
                    // this isn't worth it, since they can already wreck
                    // havoc via weaving anyway. However, we do pass the
                    // snapshot values into the woven class when we mark it
                    // as complete so that it will refect the actual values
                    // we used to define the class.
                    if (wci != null)
                    {
                        bytes = wci._getBytes();
                        List<String> wovenImports = wci.getDynamicImportsInternal();

                        // Try to add any woven dynamic imports, since they
                        // could potentially be needed when defining the class.
                        List<BundleRequirement> allWovenReqs =
                                new ArrayList<BundleRequirement>();
                        for (String s : wovenImports)
                        {
                            try
                            {
                                List<BundleRequirement> wovenReqs =
                              ManifestParser.parseDynamicImportHeader(
                                      m_logger, m_wiring.m_revision, s);
                              allWovenReqs.addAll(wovenReqs);
                            }
                            catch (BundleException ex)
                            {
                                // There should be no exception here
                                // since we checked syntax before adding
                                // dynamic import strings to list.
                            }
                        }
                        // Add the dynamic requirements.
                        if (!allWovenReqs.isEmpty())
                        {
                            // Check for duplicate woven imports.
                            // First grab existing woven imports, if any.
                            Set<String> filters = new HashSet<String>();
                            if (m_wiring.m_wovenReqs != null)
                            {
                                for (BundleRequirement req : m_wiring.m_wovenReqs)
                                {
                                    filters.add(
                                            ((BundleRequirementImpl) req)
                                            .getFilter().toString());
                                }
                            }
                            // Then check new woven imports for duplicates
                            // against existing and self.
                            int idx = allWovenReqs.size();
                            while (idx < allWovenReqs.size())
                            {
                                BundleRequirement wovenReq = allWovenReqs.get(idx);
                                String filter = ((BundleRequirementImpl)
                                        wovenReq).getFilter().toString();
                                if (!filters.contains(filter))
                                {
                                    filters.add(filter);
                                    idx++;
                                }
                                else
                                {
                                    allWovenReqs.remove(idx);
                                }
                            }
                            // Merge existing with new imports, if any.
                            if (!allWovenReqs.isEmpty())
                            {
                                if (m_wiring.m_wovenReqs != null)
                                {
                                    allWovenReqs.addAll(0, m_wiring.m_wovenReqs);
                                }
                                m_wiring.m_wovenReqs = allWovenReqs;
                            }
                        }
                    }

                    int activationPolicy =
                            getBundle().isDeclaredActivationPolicyUsed()
                            ? getBundle()
                                    .adapt(BundleRevisionImpl.class).getDeclaredActivationPolicy()
                                    : EAGER_ACTIVATION;

                    // If the revision is using deferred activation, then if
                    // we load this class from this revision we need to activate
                    // the bundle before returning the class. We will short
                    // circuit the trigger matching if the trigger is already
                    // tripped.
                    boolean isTriggerClass = m_isActivationTriggered
                            ? false : m_wiring.m_revision.isActivationTrigger(pkgName);
                    if (!m_isActivationTriggered
                            && isTriggerClass
                            && (activationPolicy == BundleRevisionImpl.LAZY_ACTIVATION)
                            && (getBundle().getState() == Bundle.STARTING))
                    {
                        List deferredList = (List) m_deferredActivation.get();
                        if (deferredList == null)
                        {
                            deferredList = new ArrayList();
                            m_deferredActivation.set(deferredList);
                        }
                        deferredList.add(new Object[] { name, getBundle() });
                    }
                    // We need to try to define a Package object for the class
                    // before we call defineClass() if we haven't already
                    // created it.
                    if (pkgName.length() > 0)
                    {
                        if (getPackage(pkgName) == null)
                        {
                            Object[] params = definePackage(pkgName);

                            // This is a harmless check-then-act situation,
                            // where threads might be racing to create different
                            // classes in the same package, so catch and ignore
                            // any IAEs that may occur.
                            try
                            {
                                definePackage(
                                        pkgName,
                                        (String) params[0],
                                        (String) params[1],
                                        (String) params[2],
                                        (String) params[3],
                                        (String) params[4],
                                        (String) params[5],
                                        null);
                            }
                            catch (IllegalArgumentException ex)
                            {
                                // Ignore.
                            }
                        }
                    }

                    // If we can load the class from a dex file do so
                    if (content instanceof JarContent)
                    {
                        try
                        {
                            clazz = getDexFileClass((JarContent) content, name, this);
                        }
                        catch (Exception ex)
                        {
                            // Looks like we can't
                        }
                    }

                    if (clazz == null)
                    {
                        // If we have a security context, then use it to
                        // define the class with it for security purposes,
                        // otherwise define the class without a protection domain.
                        if (m_wiring.m_revision.getProtectionDomain() != null)
                        {
                            clazz = defineClass(name, bytes, 0, bytes.length,
                                    m_wiring.m_revision.getProtectionDomain());
                        }
                        else
                        {
                            clazz = defineClass(name, bytes, 0, bytes.length);
                        }
                        if(wci != null)
                        {
                            wci.completeDefine(clazz);
                            wci.setState(WovenClass.DEFINED);
                            callWovenClassListeners(felix, wovenClassListeners, wci);
                        }
                    }

                    // At this point if we have a trigger class, then the deferred
                    // activation trigger has tripped.
                    if (!m_isActivationTriggered && isTriggerClass && (clazz != null))
                    {
                        m_isActivationTriggered = true;
                    }
                }
            }
            finally
            {
                synchronized (lock)
                {
                    m_classLocks.remove(name);
                    lock.notifyAll();
                }
            }
            return clazz;
        }

        void transformClass(Felix felix, WovenClassImpl wci,
                Set<ServiceReference<WeavingHook>> hooks,
                Set<ServiceReference<WovenClassListener>> wovenClassListeners,
                String name, byte[] bytes) throws Error {

            // Loop through hooks in service ranking order.
            for (ServiceReference<WeavingHook> sr : hooks)
            {
                // Only use the hook if it is not black listed.
                if (!felix.getHookRegistry().isHookBlackListed(sr))
                {
                    // Get the hook service object.
                    // Note that we don't use the bundle context
                    // to get the service object since that would
                    // perform sercurity checks.
                    WeavingHook wh = felix.getService(felix, sr, false);
                    if (wh != null)
                    {
                        try
                        {
                            BundleRevisionImpl.getSecureAction()
                                .invokeWeavingHook(wh, wci);
                        }
                        catch (Throwable th)
                        {
                            if (!(th instanceof WeavingException))
                            {
                                felix.getHookRegistry().blackListHook(sr);
                            }
                            felix.fireFrameworkEvent(
                                    FrameworkEvent.ERROR,
                                    sr.getBundle(),
                                    th);

                            // Throw class format exception per spec.
                            Error error = new ClassFormatError("Weaving hook failed.");
                            error.initCause(th);
                            throw error;
                        }
                        finally
                        {
                            felix.ungetService(felix, sr, null);
                        }
                    }
                }
            }
            wci.setState(WovenClass.TRANSFORMED);
            callWovenClassListeners(felix, wovenClassListeners, wci);
        }

        protected void callWovenClassListeners(Felix felix, Set<ServiceReference<WovenClassListener>> wovenClassListeners, WovenClass wovenClass)
        {
            if(wovenClassListeners != null)
            {
                for(ServiceReference<WovenClassListener> currentWovenClassListenerRef : wovenClassListeners)
                {
                    WovenClassListener currentWovenClassListner = felix.getService(felix, currentWovenClassListenerRef, false);
                    try
                    {
                        BundleRevisionImpl.getSecureAction().invokeWovenClassListener(currentWovenClassListner, wovenClass);
                    }
                    catch (Exception e)
                    {
                        m_logger.log(Logger.LOG_ERROR, "Woven Class Listner failed.", e);
                    }
                    finally
                    {
                        felix.ungetService(felix, currentWovenClassListenerRef, null);
                    }
                }
            }
        }

        private Object[] definePackage(String pkgName)
        {
            String spectitle = (String) m_wiring.m_revision.getHeaders().get("Specification-Title");
            String specversion = (String) m_wiring.m_revision.getHeaders().get("Specification-Version");
            String specvendor = (String) m_wiring.m_revision.getHeaders().get("Specification-Vendor");
            String impltitle = (String) m_wiring.m_revision.getHeaders().get("Implementation-Title");
            String implversion = (String) m_wiring.m_revision.getHeaders().get("Implementation-Version");
            String implvendor = (String) m_wiring.m_revision.getHeaders().get("Implementation-Vendor");
            if ((spectitle != null)
                || (specversion != null)
                || (specvendor != null)
                || (impltitle != null)
                || (implversion != null)
                || (implvendor != null))
            {
                return new Object[] {
                    spectitle, specversion, specvendor, impltitle, implversion, implvendor
                };
            }
            return new Object[] {null, null, null, null, null, null};
        }

        private Class getDexFileClass(JarContent content, String name, ClassLoader loader)
            throws Exception
        {
            if (m_jarContentToDexFile == null)
            {
                return null;
            }

            Object dexFile = null;

            if (!m_jarContentToDexFile.containsKey(content))
            {
                try
                {
                    if (m_dexFileClassLoadDex != null)
                    {
                        dexFile = m_dexFileClassLoadDex.invoke(null,
                            new Object[]{content.getFile().getAbsolutePath(),
                                content.getFile().getAbsolutePath() + ".dex", new Integer(0)});
                    }
                    else
                    {
                        dexFile = m_dexFileClassConstructor.newInstance(
                            new Object[] { content.getFile() });
                    }
                }
                finally
                {
                    m_jarContentToDexFile.put(content, dexFile);
                }
            }
            else
            {
                dexFile = m_jarContentToDexFile.get(content);
            }

            if (dexFile != null)
            {
                return (Class) m_dexFileClassLoadClass.invoke(dexFile,
                    new Object[] { name.replace('.','/'), loader });
            }
            return null;
        }

        @Override
        public URL getResource(String name)
        {
            URL url = m_wiring.getResourceByDelegation(name);
            if (m_wiring.m_useLocalURLs)
            {
                url = convertToLocalUrl(url);
            }
            return url;
        }

        @Override
        protected URL findResource(String name)
        {
            return m_wiring.m_revision.getResourceLocal(name);
        }

        // The findResources() method should only look at the revision itself, but
        // instead it tries to delegate because in Java version prior to 1.5 the
        // getResources() method was final and could not be overridden. We should
        // override getResources() like getResource() to make it delegate, but we
        // can't. As a workaround, we make findResources() delegate instead.
        @Override
        protected Enumeration findResources(String name)
        {
            Enumeration urls = m_wiring.getResourcesByDelegation(name);
            if (m_wiring.m_useLocalURLs)
            {
                urls = new ToLocalUrlEnumeration(urls);
            }
            return urls;
        }

        @Override
        protected String findLibrary(String name)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            String result = null;
            // CONCURRENCY: In the long run, we might want to break this
            // sync block in two to avoid manipulating the cache while
            // holding the lock, but for now we will do it the simple way.
            synchronized (this)
            {
                // Check to make sure we haven't already found this library.
                for (int i = 0; (result == null) && (i < m_cachedLibs.length); i++)
                {
                    if (m_cachedLibs[i][LIBNAME_IDX].equals(name))
                    {
                        result = (String) m_cachedLibs[i][LIBPATH_IDX];
                    }
                }

                // If we don't have a cached result, see if we have a matching
                // native library.
                if (result == null)
                {
                    List<NativeLibrary> libs = m_wiring.getNativeLibraries();
                    for (int libIdx = 0; (libs != null) && (libIdx < libs.size()); libIdx++)
                    {
                        if (libs.get(libIdx).match(m_wiring.m_configMap, name))
                        {
                            // Search bundle content first for native library.
                            result = m_wiring.m_revision.getContent().getEntryAsNativeLibrary(
                                libs.get(libIdx).getEntryName());
                            // If not found, then search fragments in order.
                            for (int i = 0;
                                (result == null) && (m_wiring.m_fragmentContents != null)
                                    && (i < m_wiring.m_fragmentContents.size());
                                i++)
                            {
                                result = m_wiring.m_fragmentContents.get(i).getEntryAsNativeLibrary(
                                    libs.get(libIdx).getEntryName());
                            }
                        }
                    }

                    // Remember the result for future requests.
                    if (result != null)
                    {
                        Object[][] tmp = new Object[m_cachedLibs.length + 1][];
                        System.arraycopy(m_cachedLibs, 0, tmp, 0, m_cachedLibs.length);
                        tmp[m_cachedLibs.length] = new Object[] { name, result };
                        m_cachedLibs = tmp;
                    }
                }
            }

            return result;
        }

        @Override
        public String toString()
        {
            return m_wiring.toString();
        }
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

    private static class ResourceSource implements Comparable<ResourceSource>
    {
        public final String m_resource;
        public final BundleRevision m_revision;

        public ResourceSource(String resource, BundleRevision revision)
        {
            m_resource = resource;
            m_revision = revision;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof ResourceSource)
            {
                return m_resource.equals(((ResourceSource) o).m_resource);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return m_resource.hashCode();
        }

        @Override
        public int compareTo(ResourceSource t)
        {
            return m_resource.compareTo(t.m_resource);
        }

        @Override
        public String toString()
        {
            return m_resource
                + " -> "
                + m_revision.getSymbolicName()
                + " [" + m_revision + "]";
        }
    }

    private static String diagnoseClassLoadError(
        StatefulResolver resolver, BundleRevision revision, String name)
    {
        // We will try to do some diagnostics here to help the developer
        // deal with this exception.

        // Get package name.
        String pkgName = Util.getClassPackage(name);
        if (pkgName.length() == 0)
        {
            return null;
        }

        // First, get the bundle string of the revision doing the class loader.
        String importer = revision.getBundle().toString();

        // Next, check to see if the revision imports the package.
        List<BundleWire> wires = (revision.getWiring() == null)
            ? null : revision.getWiring().getProvidedWires(null);
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).getCapability().getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE) &&
                wires.get(i).getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).equals(pkgName))
            {
                String exporter = wires.get(i).getProviderWiring().getBundle().toString();

                StringBuffer sb = new StringBuffer("*** Package '");
                sb.append(pkgName);
                sb.append("' is imported by bundle ");
                sb.append(importer);
                sb.append(" from bundle ");
                sb.append(exporter);
                sb.append(", but the exported package from bundle ");
                sb.append(exporter);
                sb.append(" does not contain the requested class '");
                sb.append(name);
                sb.append("'. Please verify that the class name is correct in the importing bundle ");
                sb.append(importer);
                sb.append(" and/or that the exported package is correctly bundled in ");
                sb.append(exporter);
                sb.append(". ***");

                return sb.toString();
            }
        }

        // Next, check to see if the package was optionally imported and
        // whether or not there is an exporter available.
        List<BundleRequirement> reqs = revision.getWiring().getRequirements(null);
/*
* TODO: RB - Fix diagnostic message for optional imports.
        for (int i = 0; (reqs != null) && (i < reqs.length); i++)
        {
            if (reqs[i].getName().equals(pkgName) && reqs[i].isOptional())
            {
                // Try to see if there is an exporter available.
                IModule[] exporters = getResolvedExporters(reqs[i], true);
                exporters = (exporters.length == 0)
                    ? getUnresolvedExporters(reqs[i], true) : exporters;

                // An exporter might be available, but it may have attributes
                // that do not match the importer's required attributes, so
                // check that case by simply looking for an exporter of the
                // desired package without any attributes.
                if (exporters.length == 0)
                {
                    IRequirement pkgReq = new Requirement(
                        ICapability.PACKAGE_NAMESPACE, "(package=" + pkgName + ")");
                    exporters = getResolvedExporters(pkgReq, true);
                    exporters = (exporters.length == 0)
                        ? getUnresolvedExporters(pkgReq, true) : exporters;
                }

                long expId = (exporters.length == 0)
                    ? -1 : Util.getBundleIdFromModuleId(exporters[0].getId());

                StringBuffer sb = new StringBuffer("*** Class '");
                sb.append(name);
                sb.append("' was not found, but this is likely normal since package '");
                sb.append(pkgName);
                sb.append("' is optionally imported by bundle ");
                sb.append(impId);
                sb.append(".");
                if (exporters.length > 0)
                {
                    sb.append(" However, bundle ");
                    sb.append(expId);
                    if (reqs[i].isSatisfied(
                        Util.getExportPackage(exporters[0], reqs[i].getName())))
                    {
                        sb.append(" does export this package. Bundle ");
                        sb.append(expId);
                        sb.append(" must be installed before bundle ");
                        sb.append(impId);
                        sb.append(" is resolved or else the optional import will be ignored.");
                    }
                    else
                    {
                        sb.append(" does export this package with attributes that do not match.");
                    }
                }
                sb.append(" ***");

                return sb.toString();
            }
        }
*/
        // Next, check to see if the package is dynamically imported by the revision.
        if (resolver.isAllowedDynamicImport(revision, pkgName))
        {
            // Try to see if there is an exporter available.
            Map<String, String> dirs = Collections.EMPTY_MAP;
            Map<String, Object> attrs = Collections.singletonMap(
                BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
            BundleRequirementImpl req = new BundleRequirementImpl(
                revision, BundleRevision.PACKAGE_NAMESPACE, dirs, attrs);
            List<BundleCapability> exporters = resolver.findProviders(req, false);

            BundleRevision provider = null;
            try
            {
                provider = resolver.resolve(revision, pkgName);
            }
            catch (Exception ex)
            {
                provider = null;
            }

            String exporter = (exporters.isEmpty())
                ? null : exporters.iterator().next().toString();

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found, but this is likely normal since package '");
            sb.append(pkgName);
            sb.append("' is dynamically imported by bundle ");
            sb.append(importer);
            sb.append(".");
            if ((exporters.size() > 0) && (provider == null))
            {
                sb.append(" However, bundle ");
                sb.append(exporter);
                sb.append(" does export this package with attributes that do not match.");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, check to see if there are any exporters for the package at all.
        Map<String, String> dirs = Collections.EMPTY_MAP;
        Map<String, Object> attrs = Collections.singletonMap(
            BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            revision, BundleRevision.PACKAGE_NAMESPACE, dirs, attrs);
        List<BundleCapability> exports = resolver.findProviders(req, false);
        if (exports.size() > 0)
        {
            boolean classpath = false;
            try
            {
                BundleRevisionImpl.getSecureAction()
                    .getClassLoader(BundleClassLoader.class).loadClass(name);
                classpath = true;
            }
            catch (NoClassDefFoundError err)
            {
                // Ignore
            }
            catch (Exception ex)
            {
                // Ignore
            }

            String exporter = exports.iterator().next().toString();

            StringBuffer sb = new StringBuffer("*** Class '");
            sb.append(name);
            sb.append("' was not found because bundle ");
            sb.append(importer);
            sb.append(" does not import '");
            sb.append(pkgName);
            sb.append("' even though bundle ");
            sb.append(exporter);
            sb.append(" does export it.");
            if (classpath)
            {
                sb.append(" Additionally, the class is also available from the system class loader. There are two fixes: 1) Add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(importer);
                sb.append("; imports are necessary for each class directly touched by bundle code or indirectly touched, such as super classes if their methods are used. ");
                sb.append("2) Add package '");
                sb.append(pkgName);
                sb.append("' to the '");
                sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
                sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            }
            else
            {
                sb.append(" To resolve this issue, add an import for '");
                sb.append(pkgName);
                sb.append("' to bundle ");
                sb.append(importer);
                sb.append(".");
            }
            sb.append(" ***");

            return sb.toString();
        }

        // Next, try to see if the class is available from the system
        // class loader.
        try
        {
            BundleRevisionImpl.getSecureAction()
                .getClassLoader(BundleClassLoader.class).loadClass(name);

            StringBuffer sb = new StringBuffer("*** Package '");
            sb.append(pkgName);
            sb.append("' is not imported by bundle ");
            sb.append(importer);
            sb.append(", nor is there any bundle that exports package '");
            sb.append(pkgName);
            sb.append("'. However, the class '");
            sb.append(name);
            sb.append("' is available from the system class loader. There are two fixes: 1) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
            sb.append("' property and modify bundle ");
            sb.append(importer);
            sb.append(" to import this package; this causes the system bundle to export class path packages. 2) Add package '");
            sb.append(pkgName);
            sb.append("' to the '");
            sb.append(Constants.FRAMEWORK_BOOTDELEGATION);
            sb.append("' property; a library or VM bug can cause classes to be loaded by the wrong class loader. The first approach is preferable for preserving modularity.");
            sb.append(" ***");

            return sb.toString();
        }
        catch (Exception ex2)
        {
        }

        // Finally, if there are no imports or exports for the package
        // and it is not available on the system class path, simply
        // log a message saying so.
        StringBuffer sb = new StringBuffer("*** Class '");
        sb.append(name);
        sb.append("' was not found. Bundle ");
        sb.append(importer);
        sb.append(" does not import package '");
        sb.append(pkgName);
        sb.append("', nor is the package exported by any other bundle or available from the system class loader.");
        sb.append(" ***");

        return sb.toString();
    }
}
