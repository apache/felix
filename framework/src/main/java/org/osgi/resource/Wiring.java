/*
 * Copyright (c) OSGi Alliance (2011, 2013). All Rights Reserved.
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

package org.osgi.resource;

import java.util.List;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A wiring for a resource. A wiring is associated with a resource and
 * represents the dependencies with other wirings.
 * 
 * <p>
 * Instances of this type must be <i>effectively immutable</i>. That is, for a
 * given instance of this interface, the methods defined by this interface must
 * always return the same result.
 * 
 * @ThreadSafe
 * @author $Id: 935272fa914a9cc0020549c43a3857acad2c45cc $
 */
@ConsumerType
public interface Wiring {
	/**
	 * Returns the capabilities provided by this wiring.
	 * 
	 * <p>
	 * Only capabilities considered by the resolver are returned. For example,
	 * capabilities with {@link Namespace#CAPABILITY_EFFECTIVE_DIRECTIVE
	 * effective} directive not equal to {@link Namespace#EFFECTIVE_RESOLVE
	 * resolve} are not returned.
	 * 
	 * <p>
	 * A capability may not be required by any wiring and thus there may be no
	 * {@link #getProvidedResourceWires(String) wires} for the capability.
	 * 
	 * <p>
	 * A wiring for a non-fragment resource provides a subset of the declared
	 * capabilities from the resource and all attached fragment
	 * resources<sup>&#8224;</sup>. Not all declared capabilities may be
	 * provided since some may be discarded. For example, if a package is
	 * declared to be both exported and imported, only one is selected and the
	 * other is discarded.
	 * <p>
	 * A wiring for a fragment resource with a symbolic name must provide
	 * exactly one {@code osgi.identity} capability.
	 * <p>
	 * &#8224; The {@code osgi.identity} capability provided by attached
	 * fragment resource must not be included in the capabilities of the host
	 * wiring.
	 * 
	 * @param namespace The namespace of the capabilities to return or
	 *        {@code null} to return the capabilities from all namespaces.
	 * @return A list containing a snapshot of the {@link Capability}s, or an
	 *         empty list if this wiring provides no capabilities in the
	 *         specified namespace. For a given namespace, the list contains the
	 *         capabilities in the order the capabilities were specified in the
	 *         manifests of the {@link #getResource() resource} and the attached
	 *         fragment resources<sup>&#8224;</sup> of this wiring. There is no
	 *         ordering defined between capabilities in different namespaces.
	 */
	List<Capability> getResourceCapabilities(String namespace);

	/**
	 * Returns the requirements of this wiring.
	 * 
	 * <p>
	 * Only requirements considered by the resolver are returned. For example,
	 * requirements with {@link Namespace#REQUIREMENT_EFFECTIVE_DIRECTIVE
	 * effective} directive not equal to {@link Namespace#EFFECTIVE_RESOLVE
	 * resolve} are not returned.
	 * 
	 * <p>
	 * A wiring for a non-fragment resource has a subset of the declared
	 * requirements from the resource and all attached fragment resources. Not
	 * all declared requirements may be present since some may be discarded. For
	 * example, if a package is declared to be optionally imported and is not
	 * actually imported, the requirement must be discarded.
	 * 
	 * @param namespace The namespace of the requirements to return or
	 *        {@code null} to return the requirements from all namespaces.
	 * @return A list containing a snapshot of the {@link Requirement}s, or an
	 *         empty list if this wiring uses no requirements in the specified
	 *         namespace. For a given namespace, the list contains the
	 *         requirements in the order the requirements were specified in the
	 *         manifests of the {@link #getResource() resource} and the attached
	 *         fragment resources of this wiring. There is no ordering defined
	 *         between requirements in different namespaces.
	 */
	List<Requirement> getResourceRequirements(String namespace);

	/**
	 * Returns the {@link Wire}s to the provided {@link Capability capabilities}
	 * of this wiring.
	 * 
	 * @param namespace The namespace of the capabilities for which to return
	 *        wires or {@code null} to return the wires for the capabilities in
	 *        all namespaces.
	 * @return A list containing a snapshot of the {@link Wire}s for the
	 *         {@link Capability capabilities} of this wiring, or an empty list
	 *         if this wiring has no capabilities in the specified namespace.
	 *         For a given namespace, the list contains the wires in the order
	 *         the capabilities were specified in the manifests of the
	 *         {@link #getResource() resource} and the attached fragment
	 *         resources of this wiring. There is no ordering defined between
	 *         capabilities in different namespaces.
	 */
	List<Wire> getProvidedResourceWires(String namespace);

	/**
	 * Returns the {@link Wire}s to the {@link Requirement requirements} in use
	 * by this wiring.
	 * 
	 * @param namespace The namespace of the requirements for which to return
	 *        wires or {@code null} to return the wires for the requirements in
	 *        all namespaces.
	 * @return A list containing a snapshot of the {@link Wire}s for the
	 *         {@link Requirement requirements} of this wiring, or an empty list
	 *         if this wiring has no requirements in the specified namespace.
	 *         For a given namespace, the list contains the wires in the order
	 *         the requirements were specified in the manifests of the
	 *         {@link #getResource() resource} and the attached fragment
	 *         resources of this wiring. There is no ordering defined between
	 *         requirements in different namespaces.
	 */
	List<Wire> getRequiredResourceWires(String namespace);

	/**
	 * Returns the resource associated with this wiring.
	 * 
	 * @return The resource associated with this wiring.
	 */
	Resource getResource();
}
