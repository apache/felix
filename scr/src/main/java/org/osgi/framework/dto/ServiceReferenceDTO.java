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

package org.osgi.framework.dto;

import java.util.Map;
import org.osgi.dto.DTO;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Data Transfer Object for a ServiceReference.
 * 
 * <p>
 * {@code ServiceReferenceDTO}s for all registered services can be obtained from
 * a {@link FrameworkDTO}. An installed Bundle can be adapted to provide a
 * {@code ServiceReferenceDTO[]} of the services registered by the Bundle. A
 * {@code ServiceReferenceDTO} obtained from a framework must convert service
 * property values which are not valid value types for DTOs to type
 * {@code String} using {@code String.valueOf(Object)}.
 * 
 * @author $Id: 2c70b84f28c41fb51c488cb03950a46188ea209f $
 * @NotThreadSafe
 */
public class ServiceReferenceDTO extends DTO {
    /**
	 * The id of the service.
	 * 
	 * @see Constants#SERVICE_ID
	 */
    public long                id;

    /**
	 * The id of the bundle that registered the service.
	 * 
	 * @see ServiceReference#getBundle()
	 */
    public long                bundle;

    /**
	 * The properties for the service.
	 * 
	 * The value type must be a numerical type, Boolean, String, DTO or an array
	 * of any of the former.
	 * 
	 * @see ServiceReference#getProperty(String)
	 */
    public Map<String, Object> properties;

    /**
	 * The ids of the bundles that are using the service.
	 * 
	 * @see ServiceReference#getUsingBundles()
	 */
    public long[]              usingBundles;
}
