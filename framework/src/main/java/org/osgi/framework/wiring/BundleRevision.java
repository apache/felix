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

package org.osgi.framework.wiring;

import java.util.List;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Bundle Revision. When a bundle is installed and each time a bundle is
 * updated, a new bundle revision of the bundle is created. Since a bundle
 * update can change the entries in a bundle, different bundle wirings for the
 * same bundle can be associated with different bundle revisions.
 * 
 * <p>
 * For a bundle that has not been uninstalled, the most recent bundle revision
 * is defined to be the current bundle revision. A bundle in the UNINSTALLED
 * state does not have a current revision. The current bundle revision for a
 * bundle can be obtained by calling {@link Bundle#adapt(Class) bundle.adapt}
 * (BundleRevision.class). Since a bundle in the UNINSTALLED state does not have
 * a current revision, adapting such a bundle returns {@code null}.
 * 
 * <p>
 * The framework defines namespaces for {@link PackageNamespace package},
 * {@link BundleNamespace bundle} and {@link HostNamespace host} capabilities
 * and requirements. These namespaces are defined only to express wiring
 * information by the framework. They must not be used in
 * {@link Constants#PROVIDE_CAPABILITY Provide-Capability} and
 * {@link Constants#REQUIRE_CAPABILITY Require-Capability} manifest headers.
 * 
 * @ThreadSafe
 * @author $Id: 1f318afdf8f5fe6097a841b49b67bf67d8c924f2 $
 */
@ProviderType
public interface BundleRevision extends BundleReference, Resource {
	/**
	 * Returns the symbolic name for this bundle revision.
	 * 
	 * @return The symbolic name for this bundle revision.
	 * @see Bundle#getSymbolicName()
	 */
	String getSymbolicName();

	/**
	 * Returns the version for this bundle revision.
	 * 
	 * @return The version for this bundle revision, or
	 *         {@link Version#emptyVersion} if this bundle revision has no
	 *         version information.
	 * @see Bundle#getVersion()
	 */
	Version getVersion();

	/**
	 * Returns the capabilities declared by this bundle revision.
	 * 
	 * @param namespace The namespace of the declared capabilities to return or
	 *        {@code null} to return the declared capabilities from all
	 *        namespaces.
	 * @return An unmodifiable list containing the declared
	 *         {@link BundleCapability}s from the specified namespace. The
	 *         returned list will be empty if this bundle revision declares no
	 *         capabilities in the specified namespace. The list contains the
	 *         declared capabilities in the order they are specified in the
	 *         manifest.
	 */
	List<BundleCapability> getDeclaredCapabilities(String namespace);

	/**
	 * Returns the requirements declared by this bundle revision.
	 * 
	 * @param namespace The namespace of the declared requirements to return or
	 *        {@code null} to return the declared requirements from all
	 *        namespaces.
	 * @return An unmodifiable list containing the declared
	 *         {@link BundleRequirement}s from the specified namespace. The
	 *         returned list will be empty if this bundle revision declares no
	 *         requirements in the specified namespace. The list contains the
	 *         declared requirements in the order they are specified in the
	 *         manifest.
	 */
	List<BundleRequirement> getDeclaredRequirements(String namespace);

	/**
	 * Namespace for package capabilities and requirements.
	 * 
	 * <p>
	 * The name of the package is stored in the capability attribute of the same
	 * name as this namespace (osgi.wiring.package). The other directives and
	 * attributes of the package, from the {@link Constants#EXPORT_PACKAGE
	 * Export-Package} manifest header, can be found in the cabability's
	 * {@link BundleCapability#getDirectives() directives} and
	 * {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#VERSION_ATTRIBUTE version} capability attribute must
	 * contain the {@link Version} of the package if one is specified or
	 * {@link Version#emptyVersion} if not specified. The
	 * {@link Constants#BUNDLE_SYMBOLICNAME_ATTRIBUTE bundle-symbolic-name}
	 * capability attribute must contain the
	 * {@link BundleRevision#getSymbolicName() symbolic name} of the provider if
	 * one is specified. The {@link Constants#BUNDLE_VERSION_ATTRIBUTE
	 * bundle-version} capability attribute must contain the
	 * {@link BundleRevision#getVersion() version} of the provider if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * The package capabilities provided by the system bundle, that is the
	 * bundle with id zero, must include the package specified by the
	 * {@link Constants#FRAMEWORK_SYSTEMPACKAGES} and
	 * {@link Constants#FRAMEWORK_SYSTEMPACKAGES_EXTRA} framework properties as
	 * well as any other package exported by the framework implementation.
	 * 
	 * <p>
	 * A bundle revision {@link BundleRevision#getDeclaredCapabilities(String)
	 * declares} zero or more package capabilities (this is, exported packages)
	 * and {@link BundleRevision#getDeclaredRequirements(String) declares} zero
	 * or more package requirements.
	 * <p>
	 * A bundle wiring {@link BundleWiring#getCapabilities(String) provides}
	 * zero or more resolved package capabilities (that is, exported packages)
	 * and {@link BundleWiring#getRequiredWires(String) requires} zero or more
	 * resolved package requirements (that is, imported packages). The number of
	 * package wires required by a bundle wiring may change as the bundle wiring
	 * may dynamically import additional packages.
	 * 
	 * @see PackageNamespace
	 */
	String	PACKAGE_NAMESPACE	= PackageNamespace.PACKAGE_NAMESPACE;

	/**
	 * Namespace for bundle capabilities and requirements.
	 * 
	 * <p>
	 * The bundle symbolic name of the bundle is stored in the capability
	 * attribute of the same name as this namespace (osgi.wiring.bundle). The
	 * other directives and attributes of the bundle, from the
	 * {@link Constants#BUNDLE_SYMBOLICNAME Bundle-SymbolicName} manifest
	 * header, can be found in the cabability's
	 * {@link BundleCapability#getDirectives() directives} and
	 * {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} capability
	 * attribute must contain the {@link Version} of the bundle from the
	 * {@link Constants#BUNDLE_VERSION Bundle-Version} manifest header if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * A non-fragment revision
	 * {@link BundleRevision#getDeclaredCapabilities(String) declares} exactly
	 * one<sup>&#8224;</sup> bundle capability (that is, the bundle can be
	 * required by another bundle). A fragment revision must not declare a
	 * bundle capability.
	 * 
	 * <p>
	 * A bundle wiring for a non-fragment revision
	 * {@link BundleWiring#getCapabilities(String) provides} exactly
	 * one<sup>&#8224;</sup> bundle capability (that is, the bundle can be
	 * required by another bundle) and
	 * {@link BundleWiring#getRequiredWires(String) requires} zero or more
	 * bundle capabilities (that is, requires other bundles).
	 * 
	 * <p>
	 * &#8224; A bundle with no bundle symbolic name (that is, a bundle with
	 * {@link Constants#BUNDLE_MANIFESTVERSION Bundle-ManifestVersion}
	 * {@literal <} 2) must not provide a bundle capability.
	 * 
	 * @see BundleNamespace
	 */
	String	BUNDLE_NAMESPACE	= BundleNamespace.BUNDLE_NAMESPACE;

	/**
	 * Namespace for host capabilities and requirements.
	 * 
	 * <p>
	 * The bundle symbolic name of the bundle is stored in the capability
	 * attribute of the same name as this namespace (osgi.wiring.host). The
	 * other directives and attributes of the bundle, from the
	 * {@link Constants#BUNDLE_SYMBOLICNAME Bundle-SymbolicName} manifest
	 * header, can be found in the cabability's
	 * {@link BundleCapability#getDirectives() directives} and
	 * {@link BundleCapability#getAttributes() attributes}. The
	 * {@link Constants#BUNDLE_VERSION_ATTRIBUTE bundle-version} capability
	 * attribute must contain the {@link Version} of the bundle from the
	 * {@link Constants#BUNDLE_VERSION Bundle-Version} manifest header if one is
	 * specified or {@link Version#emptyVersion} if not specified.
	 * 
	 * <p>
	 * A non-fragment revision
	 * {@link BundleRevision#getDeclaredCapabilities(String) declares} zero or
	 * one<sup>&#8224;</sup> host capability if the bundle
	 * {@link Constants#FRAGMENT_ATTACHMENT_DIRECTIVE allows fragments to be
	 * attached}. A fragment revision must
	 * {@link BundleRevision#getDeclaredRequirements(String) declare} exactly
	 * one host requirement.
	 * 
	 * <p>
	 * A bundle wiring for a non-fragment revision
	 * {@link BundleWiring#getCapabilities(String) provides} zero or
	 * one<sup>&#8224;</sup> host capability if the bundle
	 * {@link Constants#FRAGMENT_ATTACHMENT_DIRECTIVE allows fragments to be
	 * attached}. A bundle wiring for a fragment revision
	 * {@link BundleWiring#getRequiredWires(String) requires} a host capability
	 * for each host to which it is attached.
	 * 
	 * <p>
	 * &#8224; A bundle with no bundle symbolic name (that is, a bundle with
	 * {@link Constants#BUNDLE_MANIFESTVERSION Bundle-ManifestVersion}
	 * {@literal <} 2) must not provide a host capability.
	 * 
	 * @see HostNamespace
	 */
	String	HOST_NAMESPACE		= HostNamespace.HOST_NAMESPACE;

	/**
	 * Returns the special types of this bundle revision. The bundle revision
	 * type values are:
	 * <ul>
	 * <li>{@link #TYPE_FRAGMENT}</li>
	 * </ul>
	 * 
	 * A bundle revision may be more than one type at a time. A type code is
	 * used to identify the bundle revision type for future extendability.
	 * 
	 * <p>
	 * If this bundle revision is not one or more of the defined types then 0 is
	 * returned.
	 * 
	 * @return The special types of this bundle revision. The type values are
	 *         ORed together.
	 */
	int getTypes();

	/**
	 * Bundle revision type indicating the bundle revision is a fragment.
	 * 
	 * @see #getTypes()
	 */
	int	TYPE_FRAGMENT	= 0x00000001;

	/**
	 * Returns the bundle wiring which is using this bundle revision.
	 * 
	 * @return The bundle wiring which is using this bundle revision or
	 *         {@code null} if no bundle wiring is using this bundle revision.
	 * @see BundleWiring#getRevision()
	 */
	BundleWiring getWiring();

	/**
	 * Returns the capabilities declared by this resource.
	 * 
	 * <p>
	 * This method returns the same value as
	 * {@link #getDeclaredCapabilities(String)}.
	 * 
	 * @param namespace The namespace of the declared capabilities to return or
	 *        {@code null} to return the declared capabilities from all
	 *        namespaces.
	 * @return An unmodifiable list containing the declared {@link Capability}s
	 *         from the specified namespace. The returned list will be empty if
	 *         this resource declares no capabilities in the specified
	 *         namespace.
	 * @since 1.1
	 */
	List<Capability> getCapabilities(String namespace);

	/**
	 * Returns the requirements declared by this bundle resource.
	 * 
	 * <p>
	 * This method returns the same value as
	 * {@link #getDeclaredRequirements(String)}.
	 * 
	 * @param namespace The namespace of the declared requirements to return or
	 *        {@code null} to return the declared requirements from all
	 *        namespaces.
	 * @return An unmodifiable list containing the declared {@link Requirement}
	 *         s from the specified namespace. The returned list will be empty
	 *         if this resource declares no requirements in the specified
	 *         namespace.
	 * @since 1.1
	 */
	List<Requirement> getRequirements(String namespace);
}
