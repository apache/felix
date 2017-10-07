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

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementImpl extends CapReqBase implements Requirement {

	public static RequirementImpl copy(Requirement req, Resource resource) {
		return new RequirementImpl(req.getNamespace(), req.getDirectives(), req.getAttributes(), resource);
	}

	public RequirementImpl(String namespace, Map<String, String> directives, Map<String, Object> attribs, Resource resource) {
		super(namespace, directives, attribs, resource);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.namespace);

		String filterStr = this.directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		if (filterStr != null) {
            sb.append(":").append(filterStr);
        }

		for (Entry<String,String> directive : this.directives.entrySet()) {
			if (!Namespace.REQUIREMENT_FILTER_DIRECTIVE.equals(directive.getKey())) {
                sb.append(", ").append(directive.getKey()).append(":=").append(directive.getValue());
            }
		}

		for (Entry<String,Object> attrib : this.attribs.entrySet()) {
			sb.append(", ").append(attrib.getKey()).append("=").append(attrib.getValue());
		}

		return sb.toString();
	}

}
