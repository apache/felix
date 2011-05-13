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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Resolver;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

class ResolverStateImpl implements Resolver.ResolverState
{
    private final Logger m_logger;
    // Set of all revisions.
    private final Set<BundleRevision> m_revisions;
    // Set of all fragments.
    private final Set<BundleRevision> m_fragments;
    // Capability sets.
    private final Map<String, CapabilitySet> m_capSets;
    // Execution environment.
    private final String m_fwkExecEnvStr;
    // Parsed framework environments
    private final Set<String> m_fwkExecEnvSet;

    ResolverStateImpl(Logger logger, String fwkExecEnvStr)
    {
        m_logger = logger;
        m_revisions = new HashSet<BundleRevision>();
        m_fragments = new HashSet<BundleRevision>();
        m_capSets = new HashMap<String, CapabilitySet>();

        m_fwkExecEnvStr = (fwkExecEnvStr != null) ? fwkExecEnvStr.trim() : null;
        m_fwkExecEnvSet = parseExecutionEnvironments(fwkExecEnvStr);

        List<String> indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(BundleCapabilityImpl.BUNDLE_NAMESPACE, new CapabilitySet(indices, true));

        indices = new ArrayList<String>();
        indices.add(BundleCapabilityImpl.PACKAGE_ATTR);
        m_capSets.put(BundleCapabilityImpl.PACKAGE_NAMESPACE, new CapabilitySet(indices, true));

        indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(BundleCapabilityImpl.HOST_NAMESPACE,  new CapabilitySet(indices, true));
    }

    synchronized void addRevision(BundleRevision br)
    {
        m_revisions.add(br);
        List<BundleCapability> caps = (br.getWiring() == null)
            ? br.getDeclaredCapabilities(null)
            : br.getWiring().getCapabilities(null);
        if (caps != null)
        {
            for (BundleCapability cap : caps)
            {
                CapabilitySet capSet = m_capSets.get(cap.getNamespace());
                if (capSet == null)
                {
                    capSet = new CapabilitySet(null, true);
                    m_capSets.put(cap.getNamespace(), capSet);
                }
                capSet.addCapability(cap);
            }
        }

        if (Util.isFragment(br))
        {
            m_fragments.add(br);
        }
    }

    synchronized void removeRevision(BundleRevision br)
    {
        m_revisions.remove(br);
        List<BundleCapability> caps = (br.getWiring() == null)
            ? br.getDeclaredCapabilities(null)
            : br.getWiring().getCapabilities(null);
        if (caps != null)
        {
            for (BundleCapability cap : caps)
            {
                CapabilitySet capSet = m_capSets.get(cap.getNamespace());
                if (capSet != null)
                {
                    capSet.removeCapability(cap);
                }
            }
        }

        if (Util.isFragment(br))
        {
            m_fragments.remove(br);
        }
    }

    synchronized Set<BundleRevision> getFragments()
    {
        return new HashSet(m_fragments);
    }

// TODO: OSGi R4.3 - This will need to be changed once BundleWiring.getCapabilities()
//       is correctly implemented, since they already has to remove substituted caps.
    synchronized void removeSubstitutedCapabilities(BundleRevision br)
    {
        if (br.getWiring() != null)
        {
            // Loop through the revision's package wires and determine if any
            // of them overlap any of the packages exported by the revision.
            // If so, then the framework must have chosen to have the revision
            // import rather than export the package, so we need to remove the
            // corresponding package capability from the package capability set.
            for (BundleWire w : ((BundleRevisionImpl) br).getWires())
            {
                if (w.getCapability().getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    for (BundleCapability cap : br.getWiring().getCapabilities(null))
                    {
                        if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                            && w.getCapability().getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)
                                .equals(cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)))
                        {
                            m_capSets.get(BundleCapabilityImpl.PACKAGE_NAMESPACE).removeCapability(cap);
                            break;
                        }
                    }
                }
            }
        }
    }

    //
    // ResolverState methods.
    //

    public synchronized SortedSet<BundleCapability> getCandidates(
        BundleRequirementImpl req, boolean obeyMandatory)
    {
        BundleRevisionImpl reqRevision = (BundleRevisionImpl) req.getRevision();
        SortedSet<BundleCapability> result =
            new TreeSet<BundleCapability>(new CandidateComparator());

        CapabilitySet capSet = m_capSets.get(req.getNamespace());
        if (capSet != null)
        {
            Set<BundleCapability> matches = capSet.match(req.getFilter(), obeyMandatory);
            for (BundleCapability cap : matches)
            {
                if (System.getSecurityManager() != null)
                {
                    if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE) && (
                        !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getSecurityContext()).impliesDirect(
                            new PackagePermission((String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR),
                            PackagePermission.EXPORTONLY)) ||
                            !((reqRevision == null) ||
                                ((BundleProtectionDomain) reqRevision.getSecurityContext()).impliesDirect(
                                    new PackagePermission((String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR),
                                    cap.getRevision().getBundle(),PackagePermission.IMPORT))
                            )))
                    {
                        if (reqRevision != cap.getRevision())
                        {
                            continue;
                        }
                    }
                    else if (req.getNamespace().equals(BundleCapabilityImpl.BUNDLE_NAMESPACE) && (
                        !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getSecurityContext()).impliesDirect(
                            new BundlePermission(cap.getRevision().getSymbolicName(), BundlePermission.PROVIDE)) ||
                            !((reqRevision == null) ||
                                ((BundleProtectionDomain) reqRevision.getSecurityContext()).impliesDirect(
                                    new BundlePermission(reqRevision.getSymbolicName(), BundlePermission.REQUIRE))
                            )))
                    {
                        continue;
                    }
                    else if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE) &&
                        (!((BundleProtectionDomain) reqRevision.getSecurityContext())
                            .impliesDirect(new BundlePermission(
                                reqRevision.getSymbolicName(),
                                BundlePermission.FRAGMENT))
                        || !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getSecurityContext())
                            .impliesDirect(new BundlePermission(
                                cap.getRevision().getSymbolicName(),
                                BundlePermission.HOST))))
                    {
                        continue;
                    }
                }

                if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE)
                    && (cap.getRevision().getWiring() != null))
                {
                    continue;
                }

                result.add(cap);
            }
        }

        return result;
    }

    public void checkExecutionEnvironment(BundleRevision revision) throws ResolveException
    {
        String bundleExecEnvStr = (String)
            ((BundleRevisionImpl) revision).getHeaders().get(
                Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (bundleExecEnvStr != null)
        {
            bundleExecEnvStr = bundleExecEnvStr.trim();

            // If the bundle has specified an execution environment and the
            // framework has an execution environment specified, then we must
            // check for a match.
            if (!bundleExecEnvStr.equals("")
                && (m_fwkExecEnvStr != null)
                && (m_fwkExecEnvStr.length() > 0))
            {
                StringTokenizer tokens = new StringTokenizer(bundleExecEnvStr, ",");
                boolean found = false;
                while (tokens.hasMoreTokens() && !found)
                {
                    if (m_fwkExecEnvSet.contains(tokens.nextToken().trim()))
                    {
                        found = true;
                    }
                }
                if (!found)
                {
                    throw new ResolveException(
                        "Execution environment not supported: "
                        + bundleExecEnvStr, revision, null);
                }
            }
        }
    }

    public void checkNativeLibraries(BundleRevision revision) throws ResolveException
    {
        // Next, try to resolve any native code, since the revision is
        // not resolvable if its native code cannot be loaded.
        List<R4Library> libs = ((BundleRevisionImpl) revision).getNativeLibraries();
        if (libs != null)
        {
            String msg = null;
            // Verify that all native libraries exist in advance; this will
            // throw an exception if the native library does not exist.
            for (int libIdx = 0; (msg == null) && (libIdx < libs.size()); libIdx++)
            {
                String entryName = libs.get(libIdx).getEntryName();
                if (entryName != null)
                {
                    if (!((BundleRevisionImpl) revision).getContent().hasEntry(entryName))
                    {
                        msg = "Native library does not exist: " + entryName;
                    }
                }
            }
            // If we have a zero-length native library array, then
            // this means no native library class could be selected
            // so we should fail to resolve.
            if (libs.isEmpty())
            {
                msg = "No matching native libraries found.";
            }
            if (msg != null)
            {
                throw new ResolveException(msg, revision, null);
            }
        }
    }

    //
    // Utility methods.
    //

    /**
     * Updates the framework wide execution environment string and a cached Set of
     * execution environment tokens from the comma delimited list specified by the
     * system variable 'org.osgi.framework.executionenvironment'.
     * @param fwkExecEnvStr Comma delimited string of provided execution environments
     * @return the parsed set of execution environments
    **/
    private static Set<String> parseExecutionEnvironments(String fwkExecEnvStr)
    {
        Set<String> newSet = new HashSet<String>();
        if (fwkExecEnvStr != null)
        {
            StringTokenizer tokens = new StringTokenizer(fwkExecEnvStr, ",");
            while (tokens.hasMoreTokens())
            {
                newSet.add(tokens.nextToken().trim());
            }
        }
        return newSet;
    }
}