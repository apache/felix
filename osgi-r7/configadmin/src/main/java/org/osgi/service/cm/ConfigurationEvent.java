/*
 * Copyright (c) OSGi Alliance (2004, 2016). All Rights Reserved.
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

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;

/**
 * A Configuration Event.
 * <p>
 * {@code ConfigurationEvent} objects are delivered to all registered
 * {@code ConfigurationListener} service objects. ConfigurationEvents must be
 * delivered in chronological order with respect to each listener.
 * <p>
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #CM_UPDATED}</li>
 * <li>{@link #CM_DELETED}</li>
 * <li>{@link #CM_LOCATION_CHANGED}</li>
 * </ul>
 * Additional event types may be defined in the future.
 * <p>
 * Security Considerations. {@code ConfigurationEvent} objects do not provide
 * {@code Configuration} objects, so no sensitive configuration information is
 * available from the event. If the listener wants to locate the
 * {@code Configuration} object for the specified pid, it must use
 * {@code ConfigurationAdmin}.
 *
 * @see ConfigurationListener
 * @Immutable
 * @author $Id$
 * @since 1.2
 */
public class ConfigurationEvent {
	/**
	 * A {@code Configuration} has been updated.
	 *
	 * <p>
	 * This {@code ConfigurationEvent} type that indicates that a
	 * {@code Configuration} object has been updated with new properties.
	 *
	 * An event is fired when a call to {@link Configuration#update(Dictionary)}
	 * successfully changes a configuration.
	 */
	public static final int								CM_UPDATED			= 1;
	/**
	 * A {@code Configuration} has been deleted.
	 *
	 * <p>
	 * This {@code ConfigurationEvent} type that indicates that a
	 * {@code Configuration} object has been deleted.
	 *
	 * An event is fired when a call to {@link Configuration#delete()}
	 * successfully deletes a configuration.
	 */
	public static final int								CM_DELETED			= 2;

	/**
	 * The location of a {@code Configuration} has been changed.
	 *
	 * <p>
	 * This {@code ConfigurationEvent} type that indicates that the location of
	 * a {@code Configuration} object has been changed.
	 *
	 * An event is fired when a call to
	 * {@link Configuration#setBundleLocation(String)} successfully changes the
	 * location.
	 *
	 * @since 1.4
	 */
	public static final int								CM_LOCATION_CHANGED	= 3;
	/**
	 * Type of this event.
	 *
	 * @see #getType()
	 */
	private final int									type;
	/**
	 * The factory pid associated with this event.
	 */
	private final String								factoryPid;
	/**
	 * The pid associated with this event.
	 */
	private final String								pid;
	/**
	 * The ConfigurationAdmin service which created this event.
	 */
	private final ServiceReference<ConfigurationAdmin>	reference;

	/**
	 * Constructs a {@code ConfigurationEvent} object from the given
	 * {@code ServiceReference} object, event type, and pids.
	 *
	 * @param reference The {@code ServiceReference} object of the Configuration
	 *        Admin service that created this event.
	 * @param type The event type. See {@link #getType()}.
	 * @param factoryPid The factory pid of the associated configuration if the
	 *        target of the configuration is a ManagedServiceFactory. Otherwise
	 *        {@code null} if the target of the configuration is a
	 *        ManagedService.
	 * @param pid The pid of the associated configuration.
	 */
	public ConfigurationEvent(ServiceReference<ConfigurationAdmin> reference, int type, String factoryPid, String pid) {
		this.reference = reference;
		this.type = type;
		this.factoryPid = factoryPid;
		this.pid = pid;
		if ((reference == null) || (pid == null)) {
			throw new NullPointerException("reference and pid must not be null");
		}
	}

	/**
	 * Returns the factory pid of the associated configuration.
	 *
	 * @return Returns the factory pid of the associated configuration if the
	 *         target of the configuration is a ManagedServiceFactory. Otherwise
	 *         {@code null} if the target of the configuration is a
	 *         ManagedService.
	 */
	public String getFactoryPid() {
		return factoryPid;
	}

	/**
	 * Returns the pid of the associated configuration.
	 *
	 * @return Returns the pid of the associated configuration.
	 */
	public String getPid() {
		return pid;
	}

	/**
	 * Return the type of this event.
	 * <p>
	 * The type values are:
	 * <ul>
	 * <li>{@link #CM_UPDATED}</li>
	 * <li>{@link #CM_DELETED}</li>
	 * <li>{@link #CM_LOCATION_CHANGED}</li>
	 * </ul>
	 *
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the {@code ServiceReference} object of the Configuration Admin
	 * service that created this event.
	 *
	 * @return The {@code ServiceReference} object for the Configuration Admin
	 *         service that created this event.
	 */
	public ServiceReference<ConfigurationAdmin> getReference() {
		return reference;
	}
}
