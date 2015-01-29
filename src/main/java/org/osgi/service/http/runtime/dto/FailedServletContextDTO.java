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

/**
 * Represents a servlet context that is currently not used due to some problem.
 * 
 * The following fields return an empty array for a
 * {@code FailedServletContextDTO}:
 * <ul>
 * <li>{@link ServletContextDTO#servletDTOs}</li>
 * <li>{@link ServletContextDTO#resourceDTOs}</li>
 * <li>{@link ServletContextDTO#filterDTOs}</li>
 * <li>{@link ServletContextDTO#errorPageDTOs}</li>
 * <li>{@link ServletContextDTO#listenerDTOs}</li>
 * </ul>
 * <p>
 * The method {@link ServletContextDTO#attributes} returns an empty map for a
 * {@code FailedServletContextDTO}.
 *
 * @NotThreadSafe
 * @author $Id$
 */
public class FailedServletContextDTO extends ServletContextDTO {

	/**
	 * The reason why the servlet context represented by this DTO is not used.
	 * 
	 * @see DTOConstants#FAILURE_REASON_UNKNOWN
	 * @see DTOConstants#FAILURE_REASON_EXCEPTION_ON_INIT
	 * @see DTOConstants#FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING
	 * @see DTOConstants#FAILURE_REASON_SERVICE_NOT_GETTABLE
	 * @see DTOConstants#FAILURE_REASON_SERVLET_CONTEXT_FAILURE
	 * @see DTOConstants#FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE
	 */
	public int	failureReason;
}
