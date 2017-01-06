/*
 * Copyright (c) OSGi Alliance (2001, 2016). All Rights Reserved.
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

package org.osgi.service.cm;

import java.io.IOException;
import java.util.Dictionary;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Service for administering configuration data.
 * <p>
 * The main purpose of this interface is to store bundle configuration data
 * persistently. This information is represented in {@code Configuration}
 * objects. The actual configuration data is a {@code Dictionary} of properties
 * inside a {@code Configuration} object.
 * <p>
 * There are two principally different ways to manage configurations. First
 * there is the concept of a Managed Service, where configuration data is
 * uniquely associated with an object registered with the service registry.
 * <p>
 * Next, there is the concept of a factory where the Configuration Admin service
 * will maintain 0 or more {@code Configuration} objects for a Managed Service
 * Factory that is registered with the Framework.
 * <p>
 * The first concept is intended for configuration data about "things/services"
 * whose existence is defined externally, e.g. a specific printer. Factories are
 * intended for "things/services" that can be created any number of times, e.g.
 * a configuration for a DHCP server for different networks.
 * <p>
 * Bundles that require configuration should register a Managed Service or a
 * Managed Service Factory in the service registry. A registration property
 * named {@code service.pid} (persistent identifier or PID) must be used to
 * identify this Managed Service or Managed Service Factory to the Configuration
 * Admin service.
 * <p>
 * When the ConfigurationAdmin detects the registration of a Managed Service, it
 * checks its persistent storage for a configuration object whose
 * {@code service.pid} property matches the PID service property (
 * {@code service.pid}) of the Managed Service. If found, it calls
 * {@link ManagedService#updated(Dictionary)} method with the new properties.
 * The implementation of a Configuration Admin service must run these call-backs
 * asynchronously to allow proper synchronization.
 * <p>
 * When the Configuration Admin service detects a Managed Service Factory
 * registration, it checks its storage for configuration objects whose
 * {@code service.factoryPid} property matches the PID service property of the
 * Managed Service Factory. For each such {@code Configuration} objects, it
 * calls the {@code ManagedServiceFactory.updated} method asynchronously with
 * the new properties. The calls to the {@code updated} method of a
 * {@code ManagedServiceFactory} must be executed sequentially and not overlap
 * in time.
 * <p>
 * In general, bundles having permission to use the Configuration Admin service
 * can only access and modify their own configuration information. Accessing or
 * modifying the configuration of other bundles requires
 * {@code ConfigurationPermission[location,CONFIGURE]}, where location is the
 * configuration location.
 * <p>
 * {@code Configuration} objects can be <i>bound</i> to a specified bundle
 * location or to a region (configuration location starts with {@code ?}). If a
 * location is not set, it will be learned the first time a target is
 * registered. If the location is learned this way, the Configuration Admin
 * service must detect if the bundle corresponding to the location is
 * uninstalled. If this occurs, the {@code Configuration} object must be
 * unbound, that is its location field is set back to {@code null}.
 * <p>
 * If target's bundle location matches the configuration location it is always
 * updated.
 * <p>
 * If the configuration location starts with {@code ?}, that is, the location is
 * a region, then the configuration must be delivered to all targets registered
 * with the given PID. If security is on, the target bundle must have
 * Configuration Permission[location,TARGET], where location matches given the
 * configuration location with wildcards as in the Filter substring match. The
 * security must be verified using the
 * {@link org.osgi.framework.Bundle#hasPermission(Object)} method on the target
 * bundle.
 * <p>
 * If a target cannot be updated because the location does not match or it has
 * no permission and security is active then the Configuration Admin service
 * must not do the normal callback.
 * <p>
 * The method descriptions of this class refer to a concept of "the calling
 * bundle". This is a loose way of referring to the bundle which obtained the
 * Configuration Admin service from the service registry. Implementations of
 * {@code ConfigurationAdmin} must use a
 * {@link org.osgi.framework.ServiceFactory} to support this concept.
 *
 * @author $Id$
 * @ThreadSafe
 */
@ProviderType
public interface ConfigurationAdmin {
	/**
	 * Configuration property naming the Factory PID in the configuration
	 * dictionary. The property's value is of type {@code String}.
	 *
	 * @since 1.1
	 */
	public final static String	SERVICE_FACTORYPID		= "service.factoryPid";
	/**
	 * Configuration property naming the location of the bundle that is
	 * associated with a {@code Configuration} object. This property can be
	 * searched for but must not appear in the configuration dictionary for
	 * security reason. The property's value is of type {@code String}.
	 *
	 * @since 1.1
	 */
	public final static String	SERVICE_BUNDLELOCATION	= "service.bundleLocation";

	/**
	 * Create a new factory {@code Configuration} object with a new PID.
	 *
	 * The properties of the new {@code Configuration} object are {@code null}
	 * until the first time that its {@link Configuration#update(Dictionary)}
	 * method is called.
	 *
	 * <p>
	 * It is not required that the {@code factoryPid} maps to a registered
	 * Managed Service Factory.
	 *
	 * <p>
	 * The {@code Configuration} object is bound to the location of the calling
	 * bundle. It is possible that the same factoryPid has associated
	 * configurations that are bound to different bundles. Bundles should only
	 * see the factory configurations that they are bound to or have the proper
	 * permission.
	 *
	 * @param factoryPid PID of factory (not {@code null}).
	 * @return A new {@code Configuration} object.
	 * @throws IOException if access to persistent storage fails.
	 */
	public Configuration createFactoryConfiguration(String factoryPid) throws IOException;

	/**
	 * Create a new factory {@code Configuration} object with a new PID.
	 *
	 * The properties of the new {@code Configuration} object are {@code null}
	 * until the first time that its {@link Configuration#update(Dictionary)}
	 * method is called.
	 *
	 * <p>
	 * It is not required that the {@code factoryPid} maps to a registered
	 * Managed Service Factory.
	 *
	 * <p>
	 * The {@code Configuration} is bound to the location specified. If this
	 * location is {@code null} it will be bound to the location of the first
	 * bundle that registers a Managed Service Factory with a corresponding PID.
	 * It is possible that the same factoryPid has associated configurations
	 * that are bound to different bundles. Bundles should only see the factory
	 * configurations that they are bound to or have the proper permission.
	 *
	 * <p>
	 * If the location starts with {@code ?} then the configuration must be
	 * delivered to all targets with the corresponding PID.
	 *
	 * @param factoryPid PID of factory (not {@code null}).
	 * @param location A bundle location string, or {@code null}.
	 * @return a new {@code Configuration} object.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException when the require permissions are not available
	 * @security ConfigurationPermission[location,CONFIGURE] if location is not
	 *           {@code null}
	 * @security ConfigurationPermission["*",CONFIGURE] if location is
	 *           {@code null}
	 */
	public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException;

	/**
	 * Get an existing {@code Configuration} object from the persistent store,
	 * or create a new {@code Configuration} object.
	 *
	 * <p>
	 * If a {@code Configuration} with this PID already exists in Configuration
	 * Admin service return it. The location parameter is ignored in this case
	 * though it is still used for a security check.
	 *
	 * <p>
	 * Else, return a new {@code Configuration} object. This new object is bound
	 * to the location and the properties are set to {@code null}. If the
	 * location parameter is {@code null}, it will be set when a Managed Service
	 * with the corresponding PID is registered for the first time. If the
	 * location starts with {@code ?} then the configuration is bound to all
	 * targets that are registered with the corresponding PID.
	 *
	 * @param pid Persistent identifier.
	 * @param location The bundle location string, or {@code null}.
	 * @return An existing or new {@code Configuration} object.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException when the require permissions are not available
	 * @security ConfigurationPermission[*,CONFIGURE] if location is
	 *           {@code null} or if the returned configuration {@code c} already
	 *           exists and c.location is {@code null}
	 * @security ConfigurationPermission[location,CONFIGURE] if location is not
	 *           {@code null}
	 * @security ConfigurationPermission[c.location,CONFIGURE] if the returned
	 *           configuration {@code c} already exists and c.location is not
	 *           {@code null}
	 */
	public Configuration getConfiguration(String pid, String location) throws IOException;

	/**
	 * Get an existing or new {@code Configuration} object from the persistent
	 * store.
	 *
	 * If the {@code Configuration} object for this PID does not exist, create a
	 * new {@code Configuration} object for that PID, where properties are
	 * {@code null}. Bind its location to the calling bundle's location.
	 *
	 * <p>
	 * Otherwise, if the location of the existing {@code Configuration} object
	 * is {@code null}, set it to the calling bundle's location.
	 *
	 * @param pid persistent identifier.
	 * @return an existing or new {@code Configuration} matching the PID.
	 * @throws IOException if access to persistent storage fails.
	 * @throws SecurityException when the required permission is not available
	 * @security ConfigurationPermission[c.location,CONFIGURE] If the
	 *           configuration {@code c} already exists and c.location is not
	 *           {@code null}
	 */
	public Configuration getConfiguration(String pid) throws IOException;

    /**
     * Get an existing or new {@code Configuration} object from the persistent
     * store.
     *
     * The PID for this {@code Configuration} object is generated from the
     * provided factory PID and the alias by starting with the factory PID
     * appending the character # and then appending the alias.
     *
     * <p>
     * If a {@code Configuration} with this PID already exists in Configuration
     * Admin service return it. The location parameter is ignored in this case
     * though it is still used for a security check.
     *
     * <p>
     * Else, return a new {@code Configuration} object. This new object is bound
     * to the location and the properties are set to {@code null}. If the
     * location parameter is {@code null}, it will be set when a Managed Service
     * with the corresponding PID is registered for the first time. If the
     * location starts with {@code ?} then the configuration is bound to all
     * targets that are registered with the corresponding PID.
     *
     * @param factoryPid PID of factory (not {@code null}).
     * @param alias An alias for {@code Configuration} (not {@code null}).
     * @param location The bundle location string, or {@code null}.
     * @return An existing or new {@code Configuration} object.
     * @throws IOException if access to persistent storage fails.
     * @throws SecurityException when the require permissions are not available
     * @security ConfigurationPermission[*,CONFIGURE] if location is
     *           {@code null} or if the returned configuration {@code c} already
     *           exists and c.location is {@code null}
     * @security ConfigurationPermission[location,CONFIGURE] if location is not
     *           {@code null}
     * @security ConfigurationPermission[c.location,CONFIGURE] if the returned
     *           configuration {@code c} already exists and c.location is not
     *           {@code null}
     * @since 1.6
     */
	public Configuration getFactoryConfiguration(String factoryPid, String alias, String location) throws IOException;

    /**
     * Get an existing or new {@code Configuration} object from the persistent
     * store.
     *
     * The PID for this {@code Configuration} object is generated from the
     * provided factory PID and the alias by starting with the factory PID
     * appending the character # and then appending the alias.
     *
     * If the {@code Configuration} object for this PID does not exist, create a
     * new {@code Configuration} object for that PID, where properties are
     * {@code null}. Bind its location to the calling bundle's location.
     *
     * <p>
     * Otherwise, if the location of the existing {@code Configuration} object
     * is {@code null}, set it to the calling bundle's location.
     *
     * @param factoryPid PID of factory (not {@code null}).
     * @param alias An alias for {@code Configuration} (not {@code null}).
     * @return an existing or new {@code Configuration} matching the PID.
     * @throws IOException if access to persistent storage fails.
     * @throws SecurityException when the required permission is not available
     * @security ConfigurationPermission[c.location,CONFIGURE] If the
     *           configuration {@code c} already exists and c.location is not
     *           {@code null}
     * @since 1.6
     */
	public Configuration getFactoryConfiguration(String factoryPid, String alias) throws IOException;

	/**
	 * List the current {@code Configuration} objects which match the filter.
	 *
	 * <p>
	 * Only {@code Configuration} objects with non- {@code null} properties are
	 * considered current. That is, {@code Configuration.getProperties()} is
	 * guaranteed not to return {@code null} for each of the returned
	 * {@code Configuration} objects.
	 *
	 * <p>
	 * When there is no security on then all configurations can be returned. If
	 * security is on, the caller must have
	 * ConfigurationPermission[location,CONFIGURE].
	 *
	 * <p>
	 * The syntax of the filter string is as defined in the {@link Filter}
	 * class. The filter can test any configuration properties including the
	 * following:
	 * <ul>
	 * <li>{@code service.pid} - the persistent identity</li>
	 * <li>{@code service.factoryPid} - the factory PID, if applicable</li>
	 * <li>{@code service.bundleLocation} - the bundle location</li>
	 * </ul>
	 * The filter can also be {@code null}, meaning that all
	 * {@code Configuration} objects should be returned.
	 *
	 * @param filter A filter string, or {@code null} to retrieve all
	 *        {@code Configuration} objects.
	 * @return All matching {@code Configuration} objects, or {@code null} if
	 *         there aren't any.
	 * @throws IOException if access to persistent storage fails
	 * @throws InvalidSyntaxException if the filter string is invalid
	 * @security ConfigurationPermission[c.location,CONFIGURE] Only
	 *           configurations {@code c} are returned for which the caller has
	 *           this permission
	 */
	public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException;
}
