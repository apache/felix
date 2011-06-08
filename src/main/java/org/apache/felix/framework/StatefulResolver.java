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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Resolver;
import org.apache.felix.framework.resolver.ResolverImpl;
import org.apache.felix.framework.resolver.ResolverWire;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.apache.felix.framework.wiring.BundleWireImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

class StatefulResolver
{
    private final Felix m_felix;
    private final Resolver m_resolver;
    private final ResolverStateImpl m_resolverState;

    StatefulResolver(Felix felix)
    {
        m_felix = felix;
        m_resolver = new ResolverImpl(m_felix.getLogger());
        m_resolverState = new ResolverStateImpl(m_felix.getLogger(),
            (String) m_felix.getConfig().get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
    }

    void addRevision(BundleRevision br)
    {
        m_resolverState.addRevision(br);
    }

    void removeRevision(BundleRevision br)
    {
        m_resolverState.removeRevision(br);
    }

    Set<BundleCapability> getCandidates(BundleRequirementImpl req, boolean obeyMandatory)
    {
        return m_resolverState.getCandidates(req, obeyMandatory);
    }

    void resolve(BundleRevision rootRevision) throws ResolveException
    {
        // Although there is a race condition to check the bundle state
        // then lock it, we do this because we don't want to acquire the
        // a lock just to check if the revision is resolved, which itself
        // is a safe read. If the revision isn't resolved, we end up double
        // check the resolved status later.
// TODO: OSGi R4.3 - This locking strategy here depends on how we ultimately
//       implement getWiring(), which may change.
        if (rootRevision.getWiring() == null)
        {
            // Acquire global lock.
            boolean locked = m_felix.acquireGlobalLock();
            if (!locked)
            {
                throw new ResolveException(
                    "Unable to acquire global lock for resolve.", rootRevision, null);
            }

            Map<BundleRevision, List<ResolverWire>> wireMap = null;
            try
            {
                BundleImpl bundle = (BundleImpl) rootRevision.getBundle();

                // Extensions are resolved differently.
                if (bundle.isExtension())
                {
                    return;
                }

                // Resolve the revision.
                wireMap = m_resolver.resolve(
                    m_resolverState, rootRevision, m_resolverState.getFragments());

                // Mark all revisions as resolved.
                markResolvedRevisions(wireMap);
            }
            finally
            {
                // Always release the global lock.
                m_felix.releaseGlobalLock();
            }

            fireResolvedEvents(wireMap);
        }
    }

    BundleRevision resolve(BundleRevision revision, String pkgName)
        throws ResolveException
    {
        BundleRevision provider = null;

        // We cannot dynamically import if the revision is not already resolved
        // or if it is not allowed, so check that first. Note: We check if the
        // dynamic import is allowed without holding any locks, but this is
        // okay since the resolver will double check later after we have
        // acquired the global lock below.
        if ((revision.getWiring() != null) && isAllowedDynamicImport(revision, pkgName))
        {
            // Acquire global lock.
            boolean locked = m_felix.acquireGlobalLock();
            if (!locked)
            {
                throw new ResolveException(
                    "Unable to acquire global lock for resolve.", revision, null);
            }

            Map<BundleRevision, List<ResolverWire>> wireMap = null;
            try
            {
                // Double check to make sure that someone hasn't beaten us to
                // dynamically importing the package, which can happen if two
                // threads are racing to do so. If we have an existing wire,
                // then just return it instead.
                provider = ((BundleWiringImpl) revision.getWiring())
                    .getImportedPackageSource(pkgName);
                if (provider == null)
                {
                    wireMap = m_resolver.resolve(
                        m_resolverState, revision, pkgName,
                        m_resolverState.getFragments());

                    if ((wireMap != null) && wireMap.containsKey(revision))
                    {
                        List<ResolverWire> dynamicWires = wireMap.remove(revision);
                        ResolverWire dynamicWire = dynamicWires.get(0);

                        // Mark all revisions as resolved.
                        markResolvedRevisions(wireMap);

                        // Dynamically add new wire to importing revision.
                        if (dynamicWire != null)
                        {
                            m_felix.getDependencies().addDependent(
                                dynamicWire.getProvider(),
                                dynamicWire.getCapability(),
                                revision);

                            ((BundleWiringImpl) revision.getWiring())
                                .addDynamicWire(
                                    new BundleWireImpl(
                                        dynamicWire.getRequirer(),
                                        dynamicWire.getRequirement(),
                                        dynamicWire.getProvider(),
                                        dynamicWire.getCapability()));

                            m_felix.getLogger().log(
                                Logger.LOG_DEBUG,
                                "DYNAMIC WIRE: " + dynamicWire);

                            provider = ((BundleWiringImpl) revision.getWiring())
                                .getImportedPackageSource(pkgName);
                        }
                    }
                }
            }
            finally
            {
                // Always release the global lock.
                m_felix.releaseGlobalLock();
            }

            fireResolvedEvents(wireMap);
        }

        return provider;
    }

    // This method duplicates a lot of logic from:
    // ResolverImpl.getDynamicImportCandidates()
    boolean isAllowedDynamicImport(BundleRevision revision, String pkgName)
    {
        // Unresolved revisions cannot dynamically import, nor can the default
        // package be dynamically imported.
        if ((revision.getWiring() == null) || pkgName.length() == 0)
        {
            return false;
        }

        // If the revision doesn't have dynamic imports, then just return
        // immediately.
        List<BundleRequirement> dynamics =
            Util.getDynamicRequirements(revision.getWiring().getRequirements(null));
        if ((dynamics == null) || dynamics.isEmpty())
        {
            return false;
        }

        // If the revision exports this package, then we cannot
        // attempt to dynamically import it.
        for (BundleCapability cap : revision.getWiring().getCapabilities(null))
        {
            if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                && cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).equals(pkgName))
            {
                return false;
            }
        }

        // If this revision already imports or requires this package, then
        // we cannot dynamically import it.
        if (((BundleWiringImpl) revision.getWiring()).hasPackageSource(pkgName))
        {
            return false;
        }

        // Loop through the importer's dynamic requirements to determine if
        // there is a matching one for the package from which we want to
        // load a class.
        Map<String, Object> attrs = new HashMap(1);
        attrs.put(BundleCapabilityImpl.PACKAGE_ATTR, pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            revision,
            BundleRevision.PACKAGE_NAMESPACE,
            Collections.EMPTY_MAP,
            attrs);
        Set<BundleCapability> candidates = m_resolverState.getCandidates(req, false);

        return !candidates.isEmpty();
    }

    private void markResolvedRevisions(Map<BundleRevision, List<ResolverWire>> wireMap)
        throws ResolveException
    {
        // DO THIS IN THREE PASSES:
        // 1. Aggregate fragments per host.
        // 2. Attach wires and fragments to hosts.
        //    -> If fragments fail to attach, then undo.
        // 3. Mark hosts and fragments as resolved.

        // First pass.
        if (wireMap != null)
        {
            // First pass: Loop through the wire map to find the host wires
            // for any fragments and map a host to all of its fragments.
            Map<BundleRevision, List<BundleRevision>> hosts =
                new HashMap<BundleRevision, List<BundleRevision>>();
            for (Entry<BundleRevision, List<ResolverWire>> entry : wireMap.entrySet())
            {
                BundleRevision revision = entry.getKey();
                List<ResolverWire> wires = entry.getValue();

                if (Util.isFragment(revision))
                {
                    for (Iterator<ResolverWire> itWires = wires.iterator();
                        itWires.hasNext(); )
                    {
                        ResolverWire w = itWires.next();
                        List<BundleRevision> fragments = hosts.get(w.getProvider());
                        if (fragments == null)
                        {
                            fragments = new ArrayList<BundleRevision>();
                            hosts.put(w.getProvider(), fragments);
                        }
                        fragments.add(w.getRequirer());
                    }
                }
            }

            // Second pass: Loop through the wire map to do three things:
            // 1) convert resolver wires to bundle wires 2) create wiring
            // objects for revisions and 3) record dependencies among
            // revisions. We don't actually set the wirings here because
            // that indicates that a revision is resolved and we don't want
            // to mark anything as resolved unless we succussfully create
            // all wirings.
            Map<BundleRevision, BundleWiringImpl> wirings =
                new HashMap<BundleRevision, BundleWiringImpl>(wireMap.size());
            for (Entry<BundleRevision, List<ResolverWire>> entry : wireMap.entrySet())
            {
                BundleRevision revision = entry.getKey();
                List<ResolverWire> resolverWires = entry.getValue();

                List<BundleWire> bundleWires =
                    new ArrayList<BundleWire>(resolverWires.size());

                // Loop through resolver wires to calculate the package
                // space implied by the wires as well as to record the
                // dependencies.
                Map<String, BundleRevision> importedPkgs =
                    new HashMap<String, BundleRevision>();
                Map<String, List<BundleRevision>> requiredPkgs =
                    new HashMap<String, List<BundleRevision>>();
                for (ResolverWire rw : resolverWires)
                {
                    bundleWires.add(
                        new BundleWireImpl(
                            rw.getRequirer(),
                            rw.getRequirement(),
                            rw.getProvider(),
                            rw.getCapability()));

                    m_felix.getDependencies().addDependent(
                        rw.getProvider(), rw.getCapability(), rw.getRequirer());

                    if (Util.isFragment(revision))
                    {
                        m_felix.getLogger().log(
                            Logger.LOG_DEBUG,
                            "FRAGMENT WIRE: " + rw.toString());
                    }
                    else
                    {
                        m_felix.getLogger().log(Logger.LOG_DEBUG, "WIRE: " + rw.toString());

                        if (rw.getCapability().getNamespace()
                            .equals(BundleRevision.PACKAGE_NAMESPACE))
                        {
                            importedPkgs.put(
                                (String) rw.getCapability().getAttributes()
                                    .get(BundleCapabilityImpl.PACKAGE_ATTR),
                                rw.getProvider());
                        }
                        else if (rw.getCapability().getNamespace()
                            .equals(BundleRevision.BUNDLE_NAMESPACE))
                        {
                            Set<String> pkgs = calculateExportedAndReexportedPackages(
                                    rw.getProvider(),
                                    wireMap,
                                    new HashSet<String>(),
                                    new HashSet<BundleRevision>());
                            for (String pkg : pkgs)
                            {
                                List<BundleRevision> revs = requiredPkgs.get(pkg);
                                if (revs == null)
                                {
                                    revs = new ArrayList<BundleRevision>();
                                    requiredPkgs.put(pkg, revs);
                                }
                                revs.add(rw.getProvider());
                            }
                        }
                    }
                }

                List<BundleRevision> fragments = hosts.get(revision);
                try
                {
                    wirings.put(
                        revision,
                        new BundleWiringImpl(
                            m_felix.getLogger(),
                            m_felix.getConfig(),
                            this,
                            (BundleRevisionImpl) revision,
                            fragments,
                            bundleWires,
                            importedPkgs,
                            requiredPkgs));
                }
                catch (Exception ex)
                {
                    // This is a fatal error, so undo everything and
                    // throw an exception.
                    for (Entry<BundleRevision, BundleWiringImpl> wiringEntry
                        : wirings.entrySet())
                    {
                        // Dispose of wiring.
                        try
                        {
                            wiringEntry.getValue().dispose();
                        }
                        catch (Exception ex2)
                        {
                            // We are in big trouble.
                            RuntimeException rte = new RuntimeException(
                                "Unable to clean up resolver failure.", ex2);
                            m_felix.getLogger().log(
                                Logger.LOG_ERROR,
                                rte.getMessage(), ex2);
                            throw rte;
                        }

                        // Reindex host with no fragments.
                        m_resolverState.addRevision(revision);
                    }

                    ResolveException re = new ResolveException(
                        "Unable to resolve " + revision,
                        revision, null);
                    re.initCause(ex);
                    m_felix.getLogger().log(
                        Logger.LOG_ERROR,
                        re.getMessage(), ex);
                    throw re;
                }
            }

            // Third pass: Loop through the wire map to mark revision as resolved
            // and update the resolver state.
            for (Entry<BundleRevision, BundleWiringImpl> entry : wirings.entrySet())
            {
                BundleRevisionImpl revision = (BundleRevisionImpl) entry.getKey();

                // Mark revision as resolved.
                revision.resolve(entry.getValue());

                // Update resolver state to remove substituted capabilities.
                if (!Util.isFragment(revision))
                {
                    // Update resolver state by reindexing host with attached
                    // fragments and removing any substituted exports.
// TODO: OSGi R4.3 - We could avoid reindexing for fragments if we check it
//       the revision has fragments or not.
                    m_resolverState.addRevision(revision);
                    m_resolverState.removeSubstitutedCapabilities(revision);
                }

                // Update the state of the revision's bundle to resolved as well.
                markBundleResolved(revision);
            }
        }
    }

    private void markBundleResolved(BundleRevision revision)
    {
        // Update the bundle's state to resolved when the
        // current revision is resolved; just ignore resolve
        // events for older revisions since this only occurs
        // when an update is done on an unresolved bundle
        // and there was no refresh performed.
        BundleImpl bundle = (BundleImpl) revision.getBundle();

        // Lock the bundle first.
        try
        {
            // Acquire bundle lock.
            try
            {
                m_felix.acquireBundleLock(
                    bundle, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE);
            }
            catch (IllegalStateException ex)
            {
                // There is nothing we can do.
            }
            if (bundle.getCurrentRevision() == revision)
            {
                if (bundle.getState() != Bundle.INSTALLED)
                {
                    m_felix.getLogger().log(bundle,
                        Logger.LOG_WARNING,
                        "Received a resolve event for a bundle that has already been resolved.");
                }
                else
                {
                    m_felix.setBundleStateAndNotify(bundle, Bundle.RESOLVED);
                }
            }
        }
        finally
        {
            m_felix.releaseBundleLock(bundle);
        }
    }

    private void fireResolvedEvents(Map<BundleRevision, List<ResolverWire>> wireMap)
    {
        if (wireMap != null)
        {
            Iterator<Entry<BundleRevision, List<ResolverWire>>> iter = wireMap.entrySet().iterator();
            // Iterate over the map to fire necessary RESOLVED events.
            while (iter.hasNext())
            {
                Entry<BundleRevision, List<ResolverWire>> entry = iter.next();
                BundleRevision revision = entry.getKey();

                // Fire RESOLVED events for all fragments.
                List<BundleRevision> fragments =
                    ((BundleWiringImpl) revision.getWiring()).getFragments();
                for (int i = 0; (fragments != null) && (i < fragments.size()); i++)
                {
                    m_felix.fireBundleEvent(BundleEvent.RESOLVED, fragments.get(i).getBundle());
                }
                m_felix.fireBundleEvent(BundleEvent.RESOLVED, revision.getBundle());
            }
        }
    }

    private static Set<String> calculateExportedAndReexportedPackages(
        BundleRevision br,
        Map<BundleRevision, List<ResolverWire>> wireMap,
        Set<String> pkgs,
        Set<BundleRevision> cycles)
    {
        if (!cycles.contains(br))
        {
            cycles.add(br);

            // Add all exported packages.
            for (BundleCapability cap : br.getDeclaredCapabilities(null))
            {
                if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    pkgs.add((String)
                        cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
                }
            }

            // Now check to see if any required bundles are required with reexport
            // visibility, since we need to include those packages too.
            if (br.getWiring() == null)
            {
                for (ResolverWire rw : wireMap.get(br))
                {
                    if (rw.getCapability().getNamespace().equals(
                        BundleRevision.BUNDLE_NAMESPACE))
                    {
                        String dir = rw.getRequirement()
                            .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((dir != null) && (dir.equals(Constants.VISIBILITY_REEXPORT)))
                        {
                            calculateExportedAndReexportedPackages(
                                rw.getProvider(),
                                wireMap,
                                pkgs,
                                cycles);
                        }
                    }
                }
            }
            else
            {
                for (BundleWire bw : br.getWiring().getRequiredWires(null))
                {
                    if (bw.getCapability().getNamespace().equals(
                        BundleRevision.BUNDLE_NAMESPACE))
                    {
                        String dir = bw.getRequirement()
                            .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((dir != null) && (dir.equals(Constants.VISIBILITY_REEXPORT)))
                        {
                            calculateExportedAndReexportedPackages(
                                bw.getProviderWiring().getRevision(),
                                wireMap,
                                pkgs,
                                cycles);
                        }
                    }
                }
            }
        }

        return pkgs;
    }
}