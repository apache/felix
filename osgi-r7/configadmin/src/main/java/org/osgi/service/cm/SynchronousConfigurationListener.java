/*
 * Copyright (c) OSGi Alliance (2012, 2017). All Rights Reserved.
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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Synchronous Listener for Configuration Events. When a
 * {@code ConfigurationEvent} is fired, it is synchronously delivered to all
 * {@code SynchronousConfigurationListener}s.
 * <p>
 * {@code SynchronousConfigurationListener} objects are registered with the
 * Framework service registry and are synchronously notified with a
 * {@code ConfigurationEvent} object when an event is fired.
 * <p>
 * {@code SynchronousConfigurationListener} objects can inspect the received
 * {@code ConfigurationEvent} object to determine its type, the PID of the
 * {@code Configuration} object with which it is associated, and the
 * Configuration Admin service that fired the event.
 * <p>
 * Security Considerations. Bundles wishing to synchronously monitor
 * configuration events will require
 * {@code ServicePermission[SynchronousConfigurationListener,REGISTER]} to
 * register a {@code SynchronousConfigurationListener} service.
 *
 * @author $Id$
 * @since 1.5
 * @ThreadSafe
 */
@ConsumerType
public interface SynchronousConfigurationListener extends ConfigurationListener {
	// Marker interface
}
