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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Resolver;
import org.apache.felix.framework.resolver.ResolverImpl;
import org.apache.felix.framework.resolver.ResolverWire;
import org.apache.felix.framework.util.ShrinkableCollection;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.apache.felix.framework.wiring.BundleWireImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

class StatefulResolver
{
    private final Felix m_felix;
    private final Resolver m_resolver;
    private final ResolverStateImpl m_resolverState;
    private final List<ResolverHook> m_hooks = new ArrayList<ResolverHook>();
    private boolean m_isResolving = false;
    private Collection<BundleRevision> m_whitelist = null;

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

    void resolve(BundleRevision rootRevision) throws ResolveException, BundleException
    {
        // Although there is a race condition to check the bundle state
        // then lock it, we do this because we don't want to acquire the
        // a lock just to check if the revision is resolved, which itself
        // is a safe read. If the revision isn't resolved, we end up double
        // check the resolved status later.
        if (rootRevision.getWiring() == null)
        {
            // Acquire global lock.
            boolean locked = m_felix.acquireGlobalLock();
            if (!locked)
            {
                throw new ResolveException(
                    "Unable to acquire global lock for resolve.", rootRevision, null);
            }

            // Make sure we are not already resolving, which can be
            // the case if a resolver hook does something bad.
            if (m_isResolving)
            {
                m_felix.releaseGlobalLock();
                throw new IllegalStateException("Nested resolve operations not allowed.");
            }
            m_isResolving = true;

            Map<BundleRevision, List<ResolverWire>> wireMap = null;
            try
            {
                // Extensions are resolved differently.
                BundleImpl bundle = (BundleImpl) rootRevision.getBundle();
                if (bundle.isExtension())
                {
                    return;
                }

                // Get resolver hook factories.
                Set<ServiceReference<ResolverHookFactory>> hookRefs =
                    m_felix.getHooks(ResolverHookFactory.class);
                if (!hookRefs.isEmpty())
                {
                    // Create triggers list.
                    List<BundleRevision> triggers = new ArrayList<BundleRevision>(1);
                    triggers.add(rootRevision);
                    triggers = Collections.unmodifiableList(triggers);

                    // Create resolver hook objects by calling begin() on factory.
                    for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                    {
                        try
                        {
                            ResolverHook hook = m_felix.getService(m_felix, ref).begin(triggers);
                            if (hook != null)
                            {
                                m_hooks.add(hook);
                            }
                        }
                        catch (Throwable ex)
                        {
                            throw new BundleException(
                                "Resolver hook exception: " + ex.getMessage(),
                                BundleException.REJECTED_BY_HOOK,
                                ex);
                        }
                    }

                    // Ask hooks to indicate which revisions should not be resolved.
                    m_whitelist =
                        new ShrinkableCollection<BundleRevision>(
                            m_resolverState.getUnresolvedRevisions());
                    int originalSize = m_whitelist.size();
                    for (ResolverHook hook : m_hooks)
                    {
                        try
                        {
                            hook.filterResolvable(m_whitelist);
                        }
                        catch (Throwable ex)
                        {
                            throw new BundleException(
                                "Resolver hook exception: " + ex.getMessage(),
                                BundleException.REJECTED_BY_HOOK,
                                ex);
                        }
                    }
                    // If nothing was removed, then just null the whitelist
                    // as an optimization.
                    if (m_whitelist.size() == originalSize)
                    {
                        m_whitelist = null;
                    }

                    // Check to make sure the target revision is allowed to resolve.
                    if ((m_whitelist != null) && !m_whitelist.contains(rootRevision))
                    {
                        throw new ResolveException(
                            "Resolver hook prevented resolution.", rootRevision, null);
                    }
                }

                // Catch any resolve exception to rethrow later because
                // we may need to call end() on resolver hooks.
                ResolveException rethrow = null;
                try
                {
                    // Resolve the revision.
                    wireMap = m_resolver.resolve(
                        m_resolverState, rootRevision, m_resolverState.getFragments());
                }
                catch (ResolveException ex)
                {
                    rethrow = ex;
                }

                // If we have resolver hooks, we must call end() on them.
                if (!hookRefs.isEmpty())
                {
                    // Verify that all resolver hook service references are still valid
                    // Call end() on resolver hooks.
                    for (ResolverHook hook : m_hooks)
                    {
// TODO: OSGi R4.3/RESOLVER HOOK - We likely need to put these hooks into a map
//       to their svc ref since we aren't supposed to call end() on unregistered
//       but currently we call end() on all.
                        hook.end();
                    }
                    // Verify that all hook service references are still valid
                    // and unget all resolver hook factories.
                    boolean invalid = false;
                    for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                    {
                        if (ref.getBundle() == null)
                        {
                            invalid = true;
                        }
                        m_felix.ungetService(m_felix, ref);
                    }
                    if (invalid)
                    {
                        throw new BundleException(
                            "Resolver hook service unregistered during resolve.",
                            BundleException.REJECTED_BY_HOOK);
                    }
                }

                // If the resolve failed, rethrow the exception.
                if (rethrow != null)
                {
                    throw rethrow;
                }

                // Otherwise, mark all revisions as resolved.
                markResolvedRevisions(wireMap);
            }
            finally
            {
                // Clear resolving flag.
                m_isResolving = false;
                // Clear whitelist.
                m_whitelist = null;
                // Always clear any hooks.
                m_hooks.clear();
                // Always release the global lock.
                m_felix.releaseGlobalLock();
            }

            fireResolvedEvents(wireMap);
        }
    }

// TODO: OSGi R4.3 - Isn't this method just a generalization of the above method?
//       Can't we combine them and perhaps simplify the various resolve() methods
//       here and in Felix.java too?
    void resolve(Set<BundleRevision> revisions) throws ResolveException, BundleException
    {
        // Acquire global lock.
        boolean locked = m_felix.acquireGlobalLock();
        if (!locked)
        {
            throw new ResolveException(
                "Unable to acquire global lock for resolve.", null, null);
        }

        // Make sure we are not already resolving, which can be
        // the case if a resolver hook does something bad.
        if (m_isResolving)
        {
            m_felix.releaseGlobalLock();
            throw new IllegalStateException("Nested resolve operations not allowed.");
        }
        m_isResolving = true;

        Map<BundleRevision, List<ResolverWire>> wireMap = null;
        try
        {
            // Make our own copy of revisions.
            revisions = new HashSet<BundleRevision>(revisions);

            // Extensions are resolved differently.
            for (Iterator<BundleRevision> it = revisions.iterator(); it.hasNext(); )
            {
                BundleImpl bundle = (BundleImpl) it.next().getBundle();
                if (bundle.isExtension())
                {
                    it.remove();
                }
            }

            // Get resolver hook factories.
            Set<ServiceReference<ResolverHookFactory>> hookRefs =
                m_felix.getHooks(ResolverHookFactory.class);
            if (!hookRefs.isEmpty())
            {
                // Create triggers list.
                Collection<BundleRevision> triggers = Collections.unmodifiableSet(revisions);

                // Create resolver hook objects by calling begin() on factory.
                for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                {
                    try
                    {
                        ResolverHookFactory factory = m_felix.getService(m_felix, ref);
                        if (factory != null)
                        {
                            ResolverHook hook = factory.begin(triggers);
                            if (hook != null)
                            {
                                m_hooks.add(hook);
                            }
                        }
                    }
                    catch (Throwable ex)
                    {
                        throw new BundleException(
                            "Resolver hook exception: " + ex.getMessage(),
                            BundleException.REJECTED_BY_HOOK,
                            ex);
                    }
                }

                // Ask hooks to indicate which revisions should not be resolved.
                m_whitelist =
                    new ShrinkableCollection<BundleRevision>(
                        m_resolverState.getUnresolvedRevisions());
                int originalSize = m_whitelist.size();
                for (ResolverHook hook : m_hooks)
                {
                    try
                    {
                        hook.filterResolvable(m_whitelist);
                    }
                    catch (Throwable ex)
                    {
                        throw new BundleException(
                            "Resolver hook exception: " + ex.getMessage(),
                            BundleException.REJECTED_BY_HOOK,
                            ex);
                    }
                }
                // If nothing was removed, then just null the whitelist
                // as an optimization.
                if (m_whitelist.size() == originalSize)
                {
                    m_whitelist = null;
                }

                // Check to make sure the target revision is allowed to resolve.
                if (m_whitelist != null)
                {
                    revisions.retainAll(m_whitelist);
                    if (revisions.isEmpty())
                    {
                        throw new ResolveException(
                            "Resolver hook prevented resolution.", null, null);
                    }
                }
            }

            // Catch any resolve exception to rethrow later because
            // we may need to call end() on resolver hooks.
            ResolveException rethrow = null;
            try
            {
                // Resolve the revision.
// TODO: OSGi R4.3 - Shouldn't we still be passing in greedy attach fragments here?
                wireMap = m_resolver.resolve(
                    m_resolverState, revisions, m_resolverState.getFragments());
            }
            catch (ResolveException ex)
            {
                rethrow = ex;
            }

            // If we have resolver hooks, we must call end() on them.
            if (!hookRefs.isEmpty())
            {
                // Verify that all resolver hook service references are still valid
                // Call end() on resolver hooks.
                for (ResolverHook hook : m_hooks)
                {
// TODO: OSGi R4.3/RESOLVER HOOK - We likely need to put these hooks into a map
//       to their svc ref since we aren't supposed to call end() on unregistered
//       but currently we call end() on all.
                    hook.end();
                }
                // Verify that all hook service references are still valid
                // and unget all resolver hook factories.
                boolean invalid = false;
                for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                {
                    if (ref.getBundle() == null)
                    {
                        invalid = true;
                    }
                    m_felix.ungetService(m_felix, ref);
                }
                if (invalid)
                {
                    throw new BundleException(
                        "Resolver hook service unregistered during resolve.",
                        BundleException.REJECTED_BY_HOOK);
                }
            }

            // If the resolve failed, rethrow the exception.
            if (rethrow != null)
            {
                throw rethrow;
            }

            // Otherwise, mark all revisions as resolved.
            markResolvedRevisions(wireMap);
        }
        finally
        {
            // Clear resolving flag.
            m_isResolving = false;
            // Clear whitelist.
            m_whitelist = null;
            // Always clear any hooks.
            m_hooks.clear();
            // Always release the global lock.
            m_felix.releaseGlobalLock();
        }

        fireResolvedEvents(wireMap);
    }

    BundleRevision resolve(BundleRevision revision, String pkgName)
        throws ResolveException, BundleException
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

            // Make sure we are not already resolving, which can be
            // the case if a resolver hook does something bad.
            if (m_isResolving)
            {
                m_felix.releaseGlobalLock();
                throw new IllegalStateException("Nested resolve operations not allowed.");
            }
            m_isResolving = true;

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
                    // Get resolver hook factories.
                    Set<ServiceReference<ResolverHookFactory>> hookRefs =
                        m_felix.getHooks(ResolverHookFactory.class);
                    if (!hookRefs.isEmpty())
                    {
                        // Create triggers list.
                        List<BundleRevision> triggers = new ArrayList<BundleRevision>(1);
                        triggers.add(revision);
                        triggers = Collections.unmodifiableList(triggers);

                        // Create resolver hook objects by calling begin() on factory.
                        for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                        {
                            try
                            {
                                ResolverHook hook = m_felix.getService(m_felix, ref).begin(triggers);
                                if (hook != null)
                                {
                                    m_hooks.add(hook);
                                }
                            }
                            catch (Throwable ex)
                            {
                                throw new BundleException(
                                    "Resolver hook exception: " + ex.getMessage(),
                                    BundleException.REJECTED_BY_HOOK,
                                    ex);
                            }
                        }

                        // Ask hooks to indicate which revisions should not be resolved.
                        m_whitelist =
                            new ShrinkableCollection<BundleRevision>(
                                m_resolverState.getUnresolvedRevisions());
                        int originalSize = m_whitelist.size();
                        for (ResolverHook hook : m_hooks)
                        {
                            try
                            {
                                hook.filterResolvable(m_whitelist);
                            }
                            catch (Throwable ex)
                            {
                                throw new BundleException(
                                    "Resolver hook exception: " + ex.getMessage(),
                                    BundleException.REJECTED_BY_HOOK,
                                    ex);
                            }
                        }
                        // If nothing was removed, then just null the whitelist
                        // as an optimization.
                        if (m_whitelist.size() == originalSize)
                        {
                            m_whitelist = null;
                        }

                        // Since this is a dynamic import, the root revision is
                        // already resolved, so we don't need to check it against
                        // the whitelist as we do in other cases.
                    }

                    // Catch any resolve exception to rethrow later because
                    // we may need to call end() on resolver hooks.
                    ResolveException rethrow = null;
                    try
                    {
                        wireMap = m_resolver.resolve(
                            m_resolverState, revision, pkgName,
                            m_resolverState.getFragments());
                    }
                    catch (ResolveException ex)
                    {
                        rethrow = ex;
                    }

                    // If we have resolver hooks, we must call end() on them.
                    if (!hookRefs.isEmpty())
                    {
                        // Verify that all resolver hook service references are still valid
                        // Call end() on resolver hooks.
                        for (ResolverHook hook : m_hooks)
                        {
// TODO: OSGi R4.3/RESOLVER HOOK - We likely need to put these hooks into a map
//       to their svc ref since we aren't supposed to call end() on unregistered
//       but currently we call end() on all.
                            hook.end();
                        }
                        // Verify that all hook service references are still valid
                        // and unget all resolver hook factories.
                        boolean invalid = false;
                        for (ServiceReference<ResolverHookFactory> ref : hookRefs)
                        {
                            if (ref.getBundle() == null)
                            {
                                invalid = true;
                            }
                            m_felix.ungetService(m_felix, ref);
                        }
                        if (invalid)
                        {
                            throw new BundleException(
                                "Resolver hook service unregistered during resolve.",
                                BundleException.REJECTED_BY_HOOK);
                        }
                    }

                    // If the resolve failed, rethrow the exception.
                    if (rethrow != null)
                    {
                        throw rethrow;
                    }

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
                // Clear resolving flag.
                m_isResolving = false;
                // Clear whitelist.
                m_whitelist = null;
                // Always clear any hooks.
                m_hooks.clear();
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
                && cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).equals(pkgName))
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
        attrs.put(BundleRevision.PACKAGE_NAMESPACE, pkgName);
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
                                    .get(BundleRevision.PACKAGE_NAMESPACE),
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
            if (bundle.adapt(BundleRevision.class) == revision)
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
                        cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
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

//    void dump()
//    {
//        for (Entry<String, CapabilitySet> entry : m_capSets.entrySet())
//        {
//            System.out.println("+++ START CAPSET " + entry.getKey());
//            entry.getValue().dump();
//            System.out.println("+++ END CAPSET " + entry.getKey());
//        }
//    }

        ResolverStateImpl(Logger logger, String fwkExecEnvStr)
        {
            m_logger = logger;
            m_revisions = new HashSet<BundleRevision>();
            m_fragments = new HashSet<BundleRevision>();
            m_capSets = new HashMap<String, CapabilitySet>();

            m_fwkExecEnvStr = (fwkExecEnvStr != null) ? fwkExecEnvStr.trim() : null;
            m_fwkExecEnvSet = parseExecutionEnvironments(fwkExecEnvStr);

            List<String> indices = new ArrayList<String>();
            indices.add(BundleRevision.BUNDLE_NAMESPACE);
            m_capSets.put(BundleRevision.BUNDLE_NAMESPACE, new CapabilitySet(indices, true));

            indices = new ArrayList<String>();
            indices.add(BundleRevision.PACKAGE_NAMESPACE);
            m_capSets.put(BundleRevision.PACKAGE_NAMESPACE, new CapabilitySet(indices, true));

            indices = new ArrayList<String>();
            indices.add(BundleRevision.HOST_NAMESPACE);
            m_capSets.put(BundleRevision.HOST_NAMESPACE,  new CapabilitySet(indices, true));
        }

// TODO: OSGi R4.3/RESOLVER HOOK - We could maintain a separate list to optimize this.
        synchronized Set<BundleRevision> getUnresolvedRevisions()
        {
            Set<BundleRevision> unresolved = new HashSet<BundleRevision>();
            for (BundleRevision revision : m_revisions)
            {
                if (revision.getWiring() == null)
                {
                    unresolved.add(revision);
                }
            }
            return unresolved;
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
//       is correctly implemented, since it already has to remove substituted caps.
        synchronized void removeSubstitutedCapabilities(BundleRevision br)
        {
            if (br.getWiring() != null)
            {
                // Loop through the revision's package wires and determine if any
                // of them overlap any of the packages exported by the revision.
                // If so, then the framework must have chosen to have the revision
                // import rather than export the package, so we need to remove the
                // corresponding package capability from the package capability set.
                for (BundleWire w : br.getWiring().getRequiredWires(null))
                {
                    if (w.getCapability().getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        for (BundleCapability cap : br.getWiring().getCapabilities(null))
                        {
                            if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                                && w.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE)
                                    .equals(cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE)))
                            {
                                m_capSets.get(BundleRevision.PACKAGE_NAMESPACE).removeCapability(cap);
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
                        if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE) && (
                            !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                                new PackagePermission((String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                                PackagePermission.EXPORTONLY)) ||
                                !((reqRevision == null) ||
                                    ((BundleProtectionDomain) reqRevision.getProtectionDomain()).impliesDirect(
                                        new PackagePermission((String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                                        cap.getRevision().getBundle(),PackagePermission.IMPORT))
                                )))
                        {
                            if (reqRevision != cap.getRevision())
                            {
                                continue;
                            }
                        }
                        else if (req.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE) && (
                            !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                                new BundlePermission(cap.getRevision().getSymbolicName(), BundlePermission.PROVIDE)) ||
                                !((reqRevision == null) ||
                                    ((BundleProtectionDomain) reqRevision.getProtectionDomain()).impliesDirect(
                                        new BundlePermission(reqRevision.getSymbolicName(), BundlePermission.REQUIRE))
                                )))
                        {
                            continue;
                        }
                        else if (req.getNamespace().equals(BundleRevision.HOST_NAMESPACE) &&
                            (!((BundleProtectionDomain) reqRevision.getProtectionDomain())
                                .impliesDirect(new BundlePermission(
                                    reqRevision.getSymbolicName(),
                                    BundlePermission.FRAGMENT))
                            || !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain())
                                .impliesDirect(new BundlePermission(
                                    cap.getRevision().getSymbolicName(),
                                    BundlePermission.HOST))))
                        {
                            continue;
                        }
                    }

                    if (req.getNamespace().equals(BundleRevision.HOST_NAMESPACE)
                        && (cap.getRevision().getWiring() != null))
                    {
                        continue;
                    }

                    result.add(cap);
                }
            }

            // If we have resolver hooks, then we may need to filter our results
            // based on a whitelist and/or fine-grained candidate filtering.
            if (!result.isEmpty() && !m_hooks.isEmpty())
            {
                // It we have a whitelist, then first filter out candidates
                // from disallowed revisions.
// TODO: OSGi R4.3 - It would be better if we could think of a way to do this
//       filtering that was less costly. One possibility it to do the check in
//       ResolverState.checkExecutionEnvironment(), since it will only need to
//       be done once for any black listed revision. However, as we move toward
//       OBR-like API, this is a non-standard call, so doing it here is the only
//       standard way of achieving it.
                if (m_whitelist != null)
                {
                    for (Iterator<BundleCapability> it = result.iterator(); it.hasNext(); )
                    {
                        if (!m_whitelist.contains(it.next().getRevision()))
                        {
                            it.remove();
                        }
                    }
                }

                // Now give the hooks a chance to do fine-grained filtering.
                ShrinkableCollection<BundleCapability> shrinkable =
                    new ShrinkableCollection<BundleCapability>(result);
                for (ResolverHook hook : m_hooks)
                {
                    hook.filterMatches(req, shrinkable);
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
// TODO: OSGi R4.3 - Is it sufficient to just check declared native libs here?
//        List<R4Library> libs = ((BundleWiringImpl) revision.getWiring()).getNativeLibraries();
            List<R4Library> libs = ((BundleRevisionImpl) revision).getDeclaredNativeLibraries();
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