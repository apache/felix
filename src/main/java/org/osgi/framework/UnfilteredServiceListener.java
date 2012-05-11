/*
 * Copyright (c) OSGi Alliance (2011). All Rights Reserved.
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

package org.osgi.framework;

import org.osgi.framework.hooks.service.ListenerHook;

/**
 * A {@code ServiceEvent} listener that does <i>not</i> filter based upon any
 * filter string specified to
 * {@link BundleContext#addServiceListener(ServiceListener, String)}. Using an
 * {@code UnfilteredServiceListener} and specifying a filter string to
 * {@link BundleContext#addServiceListener(ServiceListener, String)} allows the
 * listener to receive all {@code ServiceEvent} objects while still advising
 * {@link ListenerHook} implementation of the service interests in the filter
 * string.
 * 
 * For example, an implementation of Declarative Services would add an
 * {@code UnfilteredServiceListener} with a filter string listing all the
 * services referenced by all the service components. The Declarative Services
 * implementation would receive all {@code ServiceEvent} objects for internal
 * processing and a Remote Services discovery service implementation can observe
 * the service interests of the service components using a {@link ListenerHook}.
 * When the set of service components being processed changes, the Declarative
 * Services implementation would re-add the {@code UnfilteredServiceListener}
 * with an updated filter string.
 * 
 * <p>
 * When a {@code ServiceEvent} is fired, it is synchronously delivered to an
 * {@code UnfilteredServiceListener}. The Framework may deliver
 * {@code ServiceEvent} objects to an {@code UnfilteredServiceListener} out of
 * order and may concurrently call and/or reenter an
 * {@code UnfilteredServiceListener}.
 * 
 * <p>
 * An {@code UnfilteredServiceListener} object is registered with the Framework
 * using the {@code BundleContext.addServiceListener} method.
 * {@code UnfilteredServiceListener} objects are called with a
 * {@code ServiceEvent} object when a service is registered, modified, or is in
 * the process of unregistering.
 * 
 * <p>
 * {@code ServiceEvent} object delivery to {@code UnfilteredServiceListener}
 * objects are <i>not</i> filtered by the filter specified when the listener was
 * registered. If the Java Runtime Environment supports permissions, then some
 * filtering is done. {@code ServiceEvent} objects are only delivered to the
 * listener if the bundle which defines the listener object's class has the
 * appropriate {@code ServicePermission} to get the service using at least one
 * of the named classes under which the service was registered.
 * 
 * @see ServiceEvent
 * @see ServicePermission
 * @ThreadSafe
 * @since 1.7
 * @version $Id: 543a345802f8dc7a49d29e8fb7aee7004ee2b329 $
 */

public interface UnfilteredServiceListener extends ServiceListener {
	// This is a marker interface
}
