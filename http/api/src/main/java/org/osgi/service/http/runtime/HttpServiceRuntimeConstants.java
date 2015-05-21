/*
 * Copyright (c) OSGi Alliance (2012, 2015). All Rights Reserved.
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

package org.osgi.service.http.runtime;

/**
 * Defines standard names for Http Runtime Service constants.
 * 
 * @author $Id$
 */
public final class HttpServiceRuntimeConstants {
	private HttpServiceRuntimeConstants() {
		// non-instantiable
	}

	/**
	 * Http Runtime Service service property specifying the endpoints upon which
	 * the Http Whiteboard implementation is listening.
	 * 
	 * <p>
	 * An endpoint value is a URL or a relative path, to which the Http
	 * Whiteboard implementation is listening. For example,
	 * {@code http://192.168.1.10:8080/} or {@code /myapp/}. A relative path may
	 * be used if the scheme and authority parts of the URL are not known, e.g.
	 * in a bridged Http Whiteboard implementation. If the Http Whiteboard
	 * implementation is serving the root context and neither scheme nor
	 * authority is known, the value of the property is "/". Both, a URL and a
	 * relative path, must end with a slash.
	 * <p>
	 * An Http Whiteboard implementation can be listening on multiple endpoints.
	 * 
	 * <p>
	 * The value of this service property must be of type {@code String},
	 * {@code String[]}, or {@code Collection<String>}.
	 */
	public static final String	HTTP_SERVICE_ENDPOINT	= "osgi.http.endpoint";

	/**
	 * Http Runtime Service service property to associate the Http Runtime
	 * Service with one or more HttpService services.
	 * 
	 * <p>
	 * If this Http Whiteboard implementation also implements the Http Service
	 * Specification, this service property is set to a collection of
	 * {@code service.id} for the {@code HttpService} services registered by
	 * this implementation.
	 * 
	 * <p>
	 * The value of this service property must be of type
	 * {@code Collection<Long>}.
	 */
	public static final String	HTTP_SERVICE_ID		= "osgi.http.service.id";
}
