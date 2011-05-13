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
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Resolver;
import org.apache.felix.framework.resolver.Wire;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;

class ResolverStateImpl implements Resolver.ResolverState
{
    private final Logger m_logger;
    // Set of all modules.
    private final Set<Module> m_modules;
    // Set of all fragments.
    private final Set<Module> m_fragments;
    // Capability sets.
    private final Map<String, CapabilitySet> m_capSets;
    // Execution environment.
    private final String m_fwkExecEnvStr;
    // Parsed framework environments
    private final Set<String> m_fwkExecEnvSet;

    ResolverStateImpl(Logger logger, String fwkExecEnvStr)
    {
        m_logger = logger;
        m_modules = new HashSet<Module>();
        m_fragments = new HashSet<Module>();
        m_capSets = new HashMap<String, CapabilitySet>();

        m_fwkExecEnvStr = (fwkExecEnvStr != null) ? fwkExecEnvStr.trim() : null;
        m_fwkExecEnvSet = parseExecutionEnvironments(fwkExecEnvStr);

        List<String> indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(BundleCapabilityImpl.MODULE_NAMESPACE, new CapabilitySet(indices, true));

        indices = new ArrayList<String>();
        indices.add(BundleCapabilityImpl.PACKAGE_ATTR);
        m_capSets.put(BundleCapabilityImpl.PACKAGE_NAMESPACE, new CapabilitySet(indices, true));

        indices = new ArrayList<String>();
        indices.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
        m_capSets.put(BundleCapabilityImpl.HOST_NAMESPACE,  new CapabilitySet(indices, true));
    }

    synchronized void addModule(Module m)
    {
        m_modules.add(m);
        List<BundleCapabilityImpl> caps = m.getCapabilities();
        if (caps != null)
        {
            for (BundleCapabilityImpl cap : caps)
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

        if (Util.isFragment(m))
        {
            m_fragments.add(m);
        }
    }

    synchronized void removeModule(Module m)
    {
        m_modules.remove(m);
        List<BundleCapabilityImpl> caps = m.getCapabilities();
        if (caps != null)
        {
            for (BundleCapabilityImpl cap : caps)
            {
                CapabilitySet capSet = m_capSets.get(cap.getNamespace());
                if (capSet != null)
                {
                    capSet.removeCapability(cap);
                }
            }
        }

        if (Util.isFragment(m))
        {
            m_fragments.remove(m);
        }
    }

    synchronized Set<Module> getFragments()
    {
        return new HashSet(m_fragments);
    }

    synchronized void removeSubstitutedCapabilities(Module module)
    {
        if (module.isResolved())
        {
            // Loop through the module's package wires and determine if any
            // of them overlap any of the packages exported by the module.
            // If so, then the framework must have chosen to have the module
            // import rather than export the package, so we need to remove the
            // corresponding package capability from the package capability set.
            List<Wire> wires = module.getWires();
            List<BundleCapabilityImpl> caps = module.getCapabilities();
            for (int wireIdx = 0; (wires != null) && (wireIdx < wires.size()); wireIdx++)
            {
                Wire wire = wires.get(wireIdx);
                if (wire.getCapability().getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    for (int capIdx = 0;
                        (caps != null) && (capIdx < caps.size());
                        capIdx++)
                    {
                        if (caps.get(capIdx).getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                            && wire.getCapability().getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)
                                .equals(caps.get(capIdx).getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)))
                        {
                            m_capSets.get(BundleCapabilityImpl.PACKAGE_NAMESPACE).removeCapability(caps.get(capIdx));
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

    public synchronized SortedSet<BundleCapabilityImpl> getCandidates(
        BundleRequirementImpl req, boolean obeyMandatory)
    {
        Module module = req.getModule();
        SortedSet<BundleCapabilityImpl> result = new TreeSet<BundleCapabilityImpl>(new CandidateComparator());

        CapabilitySet capSet = m_capSets.get(req.getNamespace());
        if (capSet != null)
        {
            Set<BundleCapabilityImpl> matches = capSet.match(req.getFilter(), obeyMandatory);
            for (BundleCapabilityImpl cap : matches)
            {
                if (System.getSecurityManager() != null)
                {
                    if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE) && (
                        !((BundleProtectionDomain) cap.getModule().getSecurityContext()).impliesDirect(
                            new PackagePermission((String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR),
                            PackagePermission.EXPORTONLY)) ||
                            !((module == null) ||
                                ((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                    new PackagePermission((String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR),
                                    cap.getModule().getBundle(),PackagePermission.IMPORT))
                            )))
                    {
                        if (module != cap.getModule())
                        {
                            continue;
                        }
                    }
                    else if (req.getNamespace().equals(BundleCapabilityImpl.MODULE_NAMESPACE) && (
                        !((BundleProtectionDomain) cap.getModule().getSecurityContext()).impliesDirect(
                            new BundlePermission(cap.getModule().getSymbolicName(), BundlePermission.PROVIDE)) ||
                            !((module == null) ||
                                ((BundleProtectionDomain) module.getSecurityContext()).impliesDirect(
                                    new BundlePermission(module.getSymbolicName(), BundlePermission.REQUIRE))
                            )))
                    {
                        continue;
                    }
                    else if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE) &&
                        (!((BundleProtectionDomain) req.getModule().getSecurityContext())
                            .impliesDirect(new BundlePermission(
                                req.getModule().getSymbolicName(),
                                BundlePermission.FRAGMENT))
                        || !((BundleProtectionDomain) cap.getModule().getSecurityContext())
                            .impliesDirect(new BundlePermission(
                                cap.getModule().getSymbolicName(),
                                BundlePermission.HOST))))
                    {
                        continue;
                    }
                }

                if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE)
                    && cap.getModule().isResolved())
                {
                    continue;
                }

                result.add(cap);
            }
        }

        return result;
    }

    public void checkExecutionEnvironment(Module module) throws ResolveException
    {
        String bundleExecEnvStr = (String)
            module.getHeaders().get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
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
                        + bundleExecEnvStr, module, null);
                }
            }
        }
    }

    public void checkNativeLibraries(Module module) throws ResolveException
    {
        // Next, try to resolve any native code, since the module is
        // not resolvable if its native code cannot be loaded.
        List<R4Library> libs = module.getNativeLibraries();
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
                    if (!module.getContent().hasEntry(entryName))
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
                throw new ResolveException(msg, module, null);
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