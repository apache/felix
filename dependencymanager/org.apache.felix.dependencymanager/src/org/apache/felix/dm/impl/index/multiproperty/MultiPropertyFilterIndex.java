/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.apache.felix.dm.impl.index.multiproperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class MultiPropertyFilterIndex implements FilterIndex, ServiceTrackerCustomizer {

	private final Object m_lock = new Object();

	private ServiceTracker m_tracker;

	private BundleContext m_context;

	private Map<String, Property> m_configProperties = new LinkedHashMap<>();

	private List<String> m_negatePropertyKeys = new ArrayList<>();

	private final Map<MultiPropertyKey, List<ServiceReference>> m_keyToServiceReferencesMap = new HashMap<>();

	private final Map<MultiPropertyKey, List<ServiceListener>> m_keyToListenersMap = new HashMap<>();

	private final Map<ServiceListener, String> m_listenerToFilterMap = new HashMap<>();

	public MultiPropertyFilterIndex(String configString) {
		parseConfig(configString);
	}

	public boolean isApplicable(String clazz, String filterString) {
		Filter filter = createFilter(clazz, filterString);

		if (!filter.isValid()) {
			return false;
		}
		// compare property keys to the ones in the configuration
		Set<String> filterPropertyKeys = filter.getPropertyKeys();
		if (m_configProperties.size() != filterPropertyKeys.size()) {
			return false;
		}

		Iterator<String> filterPropertyKeysIterator = filterPropertyKeys.iterator();
		while (filterPropertyKeysIterator.hasNext()) {
			String filterPropertyKey = filterPropertyKeysIterator.next();
			if (!m_configProperties.containsKey(filterPropertyKey)) {
				return false;
			} else if ((m_configProperties.get(filterPropertyKey)).isNegate() != filter.getProperty(filterPropertyKey).isNegate()) {
				// negation should be equal
				return false;
			} else if (!filter.getProperty(filterPropertyKey).isNegate() && filter.getProperty(filterPropertyKey).getValue().equals("*")) {
				// no wildcards without negation allowed
				return false;
			}
		}
		// our properties match so we're applicable
		return true;
	}

	public boolean isApplicable(ServiceReference ref) {
		String[] propertyKeys = ref.getPropertyKeys();
		TreeSet<String> referenceProperties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < propertyKeys.length; i++) {
			referenceProperties.add(propertyKeys[i]);
		}
		Iterator<String> iterator = m_configProperties.keySet().iterator();
		while (iterator.hasNext()) {
			String item = iterator.next();
			Property configProperty = m_configProperties.get(item);
			if (!configProperty.isNegate() && !(referenceProperties.contains(item))) {
				return false;
			} else if (configProperty.isNegate() && referenceProperties.contains(item)) {
				return false;
			}
		}
		return true;
	}

	private void parseConfig(String configString) {
		String[] propertyConfigs = configString.split(",");
		for (int i = 0; i < propertyConfigs.length; i++) {
			String propertyConfig = propertyConfigs[i];
			Property property = new Property();
			String key;
			String value = null;
			if (propertyConfig.startsWith("!")) {
				property.setNegate(true);
				key = propertyConfig.substring(1);
			} else if (propertyConfig.startsWith("#")) {
				property.setPermute(false);
				key = propertyConfig.substring(1);
			} else {
				key = propertyConfig;
			}
			if (key.endsWith("*")) {
				key = key.substring(0, key.indexOf("*"));
				value = "*";
			}
			property.setKey(key.toLowerCase());
			property.addValue(value, property.isNegate());
			m_configProperties.put(key.toLowerCase(), property);
			if (property.isNegate()) {
				m_negatePropertyKeys.add(key);
			}
		}
	}

	protected Collection<Property> getProperties() {
		return m_configProperties.values();
	}

	protected MultiPropertyKey createKeyFromFilter(String clazz, String filterString) {
		int filterSize = m_configProperties.size();
		return createFilter(clazz, filterString).createKey(filterSize);
	}

	//KEYS OF A FILTER
	private Filter createFilter(String clazz, String filterString) {
		String filterStringWithObjectClass = filterString;
		if (clazz != null && !clazz.isEmpty()) {
			if (filterString != null) {
				if (!filterStringWithObjectClass.startsWith("(&(objectClass=")) {
					filterStringWithObjectClass = "(&(objectClass=" + clazz + ")" + filterString + ")";
				}
			} else {
				filterStringWithObjectClass = "(objectClass=" + clazz + ")";
			}
		}
		Filter filter = Filter.parse(filterStringWithObjectClass);
		return filter;
	}

	public List<MultiPropertyKey> createKeys(ServiceReference reference) {
		List<MultiPropertyKey> results = new ArrayList<>();

		String[] keys = reference.getPropertyKeys();
		Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);

		MultiPropertyKey multiPropertyKey = new MultiPropertyKey(0);
		List<MultiPropertyKey> permutations = new ArrayList<>();

		List<List<MultiPropertyKey>> sets = new ArrayList<>();

		for (int i = 0; i < keys.length; i++) {
			String key = null;
			boolean hasUpperCase = !keys[i].equals(keys[i].toLowerCase());
			if(hasUpperCase) {
				key = keys[i].toLowerCase(); 
			} else  {
				key = keys[i];
			}
			
			if (m_configProperties.containsKey(key)) {
				Object valueObject = reference.getProperty(key);
				if (valueObject instanceof String[]) {
					String[] values = (String[]) valueObject;
					if (m_configProperties.get(key).isPermute()) {
						sets.add(getPermutations(key, values));
					} else {
						List<MultiPropertyKey> singleValues = new ArrayList<>();
						for (int v = 0; v < values.length; v++) {
							MultiPropertyKey single = new MultiPropertyKey(0);
							single.append(toKey(key, values[v]));
							singleValues.add(single);
						}
						sets.add(singleValues);
					}
				} else {
					multiPropertyKey.append(toKey(key, (String) valueObject.toString()));
				}
			}
		}

		if (permutations != null && !permutations.isEmpty()) {
			for (MultiPropertyKey permutation : permutations) {
				permutation.append(multiPropertyKey);
				results.add(permutation);
			}
		} else {
			if (!sets.isEmpty()) {

				List<List<MultiPropertyKey>> carthesianProductMultiProperty = carthesianProductMultiProperty(0, sets);
				for (List<MultiPropertyKey> keyList : carthesianProductMultiProperty) {
					MultiPropertyKey merged = new MultiPropertyKey(0);
					merged.append(multiPropertyKey);
					for (MultiPropertyKey single : keyList) {

						merged.append(single);

					}
					results.add(merged);
				}

			} else {

				results.add(multiPropertyKey);
			}
		}
		return results;
	}

	private List<List<MultiPropertyKey>> carthesianProductMultiProperty(int index, List<List<MultiPropertyKey>> sets) {
		List<List<MultiPropertyKey>> result = new ArrayList<>();
		if (index == sets.size()) {
			result.add(new ArrayList<MultiPropertyKey>());
		} else {
			List<MultiPropertyKey> set = sets.get(index);
			for (int i = 0; i < set.size(); i++) {
				MultiPropertyKey object = set.get(i);
				List<List<MultiPropertyKey>> pSets = carthesianProductMultiProperty(index + 1, sets);
				for (int j = 0; j < pSets.size(); j++) {
					List<MultiPropertyKey> pSet = pSets.get(j);
					pSet.add(object);
					result.add(pSet);
				}
			}
		}
		return result;
	}


	List<MultiPropertyKey> getPermutations(String key, String[] values) {
		List<MultiPropertyKey> results = new ArrayList<>();
		Arrays.sort(values, String.CASE_INSENSITIVE_ORDER);
		for (int v = 0; v < values.length; v++) {
			String processValue = values[v];
			List<String> items = new ArrayList<>();
			items.add(processValue);
			// per value get combinations
			List<String> subItems = new ArrayList<>(items);
			for (int w = v; w < values.length; w++) {
				// make a copy of the current list
				subItems = new ArrayList<>(subItems);
				if (w != v) {
					String value = values[w];
					subItems.add(value);
				}
				results.add(toKey(key, subItems));
			}
		}
		return results;
	}


	protected MultiPropertyKey toKey(String key, List<String> values) {
		MultiPropertyKey kvc = new MultiPropertyKey(values.size());

		for (int i = 0; i < values.size(); i++) {
			kvc.add(key, values.get(i));
		}
		return kvc;
	}

	protected MultiPropertyKey toKey(String key, Object value) {
		MultiPropertyKey kvc = new MultiPropertyKey(1);
		kvc.add(key, value.toString());
		return kvc;
	}

	@SuppressWarnings("unchecked")
	public Object addingService(ServiceReference reference) {
		BundleContext context;
		synchronized (m_lock) {
			context = m_context;
		}
		if (context != null) {
			return context.getService(reference);
		} else {
			throw new IllegalStateException("No valid bundle context.");
		}
	}

	public void addedService(ServiceReference reference, Object service) {
		if (isApplicable(reference) && shouldBeIndexed(reference)) {
			handleServiceAdd(reference);
		}
	}

	public void modifiedService(ServiceReference reference, Object service) {
		if (isApplicable(reference)) {
			handleServicePropertiesChange(reference);
		}
	}

	public void removedService(ServiceReference reference, Object service) {
		if (isApplicable(reference) && shouldBeIndexed(reference)) {
			handleServiceRemove(reference);
		}
	}

	protected void handleServiceAdd(ServiceReference reference) {
		List<MultiPropertyKey> keys = createKeys(reference);

		synchronized (m_keyToServiceReferencesMap) {
			for (int i = 0; i < keys.size(); i++) {
				List<ServiceReference> references = m_keyToServiceReferencesMap.get(keys.get(i));
				if (references == null) {
					references = new ArrayList<>(1);
					m_keyToServiceReferencesMap.put(keys.get(i), references);
				}
				references.add(reference);
			}

		}
	}

	protected void handleServicePropertiesChange(ServiceReference reference) {

		synchronized (m_keyToServiceReferencesMap) {
			// TODO this is a quite expensive linear scan over the existing collection
			// because we first need to remove any existing references and they can be
			// all over the place :)
			Iterator<List<ServiceReference>> iterator = m_keyToServiceReferencesMap.values().iterator();
			while (iterator.hasNext()) {
				List<ServiceReference> list = iterator.next();
				if (list != null) {
					Iterator<ServiceReference> i2 = list.iterator();
					while (i2.hasNext()) {
						ServiceReference ref = i2.next();
						if (ref.equals(reference)) {
							i2.remove();
						}
					}
				}
			}
			// only re-add the reference when it is still applicable for this filter index
			if (shouldBeIndexed(reference)) {
				List<MultiPropertyKey> keys = createKeys(reference);
				for (int i = 0; i < keys.size(); i++) {
					List<ServiceReference> references = m_keyToServiceReferencesMap.get(keys.get(i));
					if (references == null) {
						references = new ArrayList<>(keys.size());
						m_keyToServiceReferencesMap.put(keys.get(i), references);
					}
					references.add(reference);
				}
			}
		}
	}

	protected void handleServiceRemove(ServiceReference reference) {
		List<MultiPropertyKey> keys = createKeys(reference);
		synchronized (m_keyToServiceReferencesMap) {
			for (int i = 0; i < keys.size(); i++) {
				List<ServiceReference> references = m_keyToServiceReferencesMap.get(keys.get(i));
				if (references != null) {
					references.remove(reference);
					if (references.isEmpty()) {
						m_keyToServiceReferencesMap.remove(keys.get(i));
					} else {
						((ArrayList) reference).trimToSize();
					}
				}
			}
		}
	}

	protected boolean shouldBeIndexed(ServiceReference reference) {
		// is already applicable, so we should only check whether there's a negate field in the filter which has a value in the reference
		Iterator<String> negatePropertyKeyIterator = m_negatePropertyKeys.iterator();
		while (negatePropertyKeyIterator.hasNext()) {
			String negatePropertyKey = negatePropertyKeyIterator.next();
			if (reference.getProperty(negatePropertyKey) != null) {
				return false;
			}
		}
		return true;
	}

	public void open(BundleContext context) {
		synchronized (m_lock) {
			if (m_context != null) {
				throw new IllegalStateException("Filter already open.");
			}
			try {
				m_tracker = new ServiceTracker(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), this);
			} catch (InvalidSyntaxException e) {
				throw new Error();
			}
			m_context = context;
		}
		m_tracker.open(true, true);
	}

	public void close() {
		ServiceTracker tracker;
		synchronized (m_lock) {
			if (m_context == null) {
				throw new IllegalStateException("Filter already closed.");
			}
			tracker = m_tracker;
			m_tracker = null;
			m_context = null;
		}
		tracker.close();
	}

	public List<ServiceReference> getAllServiceReferences(String clazz, String filter) {
		List<ServiceReference> result = new ArrayList<>();
		MultiPropertyKey key = createKeyFromFilter(clazz, filter);
		synchronized (m_keyToServiceReferencesMap) {
			List<ServiceReference> references = m_keyToServiceReferencesMap.get(key);
			if (references != null) {
				result.addAll(references);
			}
		}
		return result;
	}

	public void serviceChanged(ServiceEvent event) {
		if (isApplicable(event.getServiceReference())) {
			List<MultiPropertyKey> keys = createKeys(event.getServiceReference());
			List<ServiceListener> list = new ArrayList<ServiceListener>();
			synchronized (m_keyToListenersMap) {
				for (int i = 0; i < keys.size(); i++) {
					//TODO fix
					MultiPropertyKey key = keys.get(i);
					List<ServiceListener> listeners = m_keyToListenersMap.get(key);
					if (listeners != null) {
						list.addAll(listeners);
					}
				}
			}
			if (list != null) {
				Iterator<ServiceListener> iterator = list.iterator();
				while (iterator.hasNext()) {
					ServiceListener listener = iterator.next();
					listener.serviceChanged(event);
				}
			}
		}
	}

	public void addServiceListener(ServiceListener listener, String filter) {
		MultiPropertyKey key = createKeyFromFilter(null, filter);

		synchronized (m_keyToListenersMap) {
			List<ServiceListener> listeners = m_keyToListenersMap.get(key);
			if (listeners == null) {
				listeners = new CopyOnWriteArrayList<ServiceListener>();
				m_keyToListenersMap.put(key, listeners);
			}
			listeners.add(listener);
			m_listenerToFilterMap.put(listener, filter);
		}
	}

	public void removeServiceListener(ServiceListener listener) {
		synchronized (m_keyToListenersMap) {
			String filter = m_listenerToFilterMap.remove(listener);
			if (filter != null) {
				// the listener does exist
				MultiPropertyKey key = createKeyFromFilter(null, filter);

				boolean result = filter != null;
				if (result) {
					List<ServiceListener> listeners = m_keyToListenersMap.get(key);
					if (listeners != null) {
						listeners.remove(listener);
						if (listeners.isEmpty()) {
							m_keyToListenersMap.remove(key);
						}
					}
					// TODO actually, if listeners == null that would be strange....
				}
			}
		}
	}

	protected Collection<ServiceListener> getServiceListeners() {
		return m_listenerToFilterMap.keySet();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" dMultiPropertyExactFilter[");
		sb.append("K2L: " + m_keyToListenersMap.size());
		sb.append(", K2SR: " + m_keyToServiceReferencesMap.size());
		sb.append(", L2F: " + m_listenerToFilterMap.size());
		sb.append("]");
		return sb.toString();
	}

	@Override
	public void swappedService(ServiceReference reference, Object service, ServiceReference newReference, Object newService) {
		addedService(newReference, newService);
		removedService(reference, service);
	}
}
