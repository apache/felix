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
package org.apache.felix.resolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.ResolverImpl.Blame;
import org.apache.felix.resolver.ResolverImpl.Packages;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.ResolveContext;

public class CapabilityFinder {
    private Map<Capability, Set<Capability>> packageSourcesCache = new HashMap<Capability, Set<Capability>>(256);
    private ResolveContext context;
    
    public CapabilityFinder(ResolveContext context) {
        this.context = context;
    }
    
    public Set<Capability> getPackageSources(Capability cap, Map<Resource, Packages> resourcePkgMap)
    {
        // If it is a package, then calculate sources for it.
        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            Set<Capability> sources = packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(context, cap, resourcePkgMap,
                        new HashSet<Capability>(64), new HashSet<Capability>(64));
                packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        // Otherwise, need to return generic capabilies that have
        // uses constraints so they are included for consistency
        // checking.
        String uses = cap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
        if ((uses != null) && (uses.length() > 0))
        {
            return Collections.singleton(cap);
        }

        return Collections.emptySet();
    }

    private static Set<Capability> getPackageSourcesInternal(
            ResolveContext rc, 
            Capability cap, 
            Map<Resource, Packages> resourcePkgMap,
            Set<Capability> sources, 
            Set<Capability> cycleMap)
    {
        if (!cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE) || !cycleMap.add(cap))
        {
            return sources;
        }

        // Get the package name associated with the capability.
        String pkgName = cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString();

        // Since a resource can export the same package more than once, get
        // all package capabilities for the specified package name.
        Wiring wiring = rc.getWirings().get(cap.getResource());
        List<Capability> caps = (wiring != null)
                ? wiring.getResourceCapabilities(null)
                : cap.getResource().getCapabilities(null);
        for (Capability sourceCap : caps)
        {
            if (sourceCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
               && sourceCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).equals(pkgName))
            {
                // Since capabilities may come from fragments, we need to check
                // for that case and wrap them.
                if (!cap.getResource().equals(sourceCap.getResource()))
                {
                    sourceCap = new WrappedCapability(cap.getResource(), sourceCap);
                }
                sources.add(sourceCap);
            }
        }

        // Then get any addition sources for the package from required bundles.
        Packages pkgs = resourcePkgMap.get(cap.getResource());
        List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
        if (required != null)
        {
            for (Blame blame : required)
            {
                getPackageSourcesInternal(rc, blame.m_cap, resourcePkgMap, sources, cycleMap);
            }
        }

        return sources;
    }
    
    public void clear() {
        packageSourcesCache.clear();
    }
}
