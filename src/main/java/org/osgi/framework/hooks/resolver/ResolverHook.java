/*
 * Copyright (c) OSGi Alliance (2010, 2013). All Rights Reserved.
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

package org.osgi.framework.hooks.resolver;

import java.util.Collection;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * OSGi Framework Resolver Hook instances are obtained from the OSGi
 * {@link ResolverHookFactory Framework Resolver Hook Factory} service.
 * 
 * <p>
 * A Resolver Hook instance is called by the framework during a resolve process.
 * A resolver hook may influence the outcome of a resolve process by removing
 * entries from shrinkable collections that are passed to the hook during a
 * resolve process. A shrinkable collection is a {@code Collection} that
 * supports all remove operations. Any other attempts to modify a shrinkable
 * collection will result in an {@code UnsupportedOperationException} being
 * thrown.
 * 
 * <p>
 * The following steps outline the way a framework uses the resolver hooks
 * during a resolve process.
 * <ol>
 * <li>Collect a snapshot of registered resolver hook factories that will be
 * called during the current resolve process. Any hook factories registered
 * after the snapshot is taken must not be called during the current resolve
 * process. A resolver hook factory contained in the snapshot may become
 * unregistered during the resolve process. The framework should handle this and
 * stop calling the resolver hook instance provided by the unregistered hook
 * factory and the current resolve process must fail. If possible, an exception
 * must be thrown to the caller of the API which triggered the resolve process.
 * In cases where the the caller is not available a framework event of type
 * error should be fired.</li>
 * 
 * <li>For each registered hook factory call the
 * {@link ResolverHookFactory#begin(Collection)} method to inform the hooks
 * about a resolve process beginning and to obtain a Resolver Hook instance that
 * will be used for the duration of the resolve process.</li>
 * 
 * <li>Determine the collection of unresolved bundle revisions that may be
 * considered for resolution during the current resolution process and place
 * each of the bundle revisions in a shrinkable collection {@code Resolvable}.
 * For each resolver hook call the {@link #filterResolvable(Collection)} method
 * with the shrinkable collection {@code Resolvable}.</li>
 * <li>The shrinkable collection {@code Resolvable} now contains all the
 * unresolved bundle revisions that may end up as resolved at the end of the
 * current resolve process. Any other bundle revisions that got removed from the
 * shrinkable collection {@code Resolvable} must not end up as resolved at the
 * end of the current resolve process.</li>
 * <li>For each bundle revision {@code B} left in the shrinkable collection
 * {@code Resolvable} and any bundle revision {@code B} which is currently
 * resolved that represents a singleton bundle do the following:
 * <ul>
 * <li>Determine the collection of available capabilities that have a namespace
 * of {@link IdentityNamespace osgi.identity}, are singletons, and have the same
 * symbolic name as the singleton bundle revision {@code B} and place each of
 * the matching capabilities into a shrinkable collection {@code Collisions}.</li>
 * <li>Remove the {@link IdentityNamespace osgi.identity} capability provided by
 * bundle revision {@code B} from shrinkable collection {@code Collisions}. A
 * singleton bundle cannot collide with itself.</li>
 * <li>For each resolver hook call the
 * {@link #filterSingletonCollisions(BundleCapability, Collection)} with the
 * {@link IdentityNamespace osgi.identity} capability provided by bundle
 * revision {@code B} and the shrinkable collection {@code Collisions}</li>
 * <li>The shrinkable collection {@code Collisions} now contains all singleton
 * {@link IdentityNamespace osgi.identity} capabilities that can influence the
 * ability of bundle revision {@code B} to resolve.</li>
 * <li>If the bundle revision {@code B} is already resolved then any resolvable
 * bundle revision contained in the collection {@code Collisions} is not allowed
 * to resolve.</li>
 * </ul>
 * </li>
 * <li>During a resolve process a framework is free to attempt to resolve any or
 * all bundles contained in shrinkable collection {@code Resolvable}. For each
 * bundle revision {@code B} left in the shrinkable collection
 * {@code Resolvable} which the framework attempts to resolve the following
 * steps must be followed:
 * <ul>
 * <li>For each requirement {@code R} specified by bundle revision {@code B}
 * determine the collection of capabilities that satisfy (or match) the
 * requirement and place each matching capability into a shrinkable collection
 * {@code Candidates}. A capability is considered to match a particular
 * requirement if its attributes satisfy a specified requirement and the
 * requirer bundle has permission to access the capability.</li>
 * <li>For each resolver hook call the
 * {@link #filterMatches(BundleRequirement, Collection)} with the requirement
 * {@code R} and the shrinkable collection {@code Candidates}.</li>
 * <li>The shrinkable collection {@code Candidates} now contains all the
 * capabilities that may be used to satisfy the requirement {@code R}. Any other
 * capabilities that got removed from the shrinkable collection
 * {@code Candidates} must not be used to satisfy requirement {@code R}.</li>
 * </ul>
 * </li>
 * <li>For each resolver hook call the {@link #end()} method to inform the hooks
 * about a resolve process ending.</li>
 * </ol>
 * In all cases, the order in which the resolver hooks are called is the reverse
 * compareTo ordering of their Service References. That is, the service with the
 * highest ranking number must be called first. In cases where a shrinkable
 * collection becomes empty the framework is required to call the remaining
 * registered hooks.
 * <p>
 * Resolver hooks are low level. Implementations of the resolver hook must be
 * careful not to create an unresolvable state which is very hard for a
 * developer or a provisioner to diagnose. Resolver hooks also must not be
 * allowed to start another synchronous resolve process (e.g. by calling
 * {@link Bundle#start()} or {@link FrameworkWiring#resolveBundles(Collection)}
 * ). The framework must detect this and throw an {@link IllegalStateException}.
 * 
 * @see ResolverHookFactory
 * @NotThreadSafe
 * @author $Id: 7b2a0a5dbec7b0e999112ae324d050fcf190fa5d $
 */
@ConsumerType
public interface ResolverHook {
	/**
	 * Filter resolvable candidates hook method. This method may be called
	 * multiple times during a single resolve process. This method can filter
	 * the collection of candidates by removing potential candidates. Removing a
	 * candidate will prevent the candidate from resolving during the current
	 * resolve process.
	 * 
	 * @param candidates the collection of resolvable candidates available
	 *        during a resolve process.
	 */
	void filterResolvable(Collection<BundleRevision> candidates);

	/**
	 * Filter singleton collisions hook method. This method is called during the
	 * resolve process for the specified singleton. The specified singleton
	 * represents a singleton capability and the specified collection represent
	 * a collection of singleton capabilities which are considered collision
	 * candidates. The singleton capability and the collection of collision
	 * candidates must all use the same namespace.
	 * <p>
	 * Currently only capabilities with the namespace of {@link BundleNamespace
	 * osgi.wiring.bundle} and {@link IdentityNamespace osgi.identity} can be
	 * singletons. The collision candidates will all have the same namespace, be
	 * singletons, and have the same symbolic name as the specified singleton
	 * capability.
	 * <p>
	 * In the future, capabilities in other namespaces may support the singleton
	 * concept. Hook implementations should be prepared to receive calls to this
	 * method for capabilities in namespaces other than {@link BundleNamespace
	 * osgi.wiring.bundle} or {@link IdentityNamespace osgi.identity}.
	 * <p>
	 * This method can filter the list of collision candidates by removing
	 * potential collisions. Removing a collision candidate will allow the
	 * specified singleton to resolve regardless of the resolution state of the
	 * removed collision candidate.
	 * 
	 * @param singleton the singleton involved in a resolve process
	 * @param collisionCandidates a collection of singleton collision candidates
	 */
	void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates);

	/**
	 * Filter matches hook method. This method is called during the resolve
	 * process for the specified requirement. The collection of candidates match
	 * the specified requirement. This method can filter the collection of
	 * matching candidates by removing candidates from the collection. Removing
	 * a candidate will prevent the resolve process from choosing the removed
	 * candidate to satisfy the requirement.
	 * <p>
	 * All of the candidates will have the same namespace and will match the
	 * specified requirement.
	 * <p>
	 * If the Java Runtime Environment supports permissions then the collection
	 * of candidates will only contain candidates for which the requirer has
	 * permission to access.
	 * 
	 * @param requirement the requirement to filter candidates for
	 * @param candidates a collection of candidates that match the requirement
	 */
	void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates);

	/**
	 * This method is called once at the end of the resolve process. After the
	 * end method is called the resolve process has ended. The framework must
	 * not hold onto this resolver hook instance after end has been called.
	 */
	void end();
}
