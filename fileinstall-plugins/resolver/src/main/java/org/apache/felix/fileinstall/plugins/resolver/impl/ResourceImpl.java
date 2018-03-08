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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ResourceImpl implements Resource {

	private final Map<String, List<Requirement>> requirements = new HashMap<>();
	private final Map<String, List<Capability>> capabilities = new HashMap<>();

	void addRequirement(Requirement req) {
		if (req.getResource() != this) {
            req = RequirementImpl.copy(req, this);
        }

		List<Requirement> list = this.requirements.get(req.getNamespace());
		if (list == null) {
			list = new LinkedList<>();
			this.requirements.put(req.getNamespace(), list);
		}
		list.add(req);
	}

	void addCapability(Capability cap) {
		if (cap.getResource() != this) {
            cap = CapabilityImpl.copy(cap, this);
        }

		List<Capability> list = this.capabilities.get(cap.getNamespace());
		if (list == null) {
			list = new LinkedList<>();
			this.capabilities.put(cap.getNamespace(), list);
		}
		list.add(cap);
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> result;
		if (namespace == null) {
			result = new LinkedList<>();
			for (List<Capability> list : this.capabilities.values()) {
                result.addAll(list);
            }
		} else {
			result = this.capabilities.get(namespace);
		}
		return result != null ? result : Collections.<Capability>emptyList();
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		List<Requirement> result;
		if (namespace == null) {
			result = new LinkedList<>();
			for (List<Requirement> list : this.requirements.values()) {
                result.addAll(list);
            }
		} else {
			result = this.requirements.get(namespace);
		}
		return result != null ? result : Collections.<Requirement>emptyList();
	}

	@Override
	public String toString() {
		String name = "<unknown>";
		List<Capability> idCaps = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (idCaps != null && !idCaps.isEmpty()) {
			Object nameObj = idCaps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
			if (nameObj instanceof String) {
                name = (String) nameObj;
            }
		}
		return name;
	}

}
