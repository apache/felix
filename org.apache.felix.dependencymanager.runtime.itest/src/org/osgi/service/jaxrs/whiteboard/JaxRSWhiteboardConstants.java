/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
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
package org.osgi.service.jaxrs.whiteboard;

public class JaxRSWhiteboardConstants {
	/**
	 * Service property specifying that a JAX-RS resource should be processed by
	 * the whiteboard.
	 * <p>
	 * The value of this service property must be of type {@code String} or
	 * {@link Boolean} and set to &quot;true&quot; or <code>true</code>.
	 */
	public static final String	JAX_RS_RESOURCE				= "osgi.jaxrs.resource";
}
