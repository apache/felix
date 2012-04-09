/*
 * Copyright (c) OSGi Alliance (2011, 2012). All Rights Reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;

/**
 * A resolve context provides resources, options and constraints to the
 * potential solution of a {@link Resolver#resolve(ResolveContext) resolve}
 * operation.
 * 
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
 * 
 * <p>
 * A resolver may call the methods on the resolve context any number of times
 * during a resolve operation using any thread. Implementors should ensure that
 * this class is properly thread safe.
 * 
 * <p>
 * Except for {@link #insertHostedCapability(List, HostedCapability)}, the
 * resolve context methods must be <i>idempotent</i>. This means that resources
 * must have constant capabilities and requirements and the resolve context must
 * return a consistent set of capabilities, wires and effective requirements.
 * 
 * @ThreadSafe
 * @version $Id: f92eae32ab6fadb25e13d226458d6af50e8dcbba $
 */
public abstract class ResolveContext {
	/**
	 * Return the resources that must be resolved for this resolve context.
	 * 
	 * <p>
	 * The default implementation returns an empty collection.
	 * 
	 * @return The resources that must be resolved for this resolve context. May
	 *         be empty if there are no mandatory resources.
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
	 * @return The resources that the resolver should attempt to resolve for
	 *         this resolve context. May be empty if there are no mandatory
	 *         resources.
	 */
	public Collection<Resource> getOptionalResources() {
		return emptyCollection();
	}

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
	 * Each returned Capability must match the given Requirement. This implies
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
}
