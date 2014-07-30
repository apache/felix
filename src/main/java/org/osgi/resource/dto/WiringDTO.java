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

import java.util.List;
import org.osgi.dto.DTO;
import org.osgi.resource.Wiring;

/**
 * Data Transfer Object for a Wiring node.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class WiringDTO extends DTO {
    /**
     * The unique identifier of the wiring node.
     * 
     * <p>
     * This identifier is transiently assigned and may vary across restarts.
     */
    public int                     id;

    /**
	 * The references to the capabilities for the wiring node.
	 * 
	 * @see Wiring#getResourceCapabilities(String)
	 */
    public List<CapabilityRefDTO>  capabilities;

    /**
	 * The references to the requirements for the wiring node.
	 * 
	 * @see Wiring#getResourceRequirements(String)
	 */
    public List<RequirementRefDTO> requirements;

    /**
	 * The provided wires for the wiring node.
	 * 
	 * @see Wiring#getProvidedResourceWires(String)
	 */
    public List<WireDTO>           providedWires;

    /**
	 * The required wires for the wiring node.
	 * 
	 * @see Wiring#getRequiredResourceWires(String)
	 */
    public List<WireDTO>           requiredWires;

    /**
	 * The identifier of the resource associated with the wiring node.
	 * 
	 * @see ResourceDTO#id
	 * @see Wiring#getResource()
	 */
    public int                     resource;
}
