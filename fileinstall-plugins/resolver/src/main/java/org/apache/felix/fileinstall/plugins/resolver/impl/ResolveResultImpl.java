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
package org.apache.felix.fileinstall.plugins.resolver.impl;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.felix.fileinstall.plugins.resolver.ResolveRequest;
import org.apache.felix.fileinstall.plugins.resolver.ResolveResult;
import org.osgi.resource.Resource;

public class ResolveResultImpl implements ResolveResult {
	
	private final ResolveRequest request;
	private final Map<Resource, String> resourceMap = new IdentityHashMap<>();

	public ResolveResultImpl(ResolveRequest request) {
		this.request = request;
	}

	@Override
	public ResolveRequest getRequest() {
		return request;
	}

	@Override
	public Map<Resource, String> getResources() {
		return Collections.unmodifiableMap(resourceMap);
	}

	void addResource(Resource resource, String location) {
		resourceMap.put(resource, location);
	}


}