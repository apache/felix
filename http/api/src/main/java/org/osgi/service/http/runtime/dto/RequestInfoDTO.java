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

package org.osgi.service.http.runtime.dto;

import org.osgi.dto.DTO;

/**
 * Represents the services used to process a specific request.
 * 
 * @NotThreadSafe
 * @author $Id$
 */
public class RequestInfoDTO extends DTO {
	/**
	 * The path of the request relative to the root.
	 */
	public String				path;

	/**
	 * The service id of the servlet context processing the request represented
	 * by this DTO.
	 */
	public long		servletContextId;
	
	/**
	 * The servlet filters processing this request. If no servlet filters are
	 * called for processing this request, an empty array is returned.
	 */
	public FilterDTO[] filterDTOs;
	
	/**
	 * The servlet processing this request. If the request is processed by a
	 * servlet, this field points to the DTO of the servlet. If the request is
	 * processed by another type of component like a resource, this field is
	 * {@code null}.
	 */
	public ServletDTO servletDTO;

	/**
	 * The resource processing this request. If the request is processed by a
	 * resource, this field points to the DTO of the resource. If the request is
	 * processed by another type of component like a servlet, this field is
	 * {@code null}.
	 */
	public ResourceDTO resourceDTO;
}
