/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.dependency.impl;

import org.apache.felix.ipojo.dependency.interceptors.TransformedServiceReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.*;

/**
 * Implements transformed service reference.
 */
public class TransformedServiceReferenceImpl<S> implements TransformedServiceReference<S> {

    private final ServiceReference<S> m_origin;
    private Map<String, Object> m_properties = new HashMap<String, Object>();

    public TransformedServiceReferenceImpl(ServiceReference<S> origin) {
        this.m_origin = origin;
        // Copy properties
        for (String key : origin.getPropertyKeys()) {
            m_properties.put(key, origin.getProperty(key));
        }
    }

    public TransformedServiceReferenceImpl<S> addProperty(String name, Object value) {
        if (FORBIDDEN_KEYS.contains(name)) {
            throw new IllegalArgumentException("Cannot change the property " + name);
        }
        m_properties.put(name, value);
        return this;
    }

    public TransformedServiceReference<S> addPropertyIfAbsent(String name, Object value) {
        if (! contains(name)) {
            addProperty(name, value);
        }
        return this;
    }

    public Object get(String name) {
        return m_properties.get(name);
    }

    public TransformedServiceReferenceImpl<S> removeProperty(String name) {
        if (FORBIDDEN_KEYS.contains(name)) {
            throw new IllegalArgumentException("Cannot change the property " + name);
        }
        // Store a null value.
        m_properties.put(name, null);
        return this;
    }

    public boolean contains(String name) {
        return m_properties.get(name) != null;
    }

    public ServiceReference<S> getWrappedReference() {
        if (m_origin instanceof TransformedServiceReferenceImpl) {
            return ((TransformedServiceReferenceImpl<S>) m_origin).getWrappedReference();
        } else {
            return m_origin;
        }
    }

    public Object getProperty(String key) {
        return m_properties.get(key);
    }

    public String[] getPropertyKeys() {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : m_properties.entrySet()) {
            if (entry.getValue() != null) {
                keys.add(entry.getKey());
            }
        }
        return keys.toArray(new String[keys.size()]);
    }

    public Bundle getBundle() {
        return m_origin.getBundle();
    }

    public Bundle[] getUsingBundles() {
        return m_origin.getUsingBundles();
    }

    public boolean isAssignableTo(Bundle bundle, String className) {
        return m_origin.isAssignableTo(bundle, className);
    }

    /**
     * Compares two service references.
     * This method is not delegated as we may have modified some of the properties using for the ranking.
     * @param reference the reference
     * @return 0, 1 or -1 depending of the reference.
     */
    public int compareTo(Object reference) {
        ServiceReference other = (ServiceReference) reference;

        Long id = (Long) getProperty(Constants.SERVICE_ID);
        Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

        if (id.equals(otherId)) {
            return 0; // same service
        }

        Object rankObj = getProperty(Constants.SERVICE_RANKING);
        Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

        // If no rank, then spec says it defaults to zero.
        rankObj = (rankObj == null) ? new Integer(0) : rankObj;
        otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

        // If rank is not Integer, then spec says it defaults to zero.
        Integer rank = (rankObj instanceof Integer)
                ? (Integer) rankObj : new Integer(0);
        Integer otherRank = (otherRankObj instanceof Integer)
                ? (Integer) otherRankObj : new Integer(0);

        // Sort by rank in ascending order.
        if (rank.compareTo(otherRank) < 0) {
            return -1; // lower rank
        } else if (rank.compareTo(otherRank) > 0) {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by service id in descending order.
        return (id.compareTo(otherId) < 0) ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ServiceReference) {
            Object id1 = ((ServiceReference) o).getProperty(Constants.SERVICE_ID);
            Object id2 = this.getProperty(Constants.SERVICE_ID);
            return (id1 != null && id1.equals(id2)) || (id1 == id2);
        }
        return m_origin.equals(o);
    }

    @Override
    public int hashCode() {
        return m_origin.hashCode();
    }

    @Override
    public String toString() {
        return getWrappedReference().toString() + m_properties;
    }
}
