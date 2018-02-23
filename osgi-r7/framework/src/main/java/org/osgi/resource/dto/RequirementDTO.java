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

import java.util.Map;
import org.osgi.dto.DTO;
import org.osgi.resource.Requirement;

/**
 * Data Transfer Object for a Requirement.
 * 
 * @author $Id$
 * @NotThreadSafe
 */
public class RequirementDTO extends DTO {
    /**
     * The unique identifier of the requirement.
     * 
     * <p>
     * This identifier is transiently assigned and may vary across restarts.
     */
    public int                 id;

    /**
	 * The namespace for the requirement.
	 * 
	 * @see Requirement#getNamespace()
	 */
    public String              namespace;

    /**
	 * The directives for the requirement.
	 * 
	 * @see Requirement#getDirectives()
	 */
    public Map<String, String> directives;

    /**
	 * The attributes for the requirement.
	 * 
	 * <p>
	 * The value type must be a numerical type, Boolean, String, DTO or an array
	 * of any of the former.
	 * 
	 * @see Requirement#getAttributes()
	 */
    public Map<String, Object> attributes;

    /**
	 * The identifier of the resource declaring the requirement.
	 * 
	 * @see ResourceDTO#id
	 * @see Requirement#getResource()
	 */
    public int                 resource;
}
