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

/**
 * Defines standard constants for the DTOs.
 */
public final class DTOConstants {
	private DTOConstants() {
		// non-instantiable
	}

	/**
	 * Failure reason is unknown.
	 */
	public static final int	FAILURE_REASON_UNKNOWN						= 0;

	/**
	 * No matching {@code ServletContextHelper}.
	 **/
	public static final int	FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING	= 1;

	/**
	 * Matching {@code ServletContextHelper}, but the context is not used due to
	 * a problem with the context.
	 */
	public static final int	FAILURE_REASON_SERVLET_CONTEXT_FAILURE		= 2;

	/**
	 * Service is shadowed by another service.
	 * <p>
	 * For example, a service with the same service properties but a higher
	 * service ranking.
	 */
	public static final int	FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE	= 3;

	/**
	 * An exception occurred during initializing of the service.
	 * <p>
	 * This reason can only happen for servlets and servlet filters.
	 */
	public static final int	FAILURE_REASON_EXCEPTION_ON_INIT			= 4;

	/**
	 * The service is registered in the service registry but getting the service
	 * fails as it returns {@code null}.
	 */
	public static final int	FAILURE_REASON_SERVICE_NOT_GETTABLE			= 5;

	/**
	 * The service is registered in the service registry but the service
	 * properties are invalid.
	 */
	public static final int	FAILURE_REASON_VALIDATION_FAILED			= 6;

	/**
	 * The service is not registered as a prototype scoped service and is
	 * already in use with a servlet context and therefore can't be used with
	 * another servlet context.
	 */
	public static final int	FAILURE_REASON_SERVICE_IN_USE				= 7;

	/**
	 * The servlet is not registered as it is configured to have multipart
	 * enabled, but the bundle containing the servlet has no write permission to
	 * the provided location for the uploaded files.
	 * 
	 * @since 1.1
	 */
	public static final int	FAILURE_REASON_SERVLET_WRITE_TO_LOCATION_DENIED		= 8;

	/**
	 * The servlet is not registered as it is configured to have multipart
	 * enabled, but the whiteboard implementation has no write permission to the
	 * default location for the uploaded files.
	 * 
	 * @since 1.1
	 */
	public static final int	FAILURE_REASON_WHITEBOARD_WRITE_TO_DEFAULT_DENIED	= 9;

	/**
	 * The servlet is not registered as it is configured to have multipart
	 * enabled, but the bundle containing the servlet has no read permission to
	 * the default location for the uploaded files.
	 * 
	 * @since 1.1
	 */
	public static final int	FAILURE_REASON_SERVLET_READ_FROM_DEFAULT_DENIED		= 10;
}
