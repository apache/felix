/*
 * Copyright (c) OSGi Alliance (2012, 2016). All Rights Reserved.
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

package org.osgi.framework.wiring.dto;

import java.util.Set;

import org.osgi.dto.DTO;

/**
 * Data Transfer Object for the wiring graph of the framework.
 * <p>
 * The system bundle can be adapted to provide the {@code FrameworkWiringDTO}.
 * Only the system bundle can be adapted to a {@code FrameworkWiringDTO} object
 * 
 * @author $Id: 1786218bfe09a4d46d9041d96d01b2a71612eb0d $
 * @NotThreadSafe
 * @since 1.3
 */
public class FrameworkWiringDTO extends DTO {
	/**
	 * The set of wiring nodes referenced by the wiring graph of the framework.
	 * <p>
	 * All wiring nodes referenced by wiring node identifiers in the wiring
	 * graph are contained in this set.
	 */
	public Set<BundleWiringDTO.NodeDTO>	wirings;
	/**
	 * The set of resources referenced by the wiring graph of the framework.
	 * <p>
	 * All resources referenced by resource identifiers in the wiring graph are
	 * contained in this set.
	 */
	public Set<BundleRevisionDTO>		resources;
}
