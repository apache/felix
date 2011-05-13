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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.packageadmin.RequiredBundle;

class RequiredBundleImpl implements RequiredBundle
{
    private final Felix m_felix;
    private final BundleImpl m_bundle;
    private volatile String m_toString = null;
    private volatile String m_versionString = null;

    public RequiredBundleImpl(Felix felix, BundleImpl bundle)
    {
        m_felix = felix;
        m_bundle = bundle;
    }

    public String getSymbolicName()
    {
        return m_bundle.getSymbolicName();
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Bundle[] getRequiringBundles()
    {
        // Spec says to return null for stale bundles.
        if (m_bundle.isStale())
        {
            return null;
        }

        // We need to find all revisions that require any of the revisions
        // associated with this bundle and save the associated bundle
        // of the dependent revisions.
        Set bundleSet = new HashSet();
        // Loop through all of this bundle's revisions.
        List<BundleRevision> revisions = m_bundle.getRevisions();
        for (int modIdx = 0; (revisions != null) && (modIdx < revisions.size()); modIdx++)
        {
            // For each of this bundle's revisions, loop through all of the
            // revisions that require it and add them to the dependents list.
            List<BundleRevision> dependents =
                ((BundleRevisionImpl) revisions.get(modIdx)).getDependentRequirers();
            for (int depIdx = 0; (dependents != null) && (depIdx < dependents.size()); depIdx++)
            {
                if (dependents.get(depIdx).getBundle() != null)
                {
                    bundleSet.add(dependents.get(depIdx).getBundle());
                }
            }
        }
        // Convert to an array.
        return (Bundle[]) bundleSet.toArray(new Bundle[bundleSet.size()]);
    }

    public Version getVersion()
    {
        return m_bundle.getVersion();
    }

    public boolean isRemovalPending()
    {
        return m_bundle.isRemovalPending();
    }

    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_bundle.getSymbolicName()
                + "; version=" + m_bundle.getVersion();
        }
        return m_toString;
    }
}