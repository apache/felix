/*
 * Copyright (c) OSGi Alliance (2013, 2016). All Rights Reserved.
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

package org.osgi.service.component.runtime;

import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;

/**
 * The {@code ServiceComponentRuntime} service represents the Declarative
 * Services actor, known as Service Component Runtime (SCR), that manages the
 * service components and their life cycle. The {@code ServiceComponentRuntime}
 * service allows introspection of the components managed by Service Component
 * Runtime.
 * 
 * <p>
 * This service differentiates between a {@link ComponentDescriptionDTO} and a
 * {@link ComponentConfigurationDTO}. A {@link ComponentDescriptionDTO} is a
 * representation of a declared component description. A
 * {@link ComponentConfigurationDTO} is a representation of an actual instance
 * of a declared component description parameterized by component properties.
 * <p>
 * 
 * Access to this service requires the
 * {@code ServicePermission[ServiceComponentRuntime, GET]} permission. It is
 * intended that only administrative bundles should be granted this permission
 * to limit access to the potentially intrusive methods provided by this
 * service.
 * 
 * @ThreadSafe
 * @since 1.3
 * @author $Id$
 */
@ProviderType
public interface ServiceComponentRuntime {

	/**
	 * Returns the component descriptions declared by the specified active
	 * bundles.
	 * 
	 * <p>
	 * Only component descriptions from active bundles are returned. If the
	 * specified bundles have no declared components or are not active, an empty
	 * collection is returned.
	 * 
	 * @param bundles The bundles whose declared component descriptions are to
	 *        be returned. Specifying no bundles, or the equivalent of an empty
	 *        {@code Bundle} array, will return the declared component
	 *        descriptions from all active bundles.
	 * @return The declared component descriptions of the specified active
	 *         {@code bundles}. An empty collection is returned if there are no
	 *         component descriptions for the specified active bundles.
	 */
	Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs(Bundle... bundles);

	/**
	 * Returns the {@link ComponentDescriptionDTO} declared with the specified name
	 * by the specified bundle.
	 * 
	 * <p>
	 * Only component descriptions from active bundles are returned.
	 * {@code null} if no such component is declared by the given {@code bundle}
	 * or the bundle is not active.
	 * 
	 * @param bundle The bundle declaring the component description. Must not be
	 *        {@code null}.
	 * @param name The name of the component description. Must not be
	 *        {@code null}.
	 * @return The declared component description or {@code null} if the
	 *         specified bundle is not active or does not declare a component
	 *         description with the specified name.
	 */
	ComponentDescriptionDTO getComponentDescriptionDTO(Bundle bundle, String name);

	/**
	 * Returns the component configurations for the specified component
	 * description.
	 * 
	 * @param description The component description. Must not be {@code null}.
	 * @return A collection containing a snapshot of the current component
	 *         configurations for the specified component description. An empty
	 *         collection is returned if there are none or if the provided
	 *         component description does not belong to an active bundle.
	 */
	Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(ComponentDescriptionDTO description);

	/**
	 * Returns whether the specified component description is currently enabled.
	 * 
	 * <p>
	 * The enabled state of a component description is initially set by the
	 * {@link ComponentDescriptionDTO#defaultEnabled enabled} attribute of the
	 * component description.
	 * 
	 * @param description The component description. Must not be {@code null}.
	 * @return {@code true} if the specified component description is currently
	 *         enabled. Otherwise, {@code false}.
	 * @see #enableComponent(ComponentDescriptionDTO)
	 * @see #disableComponent(ComponentDescriptionDTO)
	 * @see ComponentContext#disableComponent(String)
	 * @see ComponentContext#enableComponent(String)
	 */
	boolean isComponentEnabled(ComponentDescriptionDTO description);

	/**
	 * Enables the specified component description.
	 * <p>
	 * If the specified component description is currently enabled, this method
	 * has no effect.
	 * <p>
	 * This method must return after changing the enabled state of the specified
	 * component description. Any actions that result from this, such as
	 * activating or deactivating a component configuration, must occur
	 * asynchronously to this method call.
	 * 
	 * @param description The component description to enable. Must not be
	 *            {@code null}.
	 * @return A promise that will be resolved when the actions that result from
	 *         changing the enabled state of the specified component have
	 *         completed. If the provided description does not belong to an
	 *         active bundle, a failed promise is returned.
	 * @see #isComponentEnabled(ComponentDescriptionDTO)
	 */
	Promise<Void> enableComponent(ComponentDescriptionDTO description);

	/**
	 * Disables the specified component description.
	 * <p>
	 * If the specified component description is currently disabled, this method
	 * has no effect.
	 * <p>
	 * This method must return after changing the enabled state of the specified
	 * component description. Any actions that result from this, such as
	 * activating or deactivating a component configuration, must occur
	 * asynchronously to this method call.
	 * 
	 * @param description The component description to disable. Must not be
	 *            {@code null}.
	 * @return A promise that will be resolved when the actions that result from
	 *         changing the enabled state of the specified component have
	 *         completed. If the provided description does not belong to an
	 *         active bundle, a failed promise is returned.
	 * @see #isComponentEnabled(ComponentDescriptionDTO)
	 */
	Promise<Void> disableComponent(ComponentDescriptionDTO description);
}
