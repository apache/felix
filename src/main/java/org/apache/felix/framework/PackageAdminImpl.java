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

import java.util.*;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;


public class PackageAdminImpl implements PackageAdmin
{
    private static final Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2)
        {
            // Reverse arguments to sort in descending order.
            return ((ExportedPackage) o2).getVersion().compareTo(
                ((ExportedPackage) o1).getVersion());
        }
    };

    private Felix m_felix = null;

    PackageAdminImpl(Felix felix)
    {
        m_felix = felix;
    }

    /**
     * Returns the bundle associated with this class if the class was
     * loaded from a bundle, otherwise returns null.
     *
     * @param clazz the class for which to determine its associated bundle.
     * @return the bundle associated with the specified class, otherwise null.
    **/
    public Bundle getBundle(Class clazz)
    {
        return m_felix.getBundle(clazz);
    }

    /**
     * Returns all bundles that have a specified symbolic name and whose
     * version is in the specified version range. If no version range is
     * specified, then all bundles with the specified symbolic name are
     * returned. The array is sorted in descending version order.
     *
     * @param symbolicName the target symbolic name.
     * @param versionRange the target version range.
     * @return an array of matching bundles sorted in descending version order.
    **/
    public Bundle[] getBundles(String symbolicName, String versionRange)
    {
        VersionRange vr = (versionRange == null) ? null : VersionRange.parse(versionRange);
        Bundle[] bundles = m_felix.getBundles();
        List list = new ArrayList();
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            String sym = bundles[i].getSymbolicName();
            if ((sym != null) && sym.equals(symbolicName))
            {
                Version v = bundles[i].adapt(BundleRevision.class).getVersion();
                if ((vr == null) || vr.isInRange(v))
                {
                    list.add(bundles[i]);
                }
            }
        }
        if (list.isEmpty())
        {
            return null;
        }
        bundles = (Bundle[]) list.toArray(new Bundle[list.size()]);
        Arrays.sort(bundles,new Comparator() {
            public int compare(Object o1, Object o2)
            {
                Version v1 = ((Bundle) o1).adapt(BundleRevision.class).getVersion();
                Version v2 = ((Bundle) o2).adapt(BundleRevision.class).getVersion();
                // Compare in reverse order to get descending sort.
                return v2.compareTo(v1);
            }
        });
        return bundles;
    }

    public int getBundleType(Bundle bundle)
    {
        Map headerMap = ((BundleRevisionImpl)
            bundle.adapt(BundleRevisionImpl.class)).getHeaders();
        if (headerMap.containsKey(Constants.FRAGMENT_HOST))
        {
            return PackageAdmin.BUNDLE_TYPE_FRAGMENT;
        }
        return 0;
    }

    /**
     * Returns the exported package associated with the specified
     * package name. If there are more than one version of the package
     * being exported, then the highest version is returned.
     *
     * @param name the name of the exported package to find.
     * @return the exported package or null if no matching package was found.
    **/
    public ExportedPackage getExportedPackage(String name)
    {
        // Get all versions of the exported package.
        ExportedPackage[] pkgs = m_felix.getExportedPackages(name);
        // If there are no versions exported, then return null.
        if ((pkgs == null) || (pkgs.length == 0))
        {
            return null;
        }
        // Sort the exported versions.
        Arrays.sort(pkgs, COMPARATOR);
        // Return the highest version.
        return pkgs[0];
    }

    public ExportedPackage[] getExportedPackages(String name)
    {
        ExportedPackage[] pkgs = m_felix.getExportedPackages(name);
        return ((pkgs == null) || pkgs.length == 0) ? null : pkgs;
    }

    /**
     * Returns the packages exported by the specified bundle.
     *
     * @param bundle the bundle whose exported packages are to be returned.
     * @return an array of packages exported by the bundle or null if the
     *         bundle does not export any packages.
    **/
    public ExportedPackage[] getExportedPackages(Bundle bundle)
    {
        ExportedPackage[] pkgs = m_felix.getExportedPackages(bundle);
        return ((pkgs == null) || pkgs.length == 0) ? null : pkgs;
    }

    public Bundle[] getFragments(Bundle bundle)
    {
        // If the bundle is not a fragment, then return its fragments.
        if ((getBundleType(bundle) & BUNDLE_TYPE_FRAGMENT) == 0)
        {
            List<Bundle> list = new ArrayList<Bundle>();
            // Iterate through revisions
            for (BundleRevision revision : bundle.adapt(BundleRevisions.class).getRevisions())
            {
                // Get attached fragments.
                if (revision.getWiring() != null)
                {
                    List<BundleRevision> fragments =
                        Util.getFragments(revision.getWiring());
                    for (int i = 0; i < fragments.size(); i++)
                    {
                        Bundle b = fragments.get(i).getBundle();
                        if (b != null)
                        {
                            list.add(b);
                        }
                    }
                }
            }
            // Convert list to an array.
            return (list.isEmpty())
                ? null
                : (Bundle[]) list.toArray(new Bundle[list.size()]);
        }
        return null;
    }

    public Bundle[] getHosts(Bundle bundle)
    {
        // If the bundle is a fragment, return its hosts
        if ((getBundleType(bundle) & BUNDLE_TYPE_FRAGMENT) != 0)
        {
            List<Bundle> list = new ArrayList<Bundle>();
            // Iterate through revisions
            for (BundleRevision revision : bundle.adapt(BundleRevisions.class).getRevisions())
            {
                // Get hosts
                if (revision.getWiring() != null)
                {
                    List<BundleWire> hostWires = revision.getWiring().getRequiredWires(null);
                    for (int i = 0; (hostWires != null) && (i < hostWires.size()); i++)
                    {
                        Bundle b = hostWires.get(i).getProviderWiring().getBundle();
                        if (b != null)
                        {
                            list.add(b);
                        }
                    }
                }
            }
            // Convert list to an array.
            return (list.isEmpty())
                ? null
                : (Bundle[]) list.toArray(new Bundle[list.size()]);
        }
        return null;
    }

    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        List list = new ArrayList();
        for (Bundle bundle : m_felix.getBundles())
        {
            if ((symbolicName == null)
                || (symbolicName.equals(bundle.getSymbolicName())))
            {
                list.add(new RequiredBundleImpl(m_felix, (BundleImpl) bundle));
            }
        }
        return (list.isEmpty())
            ? null
            : (RequiredBundle[]) list.toArray(new RequiredBundle[list.size()]);
    }

    /**
     * The OSGi specification states that refreshing packages is
     * asynchronous; this method simply notifies the package admin
     * thread to do a refresh.
     * @param bundles array of bundles to refresh or <tt>null</tt> to refresh
     *                any bundles in need of refreshing.
    **/
    public void refreshPackages(Bundle[] bundles)
        throws SecurityException
    {
        List<Bundle> list = (bundles == null)
            ? null
            : Arrays.asList(bundles);
        m_felix.adapt(FrameworkWiring.class).refreshBundles(list);
    }

    public boolean resolveBundles(Bundle[] bundles)
    {
        List<Bundle> list = (bundles == null)
            ? null
            : Arrays.asList(bundles);
        return m_felix.adapt(FrameworkWiring.class).resolveBundles(list);
    }
}