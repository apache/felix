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

import aQute.bnd.service.Registry;

class BasicRegistry implements Registry {
	
	private final Map<Class<?>, List<Object>> plugins = new HashMap<>();
	
	synchronized <T> BasicRegistry put(Class<T> clazz, T plugin) {
		List<Object> list = plugins.get(clazz);
		if (list == null) {
			list = new LinkedList<>();
			plugins.put(clazz, list);
		}
		list.add(plugin);
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> List<T> getPlugins(Class<T> clazz) {
		List<T> objs = (List<T>) plugins.get(clazz);
		return objs != null ? Collections.<T>unmodifiableList(objs) : Collections.<T>emptyList();
	}

	@Override
	public <T> T getPlugin(Class<T> clazz) {
		List<T> l = getPlugins(clazz);
		return (l != null && !l.isEmpty()) ? l.get(0) : null;
	}

}
