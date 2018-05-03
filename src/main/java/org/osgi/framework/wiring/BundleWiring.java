/*
 * Copyright (c) OSGi Alliance (2010, 2016). All Rights Reserved.
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

package org.osgi.framework.wiring;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;

/**
 * A wiring for a bundle. Each time a bundle is resolved, a new bundle wiring
 * for the bundle is created. A bundle wiring is associated with a bundle
 * revision and represents the dependencies with other bundle wirings.
 * 
 * <p>
 * The bundle wiring for a bundle is the {@link #isCurrent() current} bundle
 * wiring if it is the most recent bundle wiring for the current bundle
 * revision. A bundle wiring is {@link #isInUse() in use} if it is the current
 * bundle wiring or if some other in use bundle wiring is dependent upon it. For
 * example, another bundle wiring is wired to a capability provided by the
 * bundle wiring. An in use bundle wiring for a non-fragment bundle has a class
 * loader. All bundles with non-current, in use bundle wirings are considered
 * removal pending. Once a bundle wiring is no longer in use, it is considered
 * stale and is discarded by the framework.
 * 
 * <p>
 * The current bundle wiring for a bundle can be obtained by calling
 * {@link Bundle#adapt(Class) bundle.adapt}(BundleWiring.class). A bundle in the
 * INSTALLED or UNINSTALLED state does not have a current wiring, adapting such
 * a bundle returns {@code null}.
 * 
 * @ThreadSafe
 * @author $Id: ddad9df28bbd85405057e1f1237f404f83ac8758 $
 */
@ProviderType
public interface BundleWiring extends BundleReference, Wiring {
	/**
	 * Returns {@code true} if this bundle wiring is the current bundle wiring.
	 * The bundle wiring for a bundle is the current bundle wiring if it is the
	 * most recent bundle wiring for the current bundle revision. All bundles
	 * with non-current, in use bundle wirings are considered
	 * {@link FrameworkWiring#getRemovalPendingBundles() removal pending}.
	 * 
	 * @return {@code true} if this bundle wiring is the current bundle wiring;
	 *         {@code false} otherwise.
	 */
	boolean isCurrent();

	/**
	 * Returns {@code true} if this bundle wiring is in use. A bundle wiring is
	 * in use if it is the {@link #isCurrent() current} wiring or if some other
	 * in use bundle wiring is dependent upon it. Once a bundle wiring is no
	 * longer in use, it is considered stale and is discarded by the framework.
	 * 
	 * @return {@code true} if this bundle wiring is in use; {@code false}
	 *         otherwise.
	 */
	boolean isInUse();

	/**
	 * Returns the capabilities provided by this bundle wiring.
	 * 
	 * <p>
	 * Only capabilities considered by the resolver are returned. For example,
	 * capabilities with {@link Namespace#CAPABILITY_EFFECTIVE_DIRECTIVE
	 * effective} directive not equal to {@link Namespace#EFFECTIVE_RESOLVE
	 * resolve} are not returned.
	 * 
	 * <p>
	 * A capability may not be required by any bundle wiring and thus there may
	 * be no {@link #getProvidedWires(String) wires} for the capability.
	 * 
	 * <p>
	 * A bundle wiring for a non-fragment revision provides a subset of the
	 * declared capabilities from the bundle revision and all attached fragment
	 * revisions<sup>&#8224;</sup>. Not all declared capabilities may be
	 * provided since some may be discarded. For example, if a package is
	 * declared to be both exported and imported, only one is selected and the
	 * other is discarded.
	 * <p>
	 * A bundle wiring for a fragment revision with a symbolic name must provide
	 * exactly one {@link IdentityNamespace identity} capability.
	 * <p>
	 * &#8224; The {@link IdentityNamespace identity} capability provided by
	 * attached fragment revisions must not be included in the capabilities of
	 * the host bundle wiring.
	 * 
	 * @param namespace The namespace of the capabilities to return or
	 *        {@code null} to return the capabilities from all namespaces.
	 * @return A list containing a snapshot of the {@link BundleCapability}s, or
	 *         an empty list if this bundle wiring provides no capabilities in
	 *         the specified namespace. If this bundle wiring is not
	 *         {@link #isInUse() in use}, {@code null} will be returned. For a
	 *         given namespace, the list contains the capabilities in the order
	 *         the capabilities were specified in the manifests of the
	 *         {@link #getRevision() bundle revision} and the attached
	 *         fragments<sup>&#8224;</sup> of this bundle wiring. There is no
	 *         ordering defined between capabilities in different namespaces.
	 */
	List<BundleCapability> getCapabilities(String namespace);

	/**
	 * Returns the requirements of this bundle wiring.
	 * <p>
	 * Only requirements considered by the resolver are returned. For example,
	 * requirements with {@link Namespace#REQUIREMENT_EFFECTIVE_DIRECTIVE
	 * effective} directive not equal to {@link Namespace#EFFECTIVE_RESOLVE
	 * resolve} are not returned.
	 * <p>
	 * A bundle wiring for a non-fragment revision has a subset of the declared
	 * requirements from the bundle revision and all attached fragment
	 * revisions. Not all declared requirements may be present since some may be
	 * discarded. For example, if a package is declared to be both exported and
	 * imported, only one is selected and the other is discarded.
	 * 
	 * @param namespace The namespace of the requirements to return or
	 *            {@code null} to return the requirements from all namespaces.
	 * @return A list containing a snapshot of the {@link BundleRequirement}s,
	 *         or an empty list if this bundle wiring uses no requirements in
	 *         the specified namespace. If this bundle wiring is not
	 *         {@link #isInUse() in use}, {@code null} will be returned. For a
	 *         given namespace, the list contains the requirements in the order
	 *         the requirements were specified in the manifests of the
	 *         {@link #getRevision() bundle revision} and the attached fragments
	 *         of this bundle wiring. There is no ordering defined between
	 *         requirements in different namespaces.
	 */
	List<BundleRequirement> getRequirements(String namespace);

	/**
	 * Returns the {@link BundleWire}s to the provided {@link BundleCapability
	 * capabilities} of this bundle wiring.
	 * 
	 * @param namespace The namespace of the capabilities for which to return
	 *        wires or {@code null} to return the wires for the capabilities in
	 *        all namespaces.
	 * @return A list containing a snapshot of the {@link BundleWire}s for the
	 *         {@link BundleCapability capabilities} of this bundle wiring, or
	 *         an empty list if this bundle wiring has no capabilities in the
	 *         specified namespace. If this bundle wiring is not
	 *         {@link #isInUse() in use}, {@code null} will be returned. For a
	 *         given namespace, the list contains the wires in the order the
	 *         capabilities were specified in the manifests of the
	 *         {@link #getRevision() bundle revision} and the attached fragments
	 *         of this bundle wiring. There is no ordering defined between
	 *         capabilities in different namespaces.
	 */
	List<BundleWire> getProvidedWires(String namespace);

	/**
	 * Returns the {@link BundleWire}s to the {@link BundleRequirement
	 * requirements} in use by this bundle wiring.
	 * 
	 * <p>
	 * This method may return different results if this bundle wiring
	 * establishes additional wires to more requirements. For example,
	 * dynamically importing a package will establish a new wire to the
	 * dynamically imported package.
	 * 
	 * @param namespace The namespace of the requirements for which to return
	 *        wires or {@code null} to return the wires for the requirements in
	 *        all namespaces.
	 * @return A list containing a snapshot of the {@link BundleWire}s for the
	 *         {@link BundleRequirement requirements} of this bundle wiring, or
	 *         an empty list if this bundle wiring has no requirements in the
	 *         specified namespace. If this bundle wiring is not
	 *         {@link #isInUse() in use}, {@code null} will be returned. For a
	 *         given namespace, the list contains the wires in the order the
	 *         requirements were specified in the manifests of the
	 *         {@link #getRevision() bundle revision} and the attached fragments
	 *         of this bundle wiring followed by dynamically established wires,
	 *         if any, in the order they were established. There is no ordering
	 *         defined between requirements in different namespaces.
	 */
	List<BundleWire> getRequiredWires(String namespace);

	/**
	 * Returns the bundle revision for the bundle in this bundle wiring. Since a
	 * bundle update can change the entries in a bundle, different bundle
	 * wirings for the same bundle can have different bundle revisions.
	 * 
	 * <p>
	 * The bundle object {@link BundleReference#getBundle() referenced} by the
	 * returned {@code BundleRevision} may return different information than the
	 * returned {@code BundleRevision} since the returned {@code BundleRevision}
	 * may refer to an older revision of the bundle.
	 * 
	 * @return The bundle revision for this bundle wiring.
	 * @see BundleRevision#getWiring()
	 */
	BundleRevision getRevision();

	/**
	 * Returns the class loader for this bundle wiring. Since a bundle refresh
	 * creates a new bundle wiring for a bundle, different bundle wirings for
	 * the same bundle will have different class loaders.
	 * 
	 * @return The class loader for this bundle wiring. If this bundle wiring is
	 *         not {@link #isInUse() in use} or this bundle wiring is for a
	 *         fragment revision, {@code null} will be returned.
	 * @throws SecurityException If the caller does not have the appropriate
	 *         {@code RuntimePermission("getClassLoader")}, and the Java Runtime
	 *         Environment supports permissions.
	 */
	ClassLoader getClassLoader();

	/**
	 * Returns entries in this bundle wiring's {@link #getRevision() bundle
	 * revision} and its attached fragment revisions. This bundle wiring's class
	 * loader is not used to search for entries. Only the contents of this
	 * bundle wiring's bundle revision and its attached fragment revisions are
	 * searched for the specified entries.
	 * 
	 * <p>
	 * This method takes into account that the &quot;contents&quot; of this
	 * bundle wiring can have attached fragments. This &quot;bundle space&quot;
	 * is not a namespace with unique members; the same entry name can be
	 * present multiple times. This method therefore returns a list of URL
	 * objects. These URLs can come from different JARs but have the same path
	 * name. This method can either return only entries in the specified path or
	 * recurse into subdirectories returning entries in the directory tree
	 * beginning at the specified path.
	 * 
	 * <p>
	 * URLs for directory entries must have their path end with &quot;/&quot;.
	 * <p>
	 * Note: Jar and zip files are not required to include directory entries.
	 * URLs to directory entries will not be returned if the bundle contents do
	 * not contain directory entries.
	 * 
	 * @param path The path name in which to look. The path is always relative
	 *        to the root of this bundle wiring and may begin with
	 *        &quot;/&quot;. A path value of &quot;/&quot; indicates the root of
	 *        this bundle wiring.
	 * @param filePattern The file name pattern for selecting entries in the
	 *        specified path. The pattern is only matched against the last
	 *        element of the entry path. If the entry is a directory then the
	 *        trailing &quot;/&quot; is not used for pattern matching. Substring
	 *        matching is supported, as specified in the Filter specification,
	 *        using the wildcard character (&quot;*&quot;). If {@code null} is
	 *        specified, this is equivalent to &quot;*&quot; and matches all
	 *        files.
	 * @param options The options for listing resource names. See
	 *        {@link #FINDENTRIES_RECURSE}. The method must ignore unrecognized
	 *        options.
	 * @return An unmodifiable list of URL objects for each matching entry, or
	 *         an empty list if no matching entry could be found, if this bundle
	 *         wiring is for a fragment revision or if the caller does not have
	 *         the appropriate {@code AdminPermission[bundle,RESOURCE]} and the
	 *         Java Runtime Environment supports permissions. The list is
	 *         ordered such that entries from the {@link #getRevision() bundle
	 *         revision} are returned first followed by the entries from
	 *         attached fragment revisions in attachment order. If this bundle
	 *         wiring is not {@link #isInUse() in use}, {@code null} must be
	 *         returned.
	 * @see Bundle#findEntries(String, String, boolean)
	 */
	List<URL> findEntries(String path, String filePattern, int options);

	/**
	 * The find entries operation must recurse into subdirectories.
	 * 
	 * <p>
	 * This bit may be set when calling
	 * {@link #findEntries(String, String, int)} to specify the result must
	 * include the matching entries from the specified path and its
	 * subdirectories. If this bit is not set, then the result must only include
	 * matching entries from the specified path.
	 * 
	 * @see #findEntries(String, String, int)
	 */
	int	FINDENTRIES_RECURSE	= 0x00000001;

	/**
	 * Returns the names of resources visible to this bundle wiring's
	 * {@link #getClassLoader() class loader}. The returned names can be used to
	 * access the resources via this bundle wiring's class loader.
	 * 
	 * <ul>
	 * <li>Only the resource names for resources in bundle wirings will be
	 * returned. The names of resources visible to a bundle wiring's parent
	 * class loader, such as the bootstrap class loader, must not be included in
	 * the result.</li>
	 * <li>Only established wires will be examined for resources. This method
	 * must not cause new wires for dynamic imports to be established.</li>
	 * </ul>
	 * 
	 * @param path The path name in which to look. The path is always relative
	 *        to the root of this bundle wiring's class loader and may begin
	 *        with &quot;/&quot;. A path value of &quot;/&quot; indicates the
	 *        root of this bundle wiring's class loader.
	 * @param filePattern The file name pattern for selecting resource names in
	 *        the specified path. The pattern is only matched against the last
	 *        element of the resource path. If the resource is a directory then
	 *        the trailing &quot;/&quot; is not used for pattern matching.
	 *        Substring matching is supported, as specified in the Filter
	 *        specification, using the wildcard character (&quot;*&quot;). If
	 *        {@code null} is specified, this is equivalent to &quot;*&quot; and
	 *        matches all files.
	 * @param options The options for listing resource names. See
	 *        {@link #LISTRESOURCES_LOCAL} and {@link #LISTRESOURCES_RECURSE}.
	 *        This method must ignore unrecognized options.
	 * @return An unmodifiable collection of resource names for each matching
	 *         resource, or an empty collection if no matching resource could be
	 *         found, if this bundle wiring is for a fragment revision or if the
	 *         caller does not have the appropriate
	 *         {@code AdminPermission[bundle,RESOURCE]} and the Java Runtime
	 *         Environment supports permissions. The collection is unordered and
	 *         must contain no duplicate resource names. If this bundle wiring
	 *         is not {@link #isInUse() in use}, {@code null} must be returned.
	 */
	Collection<String> listResources(String path, String filePattern, int options);

	/**
	 * The list resource names operation must recurse into subdirectories.
	 * 
	 * <p>
	 * This bit may be set when calling
	 * {@link #listResources(String, String, int)} to specify the result must
	 * include the names of matching resources from the specified path and its
	 * subdirectories. If this bit is not set, then the result must only include
	 * names of matching resources from the specified path.
	 * 
	 * @see #listResources(String, String, int)
	 */
	int	LISTRESOURCES_RECURSE	= 0x00000001;

	/**
	 * The list resource names operation must limit the result to the names of
	 * matching resources contained in this bundle wiring's
	 * {@link #getRevision() bundle revision} and its attached fragment
	 * revisions. The result must not include resource names for resources in
	 * {@link PackageNamespace package} names which are
	 * {@link #getRequiredWires(String) imported} by this wiring.
	 * 
	 * <p>
	 * This bit may be set when calling
	 * {@link #listResources(String, String, int)} to specify the result must
	 * only include the names of matching resources contained in this bundle
	 * wiring's bundle revision and its attached fragment revisions. If this bit
	 * is not set, then the result must include the names of matching resources
	 * reachable from this bundle wiring's class loader which may include the
	 * names of matching resources contained in imported packages and required
	 * bundles.
	 * 
	 * @see #listResources(String, String, int)
	 */
	int	LISTRESOURCES_LOCAL		= 0x00000002;

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
	 * <p>
	 * This method returns the same value as {@link #getCapabilities(String)}.
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
	 * @since 1.1
	 */
	@Override
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
	 * <p>
	 * This method returns the same value as {@link #getRequirements(String)}.
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
	 * @since 1.1
	 */
	@Override
	List<Requirement> getResourceRequirements(String namespace);

	/**
	 * Returns the {@link Wire}s to the provided {@link Capability capabilities}
	 * of this wiring.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getProvidedWires(String)}.
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
	 * @since 1.1
	 */
	@Override
	List<Wire> getProvidedResourceWires(String namespace);

	/**
	 * Returns the {@link Wire}s to the {@link Requirement requirements} in use
	 * by this wiring.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getRequiredWires(String)}.
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
	 * @since 1.1
	 */
	@Override
	List<Wire> getRequiredResourceWires(String namespace);

	/**
	 * Returns the resource associated with this wiring.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getRevision()}.
	 * 
	 * @return The resource associated with this wiring.
	 * @since 1.1
	 */
	@Override
	BundleRevision getResource();
}
