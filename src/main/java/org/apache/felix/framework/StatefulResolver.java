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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.util.ShrinkableCollection;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.NativeLibrary;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.apache.felix.framework.wiring.BundleWireImpl;
import org.apache.felix.resolver.ResolverImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

class StatefulResolver
{
    private final Logger m_logger;
    private final Felix m_felix;
    private final ServiceRegistry m_registry;
    private final ResolverImpl m_resolver;
    private boolean m_isResolving = false;

    // Set of all revisions.
    private final Set<BundleRevision> m_revisions;
    // Set of all fragments.
    private final Set<BundleRevision> m_fragments;
    // Capability sets.
    private final Map<String, CapabilitySet> m_capSets;
    // Maps singleton symbolic names to list of bundle revisions sorted by version.
    private final Map<String, List<BundleRevision>> m_singletons;
    // Selected singleton bundle revisions.
    private final Set<BundleRevision> m_selectedSingletons;
    // Execution environment.
    private final String m_fwkExecEnvStr;
    // Parsed framework environments
    private final Set<String> m_fwkExecEnvSet;

    StatefulResolver(Felix felix, ServiceRegistry registry)
    {
        m_felix = felix;
        m_registry = registry;
        m_logger = m_felix.getLogger();
        m_resolver = new ResolverImpl(m_logger);

        m_revisions = new HashSet<BundleRevision>();
        m_fragments = new HashSet<BundleRevision>();
        m_capSets = new HashMap<String, CapabilitySet>();
        m_singletons = new HashMap<String, List<BundleRevision>>();
        m_selectedSingletons = new HashSet<BundleRevision>();

        String fwkExecEnvStr =
            (String) m_felix.getConfig().get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
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

    void start()
    {
        m_registry.registerService(m_felix,
                new String[] { Resolver.class.getName() },
                m_resolver,
                null);
    }

    synchronized void addRevision(BundleRevision br)
    {
        // Always attempt to remove the revision, since
        // this method can be used for re-indexing a revision
        // after it has been resolved.
        removeRevision(br);

        m_revisions.add(br);

        // Add singletons to the singleton map.
        boolean isSingleton = Util.isSingleton(br);
        if (isSingleton)
        {
            // Index the new singleton.
            addToSingletonMap(m_singletons, br);
        }

        // We always need to index non-singleton bundle capabilities, but
        // singleton bundles only need to be index if they are resolved.
        // Unresolved singleton capabilities are only indexed before a
        // resolve operation when singleton selection is performed.
        if (!isSingleton || (br.getWiring() != null))
        {
            if (Util.isFragment(br))
            {
                m_fragments.add(br);
            }
            indexCapabilities(br);
        }
    }

    synchronized void removeRevision(BundleRevision br)
    {
        if (m_revisions.remove(br))
        {
            m_fragments.remove(br);
            deindexCapabilities(br);

            // If this module is a singleton, then remove it from the
            // singleton map.
            List<BundleRevision> revisions = m_singletons.get(br.getSymbolicName());
            if (revisions != null)
            {
                revisions.remove(br);
                if (revisions.isEmpty())
                {
                    m_singletons.remove(br.getSymbolicName());
                }
            }
        }
    }

    boolean isEffective(Requirement req)
    {
        String effective = req.getDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
        return ((effective == null) || effective.equals(Constants.EFFECTIVE_RESOLVE));
    }

    synchronized List<BundleCapability> findProviders(
        BundleRequirement req, boolean obeyMandatory)
    {
        ResolverHookRecord record = new ResolverHookRecord(
            Collections.<ServiceReference<ResolverHookFactory>, ResolverHook>emptyMap(), null);
        return findProvidersInternal(record, req, obeyMandatory, true);
    }

    synchronized List<BundleCapability> findProvidersInternal(
        final ResolverHookRecord record,
        final Requirement req,
        final boolean obeyMandatory,
        final boolean invokeHooksAndSecurity)
    {
        List<BundleCapability> result = new ArrayList<BundleCapability>();

        CapabilitySet capSet = m_capSets.get(req.getNamespace());
        if (capSet != null)
        {
            // Get the requirement's filter; if this is our own impl we
            // have a shortcut to get the already parsed filter, otherwise
            // we must parse it from the directive.
            SimpleFilter sf;
            if (req instanceof BundleRequirementImpl)
            {
                sf = ((BundleRequirementImpl) req).getFilter();
            }
            else
            {
                String filter = req.getDirectives().get(Constants.FILTER_DIRECTIVE);
                if (filter == null)
                {
                    sf = new SimpleFilter(null, null, SimpleFilter.MATCH_ALL);
                }
                else
                {
                    sf = SimpleFilter.parse(filter);
                }
            }

            // Find the matching candidates.
            Set<Capability> matches = capSet.match(sf, obeyMandatory);
            // Filter matching candidates.
            for (Capability cap : matches)
            {
                if (!(cap instanceof BundleCapability))
                    continue;

                BundleCapability bcap = (BundleCapability) cap;

                // Filter according to security.
                if (invokeHooksAndSecurity && filteredBySecurity((BundleRequirement)req, bcap))
                {
                    continue;
                }
                // Filter already resolved hosts, since we don't support
                // dynamic attachment of fragments.
                if (req.getNamespace().equals(BundleRevision.HOST_NAMESPACE)
                    && (bcap.getRevision().getWiring() != null))
                {
                    continue;
                }

                result.add(bcap);
            }
        }

        if ( invokeHooksAndSecurity )
        {
            // If we have resolver hooks, then we may need to filter our results
            // based on a whitelist and/or fine-grained candidate filtering.
            if (!result.isEmpty() && !record.getResolverHookRefs().isEmpty())
            {
                // It we have a whitelist, then first filter out candidates
                // from disallowed revisions.
                if (record.getBundleRevisionWhitelist() != null)
                {
                    for (Iterator<BundleCapability> it = result.iterator(); it.hasNext(); )
                    {
                        if (!record.getBundleRevisionWhitelist().contains(it.next().getRevision()))
                        {
                            it.remove();
                        }
                    }
                }

                // Now give the hooks a chance to do fine-grained filtering.
                ShrinkableCollection<BundleCapability> shrinkable =
                    new ShrinkableCollection<BundleCapability>(result);
                for (ResolverHook hook : record.getResolverHooks())
                {
                    try
                    {
                        Felix.m_secureAction
                            .invokeResolverHookMatches(hook, (BundleRequirement)req, shrinkable);
                    }
                    catch (Throwable th)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Resolver hook exception.", th);
                    }
                }
            }
        }

        Collections.sort(result, new CandidateComparator());

        return result;
    }

    private boolean filteredBySecurity(BundleRequirement req, BundleCapability cap)
    {
        if (System.getSecurityManager() != null)
        {
            BundleRevisionImpl reqRevision = (BundleRevisionImpl) req.getRevision();

            if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
            {
                if (!((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                    new PackagePermission((String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                    PackagePermission.EXPORTONLY)) ||
                    !((reqRevision == null) ||
                        ((BundleProtectionDomain) reqRevision.getProtectionDomain()).impliesDirect(
                            new PackagePermission((String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                            cap.getRevision().getBundle(),PackagePermission.IMPORT))
                    ))
                {
                    if (reqRevision != cap.getRevision())
                    {
                        return true;
                    }
                }
            }
            else if (req.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE))
            {   if (!((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                    new BundlePermission(cap.getRevision().getSymbolicName(), BundlePermission.PROVIDE)) ||
                    !((reqRevision == null) ||
                        ((BundleProtectionDomain) reqRevision.getProtectionDomain()).impliesDirect(
                            new BundlePermission(reqRevision.getSymbolicName(), BundlePermission.REQUIRE))
                    ))
                {
                    return true;
                }
            }
            else if (req.getNamespace().equals(BundleRevision.HOST_NAMESPACE))
            {
                if (!((BundleProtectionDomain) reqRevision.getProtectionDomain())
                    .impliesDirect(new BundlePermission(
                        reqRevision.getSymbolicName(),
                        BundlePermission.FRAGMENT))
                || !((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain())
                    .impliesDirect(new BundlePermission(
                        cap.getRevision().getSymbolicName(),
                        BundlePermission.HOST)))
                {
                    return true;
                }
            }
            else  if (!req.getNamespace().equals("osgi.ee"))
            {
                if (!((BundleProtectionDomain) ((BundleRevisionImpl) cap.getRevision()).getProtectionDomain()).impliesDirect(
                    new CapabilityPermission(req.getNamespace(), CapabilityPermission.PROVIDE))
                    ||
                    !((reqRevision == null) || ((BundleProtectionDomain) reqRevision.getProtectionDomain()).impliesDirect(
                    new CapabilityPermission(req.getNamespace(), cap.getAttributes(), cap.getRevision().getBundle(), CapabilityPermission.REQUIRE))))
                {
                    return true;
                }
            }
        }
        return false;
    }

    void resolve(
        Set<BundleRevision> mandatory,
        Set<BundleRevision> optional)
        throws ResolutionException, BundleException
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

        Map<Resource, List<Wire>> wireMap = null;
        try
        {
            // Make our own copy of revisions.
            mandatory = (mandatory.isEmpty())
                ? mandatory : new HashSet<BundleRevision>(mandatory);
            optional = (optional.isEmpty())
                ? optional : new HashSet<BundleRevision>(optional);

            // Prepare resolver hooks, if any.
            ResolverHookRecord record = prepareResolverHooks(mandatory, optional);

            // Select any singletons in the resolver state.
            selectSingletons(record);

            // Extensions are resolved differently.
            for (Iterator<BundleRevision> it = mandatory.iterator(); it.hasNext(); )
            {
                BundleRevision br = it.next();
                BundleImpl bundle = (BundleImpl) br.getBundle();
                if (bundle.isExtension())
                {
                    it.remove();
                }
                else if (Util.isSingleton(br) && !isSelectedSingleton(br))
                {
                    throw new ResolveException("Singleton conflict.", br, null);
                }
            }
            for (Iterator<BundleRevision> it = optional.iterator(); it.hasNext(); )
            {
                BundleRevision br = it.next();
                BundleImpl bundle = (BundleImpl) br.getBundle();
                if (bundle.isExtension())
                {
                    it.remove();
                }
                else if (Util.isSingleton(br) && !isSelectedSingleton(br))
                {
                    it.remove();
                }
            }

            // Catch any resolve exception to rethrow later because
            // we may need to call end() on resolver hooks.
            ResolutionException rethrow = null;
            try
            {
                // Resolve the revision.
                wireMap = m_resolver.resolve(
                    new ResolveContextImpl(
                        this,
                        getWirings(),
                        record,
                        mandatory,
                        optional,
                        getFragments()));
            }
            catch (ResolutionException ex)
            {
                rethrow = ex;
            }

            // Release resolver hooks, if any.
            releaseResolverHooks(record);

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
            // Always release the global lock.
            m_felix.releaseGlobalLock();
        }

        fireResolvedEvents(wireMap);
    }

    BundleRevision resolve(BundleRevision revision, String pkgName)
        throws ResolutionException, BundleException
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

            Map<Resource, List<Wire>> wireMap = null;
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
                    // Prepare resolver hooks, if any.
                    ResolverHookRecord record =
                        prepareResolverHooks(
                            Collections.singleton(revision), Collections.EMPTY_SET);

                    // Select any singletons in the resolver state.
                    selectSingletons(record);

                    // Catch any resolve exception to rethrow later because
                    // we may need to call end() on resolver hooks.
                    ResolutionException rethrow = null;
                    try
                    {
                        List<BundleRequirement> dynamics =
                                Util.getDynamicRequirements(revision.getWiring().getRequirements(null));

                        // Loop through the importer's dynamic requirements to determine if
                        // there is a matching one for the package from which we want to
                        // load a class.
                        Map<String, Object> attrs = Collections.singletonMap(
                                BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
                        BundleRequirementImpl req = new BundleRequirementImpl(
                                revision,
                                BundleRevision.PACKAGE_NAMESPACE,
                                Collections.EMPTY_MAP,
                                attrs);
                        List<BundleCapability> candidates = findProvidersInternal(record, req, false, true);

                        // Try to find a dynamic requirement that matches the capabilities.
                        BundleRequirementImpl dynReq = null;
                        for (int dynIdx = 0;
                             (candidates.size() > 0) && (dynReq == null) && (dynIdx < dynamics.size());
                             dynIdx++)
                        {
                            for (Iterator<BundleCapability> itCand = candidates.iterator();
                                 (dynReq == null) && itCand.hasNext(); )
                            {
                                Capability cap = itCand.next();
                                if (CapabilitySet.matches(
                                        cap,
                                        ((BundleRequirementImpl) dynamics.get(dynIdx)).getFilter()))
                                {
                                    dynReq = (BundleRequirementImpl) dynamics.get(dynIdx);
                                }
                            }
                        }

                        // If we found a matching dynamic requirement, then filter out
                        // any candidates that do not match it.
                        if (dynReq != null)
                        {
                            for (Iterator<BundleCapability> itCand = candidates.iterator();
                                 itCand.hasNext(); )
                            {
                                Capability cap = itCand.next();
                                if (!CapabilitySet.matches(
                                        cap, dynReq.getFilter()))
                                {
                                    itCand.remove();
                                }
                            }
                        }
                        else
                        {
                            candidates.clear();
                        }

                        wireMap = m_resolver.resolve(
                            new ResolveContextImpl(
                                this,
                                getWirings(),
                                record,
                                Collections.<BundleRevision>emptyList(),
                                Collections.<BundleRevision>emptyList(),
                                getFragments()),
                            revision, dynReq, new ArrayList<Capability>(candidates));
                    }
                    catch (ResolutionException ex)
                    {
                        rethrow = ex;
                    }

                    // Release resolver hooks, if any.
                    releaseResolverHooks(record);

                    // If the resolve failed, rethrow the exception.
                    if (rethrow != null)
                    {
                        throw rethrow;
                    }

                    if ((wireMap != null) && wireMap.containsKey(revision))
                    {
                        List<Wire> dynamicWires = wireMap.remove(revision);
                        Wire dynamicWire = dynamicWires.get(0);

                        // Mark all revisions as resolved.
                        markResolvedRevisions(wireMap);

                        // Dynamically add new wire to importing revision.
                        if (dynamicWire != null)
                        {
                            // TODO is a rw already a BundleWire?
                            // TODO can we optimize this?
                            if (dynamicWire.getRequirer() instanceof BundleRevision &&
                                dynamicWire.getRequirement() instanceof BundleRequirement &&
                                dynamicWire.getProvider() instanceof BundleRevision &&
                                dynamicWire.getCapability() instanceof BundleCapability)
                            {
                                BundleRevision dwRequirer = (BundleRevision) dynamicWire.getRequirer();
                                BundleRequirement dwRequirement = (BundleRequirement) dynamicWire.getRequirement();
                                BundleRevision dwProvider = (BundleRevision) dynamicWire.getProvider();
                                BundleCapability dwCapability = (BundleCapability) dynamicWire.getCapability();

                                BundleWire bw = new BundleWireImpl(
                                    dwRequirer,
                                    dwRequirement,
                                    dwProvider,
                                    dwCapability);

                                m_felix.getDependencies().addDependent(bw);

                                ((BundleWiringImpl) revision.getWiring()).addDynamicWire(bw);

                                m_felix.getLogger().log(
                                    Logger.LOG_DEBUG,
                                    "DYNAMIC WIRE: " + dynamicWire);

                                provider = ((BundleWiringImpl) revision.getWiring())
                                    .getImportedPackageSource(pkgName);
                            }
                        }
                    }
                }
            }
            finally
            {
                // Clear resolving flag.
                m_isResolving = false;
                // Always release the global lock.
                m_felix.releaseGlobalLock();
            }

            fireResolvedEvents(wireMap);
        }

        return provider;
    }

    private ResolverHookRecord prepareResolverHooks(
        Set<BundleRevision> mandatory, Set<BundleRevision> optional)
        throws BundleException, ResolutionException
    {
        // This map maps the hook factory service to the actual hook objects. It
        // needs to be a map that preserves insertion order to ensure that we call
        // hooks in the correct order.
        // The hooks are added in the order that m_felix.getHooks() returns them which
        // is also the order in which they should be called.
        Map<ServiceReference<ResolverHookFactory>, ResolverHook> hookMap =
            new LinkedHashMap<ServiceReference<ResolverHookFactory>, ResolverHook>();

        // Get resolver hook factories.
        Set<ServiceReference<ResolverHookFactory>> hookRefs =
            m_felix.getHookRegistry().getHooks(ResolverHookFactory.class);
        Collection<BundleRevision> whitelist;

        if (!hookRefs.isEmpty())
        {
            // Create triggers list.
            Set<BundleRevision> triggers;
            if (!mandatory.isEmpty() && !optional.isEmpty())
            {
                triggers = new HashSet<BundleRevision>(mandatory);
                triggers.addAll(optional);
            }
            else
            {
                triggers = (mandatory.isEmpty())
                    ? optional : mandatory;
            }
            triggers = Collections.unmodifiableSet(triggers);

            BundleException rethrow = null;

            // Create resolver hook objects by calling begin() on factory.
            for (ServiceReference<ResolverHookFactory> ref : hookRefs)
            {
                try
                {
                    ResolverHookFactory rhf = m_felix.getService(m_felix, ref, false);
                    if (rhf != null)
                    {
                        ResolverHook hook =
                            Felix.m_secureAction
                                .invokeResolverHookFactory(rhf, triggers);
                        if (hook != null)
                        {
                            hookMap.put(ref, hook);
                        }
                    }
                }
                catch (Throwable ex)
                {
                    rethrow = new BundleException(
                        "Resolver hook exception: " + ex.getMessage(),
                        BundleException.REJECTED_BY_HOOK,
                        ex);
                    // Resolver hook spec: if there is an exception during the resolve operation; abort.
                    // So we break here to make sure that no further resolver hooks are created.
                    break;
                }
            }

            if (rethrow != null)
            {
                for (ResolverHook hook : hookMap.values())
                {
                    try
                    {
                        Felix.m_secureAction.invokeResolverHookEnd(hook);
                    }
                    catch (Exception ex)
                    {
                        rethrow = new BundleException(
                                "Resolver hook exception: " + ex.getMessage(),
                                BundleException.REJECTED_BY_HOOK,
                                ex);
                    }
                }

                throw rethrow;
            }

            // Ask hooks to indicate which revisions should not be resolved.
            whitelist = new ShrinkableCollection<BundleRevision>(getUnresolvedRevisions());
            int originalSize = whitelist.size();
            for (ResolverHook hook : hookMap.values())
            {
                try
                {
                    Felix.m_secureAction
                        .invokeResolverHookResolvable(hook, whitelist);
                }
                catch (Throwable ex)
                {
                    rethrow = new BundleException(
                        "Resolver hook exception: " + ex.getMessage(),
                        BundleException.REJECTED_BY_HOOK,
                        ex);
                    // Resolver hook spec: if there is an exception during the resolve operation; abort.
                    // So we break here to make sure that no further resolver operations are executed.
                    break;
                }
            }

            if (rethrow != null)
            {
                for (ResolverHook hook : hookMap.values())
                {
                    try
                    {
                        Felix.m_secureAction.invokeResolverHookEnd(hook);
                    }
                    catch (Exception ex)
                    {
                        rethrow = new BundleException(
                                "Resolver hook exception: " + ex.getMessage(),
                                BundleException.REJECTED_BY_HOOK,
                                ex);
                    }
                }

                throw rethrow;
            }

            // If nothing was removed, then just null the whitelist
            // as an optimization.
            if (whitelist.size() == originalSize)
            {
                whitelist = null;
            }

            // Check to make sure the target revisions are allowed to resolve.
            if (whitelist != null)
            {
                // We only need to check this for the non-dynamic import
                // case. The dynamic import case will only have one resolved
                // trigger revision in the mandatory set, so ignore that case.
                if (mandatory.isEmpty()
                    || !optional.isEmpty()
                    || (mandatory.iterator().next().getWiring() == null))
                {
                    mandatory.retainAll(whitelist);
                    optional.retainAll(whitelist);
                    if (mandatory.isEmpty() && optional.isEmpty())
                    {
                        throw new ResolveException(
                            "Resolver hook prevented resolution.", null, null);
                    }
                }
            }
        }
        else
        {
            whitelist = null;
        }

        return new ResolverHookRecord(hookMap, whitelist);
    }

    private void releaseResolverHooks(ResolverHookRecord record)
        throws BundleException
    {
        // If we have resolver hooks, we must call end() on them.
        if (!record.getResolverHookRefs().isEmpty())
        {
            // Verify that all resolver hook service references are still valid
            // Call end() on resolver hooks.
            for (ResolverHook hook : record.getResolverHooks())
            {
                try
                {
                    Felix.m_secureAction.invokeResolverHookEnd(hook);
                }
                catch (Throwable th)
                {
                    m_logger.log(
                        Logger.LOG_WARNING, "Resolver hook exception.", th);
                }
            }
            // Verify that all hook service references are still valid
            // and unget all resolver hook factories.
            boolean invalid = false;
            for (ServiceReference<ResolverHookFactory> ref : record.getResolverHookRefs())
            {
                if (ref.getBundle() == null)
                {
                    invalid = true;
                }
                m_felix.ungetService(m_felix, ref, null);
            }
            if (invalid)
            {
                throw new BundleException(
                    "Resolver hook service unregistered during resolve.",
                    BundleException.REJECTED_BY_HOOK);
            }
        }
    }

    // This method duplicates a lot of logic from:
    // ResolverImpl.getDynamicImportCandidates()
    // This is only a rough check since it doesn't include resolver hooks.
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
        Map<String, Object> attrs = Collections.singletonMap(
            BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            revision,
            BundleRevision.PACKAGE_NAMESPACE,
            Collections.EMPTY_MAP,
            attrs);
        List<BundleCapability> candidates = findProviders(req, false);

        // Try to find a dynamic requirement that matches the capabilities.
        BundleRequirementImpl dynReq = null;
        for (int dynIdx = 0;
            (candidates.size() > 0) && (dynReq == null) && (dynIdx < dynamics.size());
            dynIdx++)
        {
            for (Iterator<BundleCapability> itCand = candidates.iterator();
                (dynReq == null) && itCand.hasNext(); )
            {
                Capability cap = itCand.next();
                if (CapabilitySet.matches(
                    cap,
                    ((BundleRequirementImpl) dynamics.get(dynIdx)).getFilter()))
                {
                    dynReq = (BundleRequirementImpl) dynamics.get(dynIdx);
                }
            }
        }

        // If we found a matching dynamic requirement, then filter out
        // any candidates that do not match it.
        if (dynReq != null)
        {
            for (Iterator<BundleCapability> itCand = candidates.iterator();
                itCand.hasNext(); )
            {
                Capability cap = itCand.next();
                if (!CapabilitySet.matches(
                    cap, dynReq.getFilter()))
                {
                    itCand.remove();
                }
            }
        }
        else
        {
            candidates.clear();
        }

        return !candidates.isEmpty();
    }

    private void markResolvedRevisions(Map<Resource, List<Wire>> wireMap)
        throws ResolveException
    {
        boolean debugLog = m_felix.getLogger().getLogLevel() >= Logger.LOG_DEBUG;

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
            Map<Resource, List<BundleRevision>> hosts =
                new HashMap<Resource, List<BundleRevision>>();
            for (Entry<Resource, List<Wire>> entry : wireMap.entrySet())
            {
                Resource revision = entry.getKey();
                List<Wire> wires = entry.getValue();

                if (Util.isFragment(revision))
                {
                    for (Iterator<Wire> itWires = wires.iterator();
                        itWires.hasNext(); )
                    {
                        Wire w = itWires.next();
                        List<BundleRevision> fragments = hosts.get(w.getProvider());
                        if (fragments == null)
                        {
                            fragments = new ArrayList<BundleRevision>();
                            hosts.put(w.getProvider(), fragments);
                        }

                        if (w.getRequirer() instanceof BundleRevision)
                            fragments.add((BundleRevision) w.getRequirer());
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
            for (Entry<Resource, List<Wire>> entry : wireMap.entrySet())
            {
                Resource resource = entry.getKey();
                if (!(resource instanceof BundleRevision))
                    continue;

                BundleRevision revision = (BundleRevision) resource;
                List<Wire> resolverWires = entry.getValue();

                List<BundleWire> bundleWires =
                    new ArrayList<BundleWire>(resolverWires.size());

                // Need to special case fragments since they may already have
                // wires if they are already attached to another host; if that
                // is the case, then we want to merge the old host wires with
                // the new ones.
                if ((revision.getWiring() != null) && Util.isFragment(revision))
                {
                    // Fragments only have host wires, so just add them all.
                    bundleWires.addAll(revision.getWiring().getRequiredWires(null));
                }

                // Loop through resolver wires to calculate the package
                // space implied by the wires as well as to record the
                // dependencies.
                Map<String, BundleRevision> importedPkgs =
                    new HashMap<String, BundleRevision>();
                Map<String, List<BundleRevision>> requiredPkgs =
                    new HashMap<String, List<BundleRevision>>();
                for (Wire rw : resolverWires)
                {
                    // TODO is a rw already a BundleWire?
                    // TODO can we optimize this?
                    if (!(rw.getRequirer() instanceof BundleRevision))
                        continue;
                    BundleRevision requirer = (BundleRevision) rw.getRequirer();
                    if (!(rw.getRequirement() instanceof BundleRequirement))
                        continue;
                    BundleRequirement requirement = (BundleRequirement) rw.getRequirement();
                    if (!(rw.getProvider() instanceof BundleRevision))
                        continue;
                    BundleRevision provider = (BundleRevision) rw.getProvider();
                    if (!(rw.getCapability() instanceof BundleCapability))
                        continue;
                    BundleCapability capability = (BundleCapability) rw.getCapability();

                    BundleWire bw = new BundleWireImpl(
                        requirer,
                        requirement,
                        provider,
                        capability);
                    bundleWires.add(bw);

                    if (Util.isFragment(revision))
                    {
                        if (debugLog)
                        {
                            m_felix.getLogger().log(
                                Logger.LOG_DEBUG,
                                "FRAGMENT WIRE: " + rw.toString());
                        }
                    }
                    else
                    {
                        if (debugLog)
                        {
                            m_felix.getLogger().log(Logger.LOG_DEBUG, "WIRE: " + rw.toString());
                        }

                        if (capability.getNamespace()
                            .equals(BundleRevision.PACKAGE_NAMESPACE))
                        {
                            importedPkgs.put(
                                (String) capability.getAttributes()
                                    .get(BundleRevision.PACKAGE_NAMESPACE),
                                provider);
                        }
                        else if (capability.getNamespace()
                            .equals(BundleRevision.BUNDLE_NAMESPACE))
                        {
                            Set<String> pkgs = calculateExportedAndReexportedPackages(
                                    provider,
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
                                revs.add(provider);
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
                BundleWiring wiring = entry.getValue();
                revision.resolve(entry.getValue());

                // Record dependencies.
                for (BundleWire bw : wiring.getRequiredWires(null))
                {
                    m_felix.getDependencies().addDependent(bw);
                }

                // Reindex the revision's capabilities since its resolved
                // capabilities could be different than its declared ones
                // (e.g., due to substitutable exports).
                addRevision(revision);

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

    private void fireResolvedEvents(Map<Resource, List<Wire>> wireMap)
    {
        if (wireMap != null)
        {
            Iterator<Entry<Resource, List<Wire>>> iter =
                wireMap.entrySet().iterator();
            // Iterate over the map to fire necessary RESOLVED events.
            while (iter.hasNext())
            {
                Entry<Resource, List<Wire>> entry = iter.next();
                Resource resource = entry.getKey();
                if (!(resource instanceof BundleRevision))
                    continue;

                BundleRevision revision = (BundleRevision) resource;

                // Fire RESOLVED events for all fragments.
                List<BundleRevision> fragments =
                    Util.getFragments(revision.getWiring());
                for (int i = 0; i < fragments.size(); i++)
                {
                    m_felix.fireBundleEvent(
                        BundleEvent.RESOLVED, fragments.get(i).getBundle());
                }
                m_felix.fireBundleEvent(BundleEvent.RESOLVED, revision.getBundle());
            }
        }
    }

    private static Set<String> calculateExportedAndReexportedPackages(
        BundleRevision br,
        Map<Resource, List<Wire>> wireMap,
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
                for (Wire rw : wireMap.get(br))
                {
                    if (rw.getCapability().getNamespace().equals(
                        BundleRevision.BUNDLE_NAMESPACE))
                    {
                        String dir = rw.getRequirement()
                            .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((dir != null) && (dir.equals(Constants.VISIBILITY_REEXPORT)))
                        {
                            calculateExportedAndReexportedPackages(
                                // TODO need to fix the cast
                                (BundleRevision) rw.getProvider(),
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

    private synchronized void indexCapabilities(BundleRevision br)
    {
        List<BundleCapability> caps =
            (Util.isFragment(br) || (br.getWiring() == null))
                ? br.getDeclaredCapabilities(null)
                : br.getWiring().getCapabilities(null);
        if (caps != null)
        {
            for (BundleCapability cap : caps)
            {
                // If the capability is from a different revision, then
                // don't index it since it is a capability from a fragment.
                // In that case, the fragment capability is still indexed.
                // It will be the resolver's responsibility to find all
                // attached hosts for fragments.
                if (cap.getRevision() == br)
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
        }
    }

    private synchronized void deindexCapabilities(BundleRevision br)
    {
        // We only need be concerned with declared capabilities here,
        // because resolved capabilities will be a subset, since fragment
        // capabilities are not considered to be part of the host.
        List<BundleCapability> caps = br.getDeclaredCapabilities(null);
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
    }

    private synchronized boolean isSelectedSingleton(BundleRevision br)
    {
        return m_selectedSingletons.contains(br);
    }

    private synchronized void selectSingletons(ResolverHookRecord record)
        throws BundleException
    {
        // First deindex any unresolved singletons to make sure
        // there aren't any available from previous resolves.
        // Also remove them from the fragment list, for the same
        // reason.
        m_selectedSingletons.clear();
        for (Entry<String, List<BundleRevision>> entry : m_singletons.entrySet())
        {
            for (BundleRevision singleton : entry.getValue())
            {
                if (singleton.getWiring() == null)
                {
                    deindexCapabilities(singleton);
                    m_fragments.remove(singleton);
                }
            }
        }

        // If no resolver hooks, then use default singleton selection
        // algorithm, otherwise defer to the resolver hooks.
        if (record.getResolverHookRefs().isEmpty())
        {
            selectDefaultSingletons(record);
        }
        else
        {
            selectSingletonsUsingHooks(record);
        }
    }

    /*
     * Selects the singleton with the highest version from groupings
     * based on the symbolic name. No selection is made if the group
     * already has a resolved singleton.
     */
    private void selectDefaultSingletons(ResolverHookRecord record)
    {
        // Now select the singletons available for this resolve operation.
        for (Entry<String, List<BundleRevision>> entry : m_singletons.entrySet())
        {
            selectSingleton(record, entry.getValue());
        }
    }

    /*
     * Groups singletons based on resolver hook filtering and then selects
     * the singleton from each group with the highest version that is in
     * the resolver hook whitelist. No selection is made if a group already
     * has a resolved singleton in it.
     */
    private void selectSingletonsUsingHooks(ResolverHookRecord record)
        throws BundleException
    {
        // Convert singleton bundle revision map into a map using
        // bundle capabilities instead, since this is what the resolver
        // hooks require.
        Map<BundleCapability, Collection<BundleCapability>> allCollisions
            = new HashMap<BundleCapability, Collection<BundleCapability>>();
        for (Entry<String, List<BundleRevision>> entry : m_singletons.entrySet())
        {
            Collection<BundleCapability> bundleCaps =
                new ArrayList<BundleCapability>();
            for (BundleRevision br : entry.getValue())
            {
                List<BundleCapability> caps =
                    br.getDeclaredCapabilities(BundleRevision.BUNDLE_NAMESPACE);
                if (!caps.isEmpty())
                {
                    bundleCaps.add(caps.get(0));
                }
            }

            for (BundleCapability bc : bundleCaps)
            {
                Collection<BundleCapability> capCopy =
                    new ShrinkableCollection<BundleCapability>(
                        new ArrayList<BundleCapability>(bundleCaps));
                capCopy.remove(bc);
                allCollisions.put(bc, capCopy);
            }
        }

        // Invoke hooks to allow them to filter singleton collisions.
        for (ResolverHook hook : record.getResolverHooks())
        {
            for (Entry<BundleCapability, Collection<BundleCapability>> entry
                : allCollisions.entrySet())
            {
                try
                {
                    Felix.m_secureAction
                        .invokeResolverHookSingleton(hook, entry.getKey(), entry.getValue());
                }
                catch (Throwable ex)
                {
                    throw new BundleException(
                        "Resolver hook exception: " + ex.getMessage(),
                        BundleException.REJECTED_BY_HOOK,
                        ex);
                }
            }
        }

        // Create groups according to how the resolver hooks filtered the
        // collisions.
        List<List<BundleRevision>> groups = new ArrayList<List<BundleRevision>>();
        while (!allCollisions.isEmpty())
        {
            BundleCapability target = allCollisions.entrySet().iterator().next().getKey();
            groups.add(groupSingletons(allCollisions, target, new ArrayList<BundleRevision>()));
        }

        // Now select the singletons available for this resolve operation.
        for (List<BundleRevision> group : groups)
        {
            selectSingleton(record, group);
        }
    }

    private List<BundleRevision> groupSingletons(
        Map<BundleCapability, Collection<BundleCapability>> allCollisions,
        BundleCapability target, List<BundleRevision> group)
    {
        if (!group.contains(target.getRevision()))
        {
            // Add the target since it is implicitly part of the group.
            group.add(target.getRevision());

            // Recursively add the revisions of any singleton's in the
            // target's collisions.
            Collection<BundleCapability> collisions = allCollisions.remove(target);
            for (BundleCapability collision : collisions)
            {
                groupSingletons(allCollisions, collision, group);
            }

            // Need to check the values of other collisions for this target
            // and add those to the target's group too, since collisions are
            // treated as two-way relationships. Repeat until there are no
            // collision groups left that contain the target capability.
            boolean repeat;
            do
            {
                repeat = false;
                for (Entry<BundleCapability, Collection<BundleCapability>> entry:
                    allCollisions.entrySet())
                {
                    if (entry.getValue().contains(target))
                    {
                        repeat = true;
                        groupSingletons(allCollisions, entry.getKey(), group);
                        break;
                    }
                }
            }
            while (repeat);
        }
        return group;
    }

    /*
     * Selects the highest bundle revision from the group that is
     * in the resolver hook whitelist (if there are hooks). No
     * selection is made if there is an already resolved singleton
     * in the group, since it is already indexed.
     */
    private void selectSingleton(ResolverHookRecord record, List<BundleRevision> singletons)
    {
        BundleRevision selected = null;
        for (BundleRevision singleton : singletons)
        {
            // If a singleton is already resolved,
            // then there is nothing to do.
            if (singleton.getWiring() != null)
            {
                selected = null;
                break;
            }
            // If this singleton is not in the whitelist, then it cannot
            // be selected. If it is, in can only be selected if it has
            // a higher version than the currently selected singleton, if
            // there is one.
            if (((record.getBundleRevisionWhitelist() == null) || record.getBundleRevisionWhitelist().contains(singleton))
                && ((selected == null)
                    || (selected.getVersion().compareTo(singleton.getVersion()) > 0)))
            {
                selected = singleton;
            }
        }
        if (selected != null)
        {
            // Record the selected singleton.
            m_selectedSingletons.add(selected);
            // Index its capabilities.
            indexCapabilities(selected);
            // If the selected singleton is a fragment, then
            // add it to the list of fragments.
            if (Util.isFragment(selected))
            {
                m_fragments.add(selected);
            }
        }
    }

    private synchronized Set<BundleRevision> getFragments()
    {
        Set<BundleRevision> fragments = new HashSet(m_fragments);
        // Filter out any fragments that are not the current revision.
        for (Iterator<BundleRevision> it = fragments.iterator(); it.hasNext(); )
        {
            BundleRevision fragment = it.next();
            BundleRevision currentFragmentRevision =
                fragment.getBundle().adapt(BundleRevision.class);
            if (fragment != currentFragmentRevision)
            {
                it.remove();
            }
        }
        return fragments;
    }

    void checkNativeLibraries(BundleRevision revision) throws ResolveException
    {
        // Next, try to resolve any native code, since the revision is
        // not resolvable if its native code cannot be loaded.
        List<NativeLibrary> libs = ((BundleRevisionImpl) revision).getDeclaredNativeLibraries();
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

    private synchronized Set<BundleRevision> getUnresolvedRevisions()
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

    private synchronized Map<Resource, Wiring> getWirings()
    {
        Map<Resource, Wiring> wirings = new HashMap<Resource, Wiring>();

        for (BundleRevision revision : m_revisions)
        {
            if (revision.getWiring() != null)
            {
                wirings.put(revision, revision.getWiring());
            }
        }
        return wirings;
    }

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

    private static void addToSingletonMap(
        Map<String, List<BundleRevision>> singletons, BundleRevision br)
    {
        List<BundleRevision> revisions = singletons.get(br.getSymbolicName());
        if (revisions == null)
        {
            revisions = new ArrayList<BundleRevision>();
        }
        revisions.add(br);
        singletons.put(br.getSymbolicName(), revisions);
    }

    static class ResolverHookRecord
    {
        final Map<ServiceReference<ResolverHookFactory>, ResolverHook> m_resolveHookMap;
        final Collection<BundleRevision> m_brWhitelist;

        /** The map passed in must be of an ordered type, so that the iteration order over the values
         * is predictable.
         */
        ResolverHookRecord(Map<ServiceReference<ResolverHookFactory>, ResolverHook> resolveHookMap,
            Collection<BundleRevision> brWhiteList)
        {
            m_resolveHookMap = resolveHookMap;
            m_brWhitelist = brWhiteList;
        }

        Collection<BundleRevision> getBundleRevisionWhitelist()
        {
            return m_brWhitelist;
        }

        Set<ServiceReference<ResolverHookFactory>> getResolverHookRefs()
        {
            return m_resolveHookMap.keySet();
        }

        // This slightly over the top implementation to obtain the hooks is to ensure that at the point that
        // the actual hook is obtained, the service is still registered. There are CT tests that unregister
        // the hook service while iterating over the hooks and expect that the unregistered hook is not called
        // in that case.
        Iterable<ResolverHook> getResolverHooks()
        {
            return new Iterable<ResolverHook>()
            {
                @Override
                public Iterator<ResolverHook> iterator()
                {
                    return new Iterator<ResolverHook>()
                    {
                        private Iterator<Map.Entry<ServiceReference<ResolverHookFactory>, ResolverHook>> it =
                            m_resolveHookMap.entrySet().iterator();
                        private Entry<ServiceReference<ResolverHookFactory>, ResolverHook> next = null;

                        @Override
                        public boolean hasNext()
                        {
                            if (next == null)
                                findNext();

                            return next != null;
                        }

                        @Override
                        public ResolverHook next()
                        {
                            if (next == null)
                                findNext();

                            if (next == null)
                                throw new NoSuchElementException();

                            ResolverHook hook = next.getValue();
                            next = null;
                            return hook;
                        }

                        // Find the next hook on the iterator, but only if the service is still registered.
                        // If the service has since been unregistered, skip the hook.
                        private void findNext()
                        {
                            while (it.hasNext())
                            {
                                next = it.next();
                                if (next.getKey().getBundle() != null)
                                    return;
                                else
                                    next = null;
                            }
                        }

                        @Override
                        public void remove()
                        {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }
    }
}
