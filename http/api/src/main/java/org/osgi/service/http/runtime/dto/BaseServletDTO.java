/*
 * Copyright (c) OSGi Alliance (2012, 2017). All Rights Reserved.
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

import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents common information about a {@code javax.servlet.Servlet} service.
 *
 * @NotThreadSafe
 * @author $Id$
 */
public abstract class BaseServletDTO extends DTO {
	/**
	 * The name of the servlet. This value is never {@code null}, unless this
	 * object represents a {@code FailedServletDTO} or a
	 * {@code FailedErrorPageDTO} where the value might be {@code null}.
	 */
	public String				name;

	/**
	 * The information string from the servlet.
	 * <p>
	 * This is the value returned by the {@code Servlet.getServletInfo()}
	 * method. For a {@code FailedServletDTO} or a {@code FailedErrorPageDTO}
	 * this is always {@code null}.
	 */
	public String				servletInfo;

	/**
	 * Specifies whether the servlet supports asynchronous processing.
	 */
	public boolean				asyncSupported;

	/**
	 * The servlet initialization parameters as provided during registration of
	 * the servlet. Additional parameters like the Http Service Runtime
	 * attributes are not included. If the service has no initialization
	 * parameters, the map is empty.
	 */
	public Map<String, String>	initParams;

	/**
	 * The service id of the servlet context for the servlet represented by this
	 * DTO.
	 */
	public long		servletContextId;

	/**
	 * Service property identifying the servlet. In the case of a servlet
	 * registered in the service registry and picked up by a Http Whiteboard
	 * Implementation, this value is not negative and corresponds to the service
	 * id in the registry. If the servlet has not been registered in the service
	 * registry, the value is negative and a unique negative value is generated
	 * by the Http Service Runtime in this case.
	 */
	public long		serviceId;
}
