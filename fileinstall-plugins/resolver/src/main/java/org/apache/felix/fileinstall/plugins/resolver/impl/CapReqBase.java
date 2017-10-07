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
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Resource;

public class CapReqBase {

	protected final String namespace;
	protected final Map<String, String> directives;
	protected final Map<String, Object> attribs;
	protected final Resource resource;

	public CapReqBase(String namespace, Map<String,String> directives, Map<String, Object> attribs, Resource resource) {
		this.namespace = namespace;
		this.directives = new HashMap<>(directives);
		this.attribs = new HashMap<>(attribs);
		this.resource = resource;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attribs);
	}

	public Resource getResource() {
		return resource;
	}
	
}
