/*
 * Copyright (c) OSGi Alliance (2011, 2017). All Rights Reserved.
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

package org.osgi.service.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;

/**
 * A resolve context provides resources, options and constraints to the
 * potential solution of a {@link Resolver#resolve(ResolveContext) resolve}
 * operation.
 * <p>
 * Resolve Contexts:
 * <ul>
 * <li>Specify the mandatory and optional resources to resolve. The mandatory
 * and optional resources must be consistent and correct. For example, they must
 * not violate the singleton policy of the implementer.</li>
 * <li>Provide {@link Capability capabilities} that the Resolver can use to
 * satisfy {@link Requirement requirements} via the
 * {@link #findProviders(Requirement)} method</li>
 * <li>Constrain solutions via the {@link #getWirings()} method. A wiring
 * consists of a map of existing {@link Resource resources} to {@link Wiring
 * wiring}.</li>
 * <li>Filter requirements that are part of a resolve operation via the
 * {@link #isEffective(Requirement)}.</li>
 * </ul>
 * <p>
 * A resolver may call the methods on the resolve context any number of times
 * during a resolve operation using any thread. Implementors should ensure that
 * this class is properly thread safe.
 * <p>
 * Except for {@link #insertHostedCapability(List, HostedCapability)} and
 * {@link #onCancel(Runnable)}, the resolve context methods must be
 * <i>idempotent</i>. This means that resources must have constant capabilities
 * and requirements and the resolve context must return a consistent set of
 * capabilities, wires and effective requirements.
 * 
 * @ThreadSafe
 * @author $Id: 5a3d32eb947b703bcca36f00d1ba6a86ae4a8e4b $
 */
@ConsumerType
public abstract class ResolveContext {
	/**
	 * Return the resources that must be resolved for this resolve context.
	 * 
	 * <p>
	 * The default implementation returns an empty collection.
	 * 
	 * @return A collection of the resources that must be resolved for this
	 *         resolve context. May be empty if there are no mandatory
	 *         resources. The returned collection may be unmodifiable.
	 */
	public Collection<Resource> getMandatoryResources() {
		return emptyCollection();
	}

	/**
	 * Return the resources that the resolver should attempt to resolve for this
	 * resolve context. Inability to resolve one of the specified resources will
	 * not result in a resolution exception.
	 * 
	 * <p>
	 * The default implementation returns an empty collection.
	 * 
	 * @return A collection of the resources that the resolver should attempt to
	 *         resolve for this resolve context. May be empty if there are no
	 *         optional resources. The returned collection may be unmodifiable.
	 */
	public Collection<Resource> getOptionalResources() {
		return emptyCollection();
	}

	@SuppressWarnings("unchecked")
	private static <T> Collection<T> emptyCollection() {
		return Collections.EMPTY_LIST;
	}

	/**
	 * Find Capabilities that match the given Requirement.
	 * <p>
	 * The returned list contains {@link Capability} objects where the Resource
	 * must be the declared Resource of the Capability. The Resolver can then
	 * add additional {@link HostedCapability} objects with the
	 * {@link #insertHostedCapability(List, HostedCapability)} method when it,
	 * for example, attaches fragments. Those {@link HostedCapability} objects
	 * will then use the host's Resource which likely differs from the declared
	 * Resource of the corresponding Capability.
	 * 
	 * <p>
	 * The returned list is in priority order such that the Capabilities with a
	 * lower index have a preference over those with a higher index. The
	 * resolver must use the
	 * {@link #insertHostedCapability(List, HostedCapability)} method to add
	 * additional Capabilities to maintain priority order. In general, this is
	 * necessary when the Resolver uses Capabilities declared in a Resource but
	 * that must originate from an attached host.
	 * 
	 * <p>
	 * Each returned Capability must match the given Requirement. This means
	 * that the filter in the Requirement must match as well as any namespace
	 * specific directives. For example, the mandatory attributes for the
	 * {@code osgi.wiring.package} namespace.
	 * 
	 * @param requirement The requirement that a resolver is attempting to
	 *        satisfy. Must not be {@code null}.
	 * @return A list of {@link Capability} objects that match the specified
	 *         requirement.
	 */
	public abstract List<Capability> findProviders(Requirement requirement);

	/**
	 * Add a {@link HostedCapability} to the list of capabilities returned from
	 * {@link #findProviders(Requirement)}.
	 * 
	 * <p>
	 * This method is used by the {@link Resolver} to add Capabilities that are
	 * hosted by another Resource to the list of Capabilities returned from
	 * {@link #findProviders(Requirement)}. This function is necessary to allow
	 * fragments to attach to hosts, thereby changing the origin of a
	 * Capability. This method must insert the specified HostedCapability in a
	 * place that makes the list maintain the preference order. It must return
	 * the index in the list of the inserted {@link HostedCapability}.
	 * 
	 * @param capabilities The list returned from
	 *        {@link #findProviders(Requirement)}. Must not be {@code null}.
	 * @param hostedCapability The HostedCapability to insert in the specified
	 *        list. Must not be {@code null}.
	 * @return The index in the list of the inserted HostedCapability.
	 * 
	 */
	public abstract int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability);

	/**
	 * Test if a given requirement should be wired in the resolve operation. If
	 * this method returns {@code false}, then the resolver should ignore this
	 * requirement during the resolve operation.
	 * 
	 * <p>
	 * The primary use case for this is to test the {@code effective} directive
	 * on the requirement, though implementations are free to use any effective
	 * test.
	 * 
	 * @param requirement The Requirement to test. Must not be {@code null}.
	 * @return {@code true} if the requirement should be considered as part of
	 *         the resolve operation.
	 */
	public abstract boolean isEffective(Requirement requirement);

	/**
	 * Returns the wirings for existing resolved resources.
	 * 
	 * <p>
	 * For example, if this resolve context is for an OSGi framework, then the
	 * result would contain all the currently resolved bundles with each
	 * bundle's current wiring.
	 * 
	 * <p>
	 * Multiple calls to this method for this resolve context must return the
	 * same result.
	 * 
	 * @return The wirings for existing resolved resources. The returned map is
	 *         unmodifiable.
	 */
	public abstract Map<Resource, Wiring> getWirings();

	/**
	 * Find resources that are related to the given resource.
	 * <p>
	 * The resolver attempts to resolve related resources during the current
	 * resolve operation. Failing to resolve one of the related resources will
	 * not result in a resolution exception unless the related resource is also
	 * a {@link #getMandatoryResources() mandatory} resource.
	 * <p>
	 * The resolve context is asked to return related resources for each
	 * resource that is pulled into a resolve operation. This includes the
	 * {@link #getMandatoryResources() mandatory} and
	 * {@link #getOptionalResources() optional} resources and each related
	 * resource returned by this method.
	 * <p>
	 * For example, a fragment can be considered a related resource for a host
	 * bundle. When a host is being resolved the resolve context will be asked
	 * if any related resources should be added to the resolve operation. The
	 * resolve context may decide that the potential fragments of the host
	 * should be resolved along with the host.
	 * 
	 * @param resource The Resource that a resolver is attempting to find
	 *            related resources for. Must not be {@code null}.
	 * @return A collection of the resources that the resolver should attempt to
	 *         resolve for this resolve context. May be empty if there are no
	 *         related resources. The returned collection may be unmodifiable.
	 * @since 1.1
	 */
	public Collection<Resource> findRelatedResources(Resource resource) {
		return Collections.emptyList();
	}

	/**
	 * Registers a callback with the resolve context that is associated with the
	 * currently running resolve operation. The callback can be executed in
	 * order to cancel the currently running resolve operation.
	 * <p>
	 * When a resolve operation begins, the resolver must call this method once
	 * and only once for the duration of the resolve operation and that call
	 * must happen before calling any other method on this resolve context. If
	 * the specified callback is executed then the resolver must cancel the
	 * currently running resolve operation and throw a
	 * {@link ResolutionException} with a cause of type
	 * {@link CancellationException}.
	 * <p>
	 * The callback allows a resolve context to cancel a long running resolve
	 * operation that appears to be running endlessly or at risk of running out
	 * of resources. The resolve context may then decide to give up on resolve
	 * operation or attempt to try another resolve operation with a smaller set
	 * of resources which may allow the resolve operation to complete normally.
	 * 
	 * @param callback the callback to execute in order to cancel the resolve
	 *            operation. Must not be {@code null}.
	 * @throws IllegalStateException if the resolver attempts to register more
	 *             than one callback for a resolve operation
	 * @since 1.1
	 */
	public void onCancel(Runnable callback) {
		// do nothing by default
	}

	/**
	 * Returns the subset of {@link Wiring#getRequiredResourceWires(String)
	 * required} wires that provide wires to {@link Capability capabilities}
	 * which substitute capabilities of the wiring. For example, when a
	 * {@link PackageNamespace package} name is both provided and required by
	 * the same resource. If the package requirement is resolved to a capability
	 * provided by a different wiring then the package capability is considered
	 * to be substituted.
	 * <p>
	 * The resolver asks the resolve context to return substitution wires for
	 * each wiring that {@link Wiring#getResourceCapabilities(String) provides}
	 * a {@link BundleNamespace bundle} namespace capability that is used to
	 * resolve one or more bundle requirements.
	 * <p>
	 * Note that this method searches all the {@link PackageNamespace package}
	 * capabilities declared as {@link Resource#getCapabilities(String)
	 * provided} by the resource associated with the wiring and fragment
	 * resources wired to the wiring with the {@link HostNamespace host}
	 * namespace. The provided package names are compared against the
	 * {@link Wiring#getRequiredResourceWires(String) required} package wires to
	 * determine which wires are substitution wires. Subclasses of
	 * <code>ResolveContext</code> should provide a more efficient
	 * implementation of this method.
	 *
	 * @param wiring the wiring to get the substitution wires for. Must not be
	 *            {@code null}.
	 * @return A list containing a snapshot of the substitution {@link Wire}s
	 *         for the {@link Requirement requirements} of the wiring, or an
	 *         empty list if the wiring has no substitution wires. The list
	 *         contains the wires in the order they are found in the
	 *         {@link Wiring#getRequiredResourceWires(String) required} wires of
	 *         the wiring.
	 * @since 1.1
	 */
	public List<Wire> getSubstitutionWires(Wiring wiring) {
		// Keep track of the declared capability package names
		Set<String> exportNames = new HashSet<String>();

		// Add packages declared as provided by the wiring host
		for (Capability cap : wiring.getResource().getCapabilities(null)) {
			if (PackageNamespace.PACKAGE_NAMESPACE.equals(cap.getNamespace())) {
				exportNames.add((String) cap.getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE));
			}
		}

		// Add packages declared as provided by the attached fragments
		for (Wire wire : wiring.getProvidedResourceWires(null)) {
			if (HostNamespace.HOST_NAMESPACE
					.equals(wire.getCapability().getNamespace())) {
				Resource fragment = wire.getRequirement().getResource();
				for (Capability cap : fragment.getCapabilities(null)) {
					if (PackageNamespace.PACKAGE_NAMESPACE
							.equals(cap.getNamespace())) {
						exportNames.add((String) cap.getAttributes()
								.get(PackageNamespace.PACKAGE_NAMESPACE));
					}
				}
			}
		}

		// collect the package wires that substitute one of the declared
		// export package names
		List<Wire> substitutionWires = new ArrayList<Wire>();
		for (Wire wire : wiring.getRequiredResourceWires(null)) {
			if (PackageNamespace.PACKAGE_NAMESPACE
					.equals(wire.getCapability().getNamespace())) {
				if (exportNames
						.contains(wire.getCapability().getAttributes().get(
								PackageNamespace.PACKAGE_NAMESPACE))) {
					substitutionWires.add(wire);
				}
			}
		}
		return substitutionWires;
	}
}
