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
 * @author $Id: f519b5fddd8002ffc252d039856acc8ba6f422d6 $
 */
public class ComponentConfigurationDTO extends DTO {
	/**
	 * The component configuration is unsatisfied due to a missing required
	 * configuration.
	 */
	public static final int			UNSATISFIED_CONFIGURATION	= 1;

	/**
	 * The component configuration is unsatisfied due to an unsatisfied
	 * reference.
	 */
	public static final int			UNSATISFIED_REFERENCE		= 2;

	/**
	 * The component configuration is satisfied.
	 * 
	 * <p>
	 * Any {@link ComponentDescriptionDTO#serviceInterfaces services} declared
	 * by the component description are registered.
	 */
	public static final int			SATISFIED					= 4;

	/**
	 * The component configuration is active.
	 * 
	 * <p>
	 * This is the normal operational state of a component configuration.
	 */
	public static final int			ACTIVE						= 8;

	/**
	 * The representation of the component configuration's component
	 * description.
	 */
	public ComponentDescriptionDTO	description;

	/**
	 * The id of the component configuration.
	 * 
	 * <p>
	 * The id is a non-persistent, unique value assigned at runtime. The id is
	 * also available as the {@code component.id} component property.
	 */
	public long						id;

	/**
	 * The current state of the component configuration.
	 * 
	 * <p>
	 * This is one of {@link #UNSATISFIED_CONFIGURATION},
	 * {@link #UNSATISFIED_REFERENCE}, {@link #SATISFIED} or {@link #ACTIVE}.
	 */
	public int					state;

	/**
	 * The component properties for the component configuration.
	 * 
	 * @see ComponentContext#getProperties()
	 */
	public Map<String, Object>	properties;

	/**
	 * The satisfied references.
	 * 
	 * <p>
	 * Each {@link SatisfiedReferenceDTO} in the array represents a satisfied
	 * reference of the component configuration. The array must be empty if the
	 * component configuration has no satisfied references.
	 */
	public SatisfiedReferenceDTO[]		satisfiedReferences;

	/**
	 * The unsatisfied references.
	 * 
	 * <p>
	 * Each {@link UnsatisfiedReferenceDTO} in the array represents an
	 * unsatisfied reference of the component configuration. The array must be
	 * empty if the component configuration has no unsatisfied references.
	 */
	public UnsatisfiedReferenceDTO[]	unsatisfiedReferences;
}
