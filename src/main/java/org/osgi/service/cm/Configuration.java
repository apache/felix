/*
 * Copyright (c) OSGi Alliance (2001, 2012). All Rights Reserved.
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
import org.osgi.framework.Filter;

/**
 * The configuration information for a {@code ManagedService} or
 * {@code ManagedServiceFactory} object.
 * 
 * The Configuration Admin service uses this interface to represent the
 * configuration information for a {@code ManagedService} or for a service
 * instance of a {@code ManagedServiceFactory}.
 * 
 * <p>
 * A {@code Configuration} object contains a configuration dictionary and allows
 * the properties to be updated via this object. Bundles wishing to receive
 * configuration dictionaries do not need to use this class - they register a
 * {@code ManagedService} or {@code ManagedServiceFactory}. Only administrative
 * bundles, and bundles wishing to update their own configurations need to use
 * this class.
 * 
 * <p>
 * The properties handled in this configuration have case insensitive
 * {@code String} objects as keys. However, case must be preserved from the last
 * set key/value.
 * <p>
 * A configuration can be <i>bound</i> to a specific bundle or to a region of
 * bundles using the <em>location</em>. In its simplest form the location is the
 * location of the target bundle that registered a Managed Service or a Managed
 * Service Factory. However, if the location starts with {@code ?} then the
 * location indicates multiple delivery. In such a case the configuration must
 * be delivered to all targets.
 * 
 * If security is on, the Configuration Permission can be used to restrict the
 * targets that receive updates. The Configuration Admin must only update a
 * target when the configuration location matches the location of the target's
 * bundle or the target bundle has a Configuration Permission with the action
 * {@link ConfigurationPermission#TARGET} and a name that matches the
 * configuration location. The name in the permission may contain wildcards (
 * {@code '*'}) to match the location using the same substring matching rules as
 * {@link Filter}.
 * 
 * Bundles can always create, manipulate, and be updated from configurations
 * that have a location that matches their bundle location.
 * 
 * <p>
 * If a configuration's location is {@code null}, it is not yet bound to a
 * location. It will become bound to the location of the first bundle that
 * registers a {@code ManagedService} or {@code ManagedServiceFactory} object
 * with the corresponding PID.
 * <p>
 * The same {@code Configuration} object is used for configuring both a Managed
 * Service Factory and a Managed Service. When it is important to differentiate
 * between these two the term "factory configuration" is used.
 * 
 * @noimplement
 * @version $Id: 4e016c45b463ae5c7b665ca53931441141088860 $
 */
public interface Configuration {
	/**
	 * Get the PID for this {@code Configuration} object.
	 * 
	 * @return the PID for this {@code Configuration} object.
	 * @throws IllegalStateException if this configuration has been deleted
	 */
	public String getPid();

	/**
	 * Return the properties of this {@code Configuration} object.
	 * 
	 * The {@code Dictionary} object returned is a private copy for the caller
	 * and may be changed without influencing the stored configuration. The keys
	 * in the returned dictionary are case insensitive and are always of type
	 * {@code String}.
	 * 
	 * <p>
	 * If called just after the configuration is created and before update has
	 * been called, this method returns {@code null}.
	 * 
	 * @return A private copy of the properties for the caller or {@code null}.
	 *         These properties must not contain the "service.bundleLocation"
	 *         property. The value of this property may be obtained from the
	 *         {@link #getBundleLocation()} method.
	 * @throws IllegalStateException If this configuration has been deleted.
	 */
	public Dictionary<String, Object> getProperties();

	/**
	 * Update the properties of this {@code Configuration} object.
	 * 
	 * Stores the properties in persistent storage after adding or overwriting
	 * the following properties:
	 * <ul>
	 * <li>"service.pid" : is set to be the PID of this configuration.</li>
	 * <li>"service.factoryPid" : if this is a factory configuration it is set
	 * to the factory PID else it is not set.</li>
	 * </ul>
	 * These system properties are all of type {@code String}.
	 * 
	 * <p>
	 * If the corresponding Managed Service/Managed Service Factory is
	 * registered, its updated method must be called asynchronously. Else, this
	 * callback is delayed until aforementioned registration occurs.
	 * 
	 * <p>
	 * Also initiates an asynchronous call to all {@link ConfigurationListener}s
	 * with a {@link ConfigurationEvent#CM_UPDATED} event.
	 * 
	 * @param properties the new set of properties for this configuration
	 * @throws IOException if update cannot be made persistent
	 * @throws IllegalArgumentException if the {@code Dictionary} object
	 *         contains invalid configuration types or contains case variants of
	 *         the same key name.
	 * @throws IllegalStateException If this configuration has been deleted.
	 */
	public void update(Dictionary<String, ?> properties) throws IOException;

	/**
	 * Delete this {@code Configuration} object.
	 * 
	 * Removes this configuration object from the persistent store. Notify
	 * asynchronously the corresponding Managed Service or Managed Service
	 * Factory. A {@link ManagedService} object is notified by a call to its
	 * {@code updated} method with a {@code null} properties argument. A
	 * {@link ManagedServiceFactory} object is notified by a call to its
	 * {@code deleted} method.
	 * 
	 * <p>
	 * Also initiates an asynchronous call to all {@link ConfigurationListener}s
	 * with a {@link ConfigurationEvent#CM_DELETED} event.
	 * 
	 * @throws IOException If delete fails.
	 * @throws IllegalStateException If this configuration has been deleted.
	 */
	public void delete() throws IOException;

	/**
	 * For a factory configuration return the PID of the corresponding Managed
	 * Service Factory, else return {@code null}.
	 * 
	 * @return factory PID or {@code null}
	 * @throws IllegalStateException If this configuration has been deleted.
	 */
	public String getFactoryPid();

	/**
	 * Update the {@code Configuration} object with the current properties.
	 * 
	 * Initiate the {@code updated} callback to the Managed Service or Managed
	 * Service Factory with the current properties asynchronously.
	 * 
	 * <p>
	 * This is the only way for a bundle that uses a Configuration Plugin
	 * service to initiate a callback. For example, when that bundle detects a
	 * change that requires an update of the Managed Service or Managed Service
	 * Factory via its {@code ConfigurationPlugin} object.
	 * 
	 * @see ConfigurationPlugin
	 * @throws IOException if update cannot access the properties in persistent
	 *         storage
	 * @throws IllegalStateException If this configuration has been deleted.
	 */
	public void update() throws IOException;

	/**
	 * Bind this {@code Configuration} object to the specified location.
	 * 
	 * If the location parameter is {@code null} then the {@code Configuration}
	 * object will not be bound to a location/region. It will be set to the
	 * bundle's location before the first time a Managed Service/Managed Service
	 * Factory receives this {@code Configuration} object via the updated method
	 * and before any plugins are called. The bundle location or region will be
	 * set persistently.
	 * 
	 * <p>
	 * If the location starts with {@code ?} then all targets registered with
	 * the given PID must be updated.
	 * 
	 * <p>
	 * If the location is changed then existing targets must be informed. If
	 * they can no longer see this configuration, the configuration must be
	 * deleted or updated with {@code null}. If this configuration becomes
	 * visible then they must be updated with this configuration.
	 * 
	 * <p>
	 * Also initiates an asynchronous call to all {@link ConfigurationListener}s
	 * with a {@link ConfigurationEvent#CM_LOCATION_CHANGED} event.
	 * 
	 * @param location a location, region, or {@code null}
	 * @throws IllegalStateException If this configuration has been deleted.
	 * @throws SecurityException when the required permissions are not available
	 * @throws SecurityException when the required permissions are not available
	 * @security ConfigurationPermission[this.location,CONFIGURE] if
	 *           this.location is not {@code null}
	 * @security ConfigurationPermission[location,CONFIGURE] if location is not
	 *           {@code null}
	 * @security ConfigurationPermission["*",CONFIGURE] if this.location is
	 *           {@code null} or if location is {@code null}
	 */
	public void setBundleLocation(String location);

	/**
	 * Get the bundle location.
	 * 
	 * Returns the bundle location or region to which this configuration is
	 * bound, or {@code null} if it is not yet bound to a bundle location or
	 * region. If the location starts with {@code ?} then the configuration is
	 * delivered to all targets and not restricted to a single bundle.
	 * 
	 * @return location to which this configuration is bound, or {@code null}.
	 * @throws IllegalStateException If this configuration has been deleted.
	 * @throws SecurityException when the required permissions are not available
	 * @security ConfigurationPermission[this.location,CONFIGURE] if
	 *           this.location is not {@code null}
	 * @security ConfigurationPermission["*",CONFIGURE] if this.location is
	 *           {@code null}
	 * 
	 */
	public String getBundleLocation();

	/**
	 * Get the change count.
	 * 
	 * The Configuration must maintain a change counter that every time when
	 * this configuration is updated and its properties are stored is
	 * incremented with a positive value. The counter must be changed after the
	 * properties are persisted but before the targets are updated and events
	 * are sent out.
	 * 
	 * @return A monotonously increasing value reflecting changes in this
	 *         Configuration
	 * 
	 * @since 1.5
	 */
	public long getChangeCount();

	/**
	 * Equality is defined to have equal PIDs
	 * 
	 * Two Configuration objects are equal when their PIDs are equal.
	 * 
	 * @param other {@code Configuration} object to compare against
	 * @return {@code true} if equal, {@code false} if not a
	 *         {@code Configuration} object or one with a different PID.
	 */
	public boolean equals(Object other);

	/**
	 * Hash code is based on PID.
	 * 
	 * The hash code for two Configuration objects must be the same when the
	 * Configuration PID's are the same.
	 * 
	 * @return hash code for this Configuration object
	 */
	public int hashCode();
}
