/*
 * Copyright (c) OSGi Alliance (2006, 2017). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This document is an experimental draft to enable interoperability
// between bundle repositories. There is currently no commitment to
// turn this draft into an official specification.

package org.osgi.service.resolver;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;

/**
 * A resolver service resolves the specified resources in the context supplied
 * by the caller.
 * 
 * @ThreadSafe
 * @author $Id: 86bff007315e1e3c03eca6aaacdcfa379be9ddea $
 */
@ProviderType
public interface Resolver {
	/**
	 * Resolve the specified resolve context and return any new resources and
	 * wires to the caller.
	 * 
	 * <p>
	 * The resolver considers two groups of resources:
	 * <ul>
	 * <li>Mandatory - any resource in the
	 * {@link ResolveContext#getMandatoryResources() mandatory group} must be
	 * resolved. A failure to satisfy any mandatory requirement for these
	 * resources will result in throwing a {@link ResolutionException}</li>
	 * <li>Optional - any resource in the
	 * {@link ResolveContext#getOptionalResources() optional group} may be
	 * resolved. A failure to satisfy a mandatory requirement for a resource in
	 * this group will not fail the overall resolution but no resources or wires
	 * will be returned for that resource.</li>
	 * </ul>
	 * 
	 * <p>
	 * The resolve method returns the delta between the start state defined by
	 * {@link ResolveContext#getWirings()} and the end resolved state. That is,
	 * only new resources and wires are included.
	 * 
	 * <p>
	 * The behavior of the resolver is not defined if the specified resolve
	 * context supplies inconsistent information.
	 * 
	 * @param context The resolve context for the resolve operation. Must not be
	 *        {@code null}.
	 * @return The new resources and wires required to satisfy the specified
	 *         resolve context. The returned map is the property of the caller
	 *         and can be modified by the caller.
	 * @throws ResolutionException If the resolution cannot be satisfied.
	 */
	Map<Resource, List<Wire>> resolve(ResolveContext context) throws ResolutionException;

	/**
	 * Resolves a given requirement dynamically for the given host wiring using
	 * the given resolve context and return any new resources and wires to the
	 * caller.
	 * <p>
	 * The requirement must be a {@link Wiring#getResourceRequirements(String)
	 * requirement} of the wiring and must use the
	 * {@link PackageNamespace#PACKAGE_NAMESPACE package} namespace with a
	 * {@link Namespace#REQUIREMENT_RESOLUTION_DIRECTIVE resolution} of type
	 * {@link PackageNamespace#RESOLUTION_DYNAMIC dynamic}.
	 * <p>
	 * The resolve context is not asked for
	 * {@link ResolveContext#getMandatoryResources() mandatory} resources or for
	 * {@link ResolveContext#getMandatoryResources() optional} resources. The
	 * resolve context is asked to
	 * {@link ResolveContext#findProviders(Requirement) find providers} for the
	 * given requirement. The matching {@link PackageNamespace#PACKAGE_NAMESPACE
	 * package} capabilities returned by the resolve context must not have a
	 * {@link PackageNamespace#PACKAGE_NAMESPACE osgi.wiring.package} attribute
	 * equal to a {@link PackageNamespace#PACKAGE_NAMESPACE package} capability
	 * already {@link Wiring#getRequiredResourceWires(String) wired to} by the
	 * wiring or equal a {@link PackageNamespace#PACKAGE_NAMESPACE package}
	 * capability {@link Wiring#getResourceCapabilities(String) provided} by the
	 * wiring. The resolve context may be requested to
	 * {@link ResolveContext#findProviders(Requirement) find providers} for
	 * other requirements in order to resolve the resources that provide the
	 * matching capabilities to the given requirement.
	 * <p>
	 * If the requirement {@link Namespace#REQUIREMENT_CARDINALITY_DIRECTIVE
	 * cardinality} is not {@link Namespace#CARDINALITY_MULTIPLE multiple} then
	 * no new wire must be created if the
	 * {@link Wiring#getRequiredResourceWires(String) wires} of the wiring
	 * already contain a wire that uses the {@link Wire#getRequirement()
	 * requirement}
	 * <p>
	 * This operation may resolve additional resources in order to resolve the
	 * dynamic requirement. The returned map will contain entries for each
	 * resource that got resolved in addition to the specified wiring
	 * {@link Wiring#getResource() resource}. The wire list for the wiring
	 * resource will only contain one wire which is for the dynamic requirement.
	 * 
	 * @param context The resolve context for the resolve operation. Must not be
	 *            {@code null}.
	 * @param hostWiring The wiring with the dynamic
	 *            {@link Wiring#getResourceRequirements(String) requirement}.
	 *            Must not be {@code null}.
	 * @param dynamicRequirement The dynamic requirement. Must not be
	 *            {@code null}.
	 * @return The new resources and wires required to satisfy the specified
	 *         dynamic requirement. The returned map is the property of the
	 *         caller and can be modified by the caller. If no new wires were
	 *         created then a ResolutionException is thrown.
	 * @throws ResolutionException if the dynamic requirement cannot be resolved
	 */
	public Map<Resource,List<Wire>> resolveDynamic(ResolveContext context,
			Wiring hostWiring, Requirement dynamicRequirement)
			throws ResolutionException;
}
