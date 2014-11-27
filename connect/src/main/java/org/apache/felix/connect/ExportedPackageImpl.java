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

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.service.packageadmin.ExportedPackage;

class ExportedPackageImpl implements ExportedPackage
{
    private final BundleCapability m_export;

    public ExportedPackageImpl(BundleCapability export)
    {
        m_export = export;
    }

    public Bundle getExportingBundle()
    {
        return m_export.getRevision().getBundle();
    }

    public Bundle[] getImportingBundles()
    {
        // Create set for storing importing bundles.
        Set<Bundle> result = new HashSet<Bundle>();
        // Get all importers and requirers for all revisions of the bundle.
        // The spec says that require-bundle should be returned with importers.
        for (BundleWire wire : m_export.getRevision().getWiring().getProvidedWires(null))
        {
            if (wire.getCapability() == m_export
                    || BundleNamespace.BUNDLE_NAMESPACE.equals(wire.getCapability().getNamespace()))
            {
                result.add( wire.getRequirer().getBundle() );
            }
        }
        // Return the results.
        return result.toArray(new Bundle[result.size()]);
    }

    public String getName()
    {
        return (String) m_export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
    }

    public String getSpecificationVersion()
    {
        return getVersion().toString();
    }

    public Version getVersion()
    {
        return m_export.getAttributes().containsKey(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE)
                ? (Version) m_export.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE)
                : Version.emptyVersion;
    }

    public boolean isRemovalPending()
    {
        return false;
    }

    public String toString()
    {
        return getName() + "; version=" + getVersion();
    }
}