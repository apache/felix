/*
 * Copyright (c) OSGi Alliance (2012, 2014). All Rights Reserved.
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

package org.osgi.resource.dto;

import org.osgi.dto.DTO;
import org.osgi.resource.Wire;

/**
 * Data Transfer Object for a Wire.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class WireDTO extends DTO {
    /**
	 * Reference to the Capability for the wire.
	 * 
	 * @see Wire#getCapability()
	 */
    public CapabilityRefDTO  capability;

    /**
	 * Reference to the Requirement for the wire.
	 * 
	 * @see Wire#getRequirement()
	 */
    public RequirementRefDTO requirement;

    /**
	 * The identifier of the provider resource for the wire.
	 * 
	 * @see ResourceDTO#id
	 * @see Wire#getProvider()
	 */
    public int               provider;

    /**
	 * The identifier of the requiring resource for the wire.
	 * 
	 * @see ResourceDTO#id
	 * @see Wire#getRequirer()
	 */
    public int               requirer;
}
