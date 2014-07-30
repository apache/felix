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

package org.osgi.framework.wiring.dto;

import org.osgi.framework.wiring.BundleWire;
import org.osgi.resource.dto.WireDTO;
import org.osgi.resource.dto.WiringDTO;

/**
 * Data Transfer Object for a BundleWire.
 * 
 * <p>
 * {@code BundleWireDTO}s are referenced {@link BundleWiringDTO.NodeDTO}s.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class BundleWireDTO extends WireDTO {
    /**
	 * The identifier of the provider wiring for the bundle wire.
	 * 
	 * @see WiringDTO#id
	 * @see BundleWire#getProviderWiring()
	 */
    public int providerWiring;

    /**
	 * The identifier of the requiring wiring for the bundle wire.
	 * 
	 * @see WiringDTO#id
	 * @see BundleWire#getRequirerWiring()
	 */
    public int requirerWiring;
}
