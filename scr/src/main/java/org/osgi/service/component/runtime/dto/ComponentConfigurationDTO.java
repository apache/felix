/*
 * Copyright (c) OSGi Alliance (2013, 2014). All Rights Reserved.
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

package org.osgi.service.component.runtime.dto;

import java.util.Map;
import org.osgi.dto.DTO;
import org.osgi.service.component.ComponentContext;

/**
 * A representation of an actual instance of a declared component description
 * parameterized by component properties.
 * 
 * @since 1.3
 * @NotThreadSafe
 * @author $Id: b3f49d694f497e55dc7ffed0e7d910fb3bab83da $
 */
public class ComponentConfigurationDTO extends DTO {
	/**
	 * The component configuration is unsatisfied.
	 * 
	 * <p>
	 * This is the initial state of a component configuration. When the
	 * component configuration becomes satisfied it enters the
	 * {@link #SATISFIED} state.
	 */
	public static final int		UNSATISFIED		= 1;

	/**
	 * The component configuration is satisfied.
	 * 
	 * <p>
	 * Any {@link ComponentDescriptionDTO#serviceInterfaces services} declared by
	 * the component description are registered.
	 * 
	 * If the component configuration becomes unsatisfied for any reason, any
	 * declared services must be unregistered and the component configuration
	 * returns to the {@link #UNSATISFIED} state.
	 */
	public static final int		SATISFIED		= 2;

	/**
	 * The component configuration is active.
	 * 
	 * <p>
	 * This is the normal operational state of a component configuration. The
	 * component configuration will move to the {@link #SATISFIED} state when it
	 * is deactivated.
	 */
	public static final int			ACTIVE		= 4;

	/**
	 * The representation of the component configuration's component
	 * description.
	 */
	public ComponentDescriptionDTO	description;

	/**
	 * The current state of the component configuration.
	 * 
	 * <p>
	 * This is one of {@link #UNSATISFIED}, {@link #SATISFIED} or
	 * {@link #ACTIVE}.
	 */
	public int					state;

	/**
	 * The component properties for the component configuration.
	 * 
	 * @see ComponentContext#getProperties()
	 */
	public Map<String, Object>	properties;

	/**
	 * The currently bound references.
	 * 
	 * <p>
	 * Each {@link BoundReferenceDTO} in the array represents a service bound to a
	 * reference of the component configuration. The array will be empty if the
	 * component configuration has no bound references.
	 */
	public BoundReferenceDTO[]		boundReferences;
	
	/**
	 * The id of the component description.
	 * 
	 * <p>
	 * The id is a non-persistent, unique value assigned at runtime. The id is
	 * also available as the {@code component.id} component property.
	 */
	public long					id;
}
