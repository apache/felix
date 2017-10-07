/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.resolver;

import java.util.Map;

import org.osgi.resource.Resource;

public interface ResolveResult {

	/**
	 * Return the request that was satisfied by this result.
	 */
	ResolveRequest getRequest();

	/**
	 * Return the resources to install. Each resource is mapped to a location,
	 * which is either the location field of an existing installed bundle or the
	 * physical URL of a location from which the bundle can be installed.
	 */
	Map<Resource, String> getResources();

}
