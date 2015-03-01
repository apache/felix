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
package org.apache.felix.dm.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * OSGi service utilities.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceUtil {
    /**
     * Useful when needing to provide empty service properties.
     */
    public final static Dictionary<String, Object> EMPTY_PROPERTIES = new Hashtable<>();

    /**
     * Returns the service ranking of a service, based on its service reference. If
     * the service has a property specifying its ranking, that will be returned. If
     * not, the default ranking of zero will be returned.
     * 
     * @param ref the service reference to determine the ranking for
     * @return the ranking
     */
    public static int getRanking(ServiceReference ref) {
        return getRankingAsInteger(ref).intValue();
    }
    
    /**
     * Returns the service ranking of a service, based on its service reference. If
     * the service has a property specifying its ranking, that will be returned. If
     * not, the default ranking of zero will be returned.
     * 
     * @param ref the service reference to determine the ranking for
     * @return the ranking
     */
    public static Integer getRankingAsInteger(ServiceReference ref) {
        Integer rank = (Integer) ref.getProperty(Constants.SERVICE_RANKING);
        if (rank != null) {
            return rank;
        }
        return new Integer(0);
    }
    
    /**
     * Returns the service ID of a service, based on its service reference. This
     * method is aware of service aspects as defined by the dependency manager and
     * will return the ID of the orginal service if you give it an aspect.
     * 
     * @param ref the service reference to determine the service ID of
     * @return the service ID
     */
    public static long getServiceId(ServiceReference ref) {
        return getServiceIdAsLong(ref).longValue();
    }
    
    /**
     * Returns the service ID of a service, based on its service reference. This
     * method is aware of service aspects as defined by the dependency manager and
     * will return the ID of the orginal service if you give it an aspect.
     * 
     * @param ref the service reference to determine the service ID of
     * @return the service ID
     */
    public static Long getServiceIdAsLong(ServiceReference ref) {
    	return getServiceIdObject(ref);
    }
    
    public static Long getServiceIdObject(ServiceReference ref) {
        Long aid = (Long) ref.getProperty(DependencyManager.ASPECT);
        if (aid != null) {
            return aid;
        }
        Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
        if (sid != null) {
            return sid;
        }
        throw new IllegalArgumentException("Invalid service reference, no service ID found");
    }

    /**
     * Determines if the service is an aspect as defined by the dependency manager.
     * Aspects are defined by a property and this method will check for its presence.
     * 
     * @param ref the service reference
     * @return <code>true</code> if it's an aspect, <code>false</code> otherwise
     */
    public static boolean isAspect(ServiceReference ref) {
        Long aid = (Long) ref.getProperty(DependencyManager.ASPECT);
        return (aid != null);
    }
    
    /**
     * Converts a service reference to a string, listing both the bundle it was
     * registered from and all properties.
     * 
     * @param ref the service reference
     * @return a string representation of the service
     */
    public static String toString(ServiceReference ref) {
        if (ref == null) {
            return "ServiceReference[null]";
        }
        else {
            StringBuffer buf = new StringBuffer();
            Bundle bundle = ref.getBundle();
            if (bundle != null) {
                buf.append("ServiceReference[");
                buf.append(bundle.getBundleId());
                buf.append("]{");
            }
            else {
                buf.append("ServiceReference[unregistered]{");
            }
            buf.append(propertiesToString(ref, null));
            buf.append("}");
            return buf.toString();
        }
    }
    
    /**
     * Converts the properties of a service reference to a string.
     * 
     * @param ref the service reference
     * @param exclude a list of properties to exclude, or <code>null</code> to show everything
     * @return a string representation of the service properties
     */
    public static String propertiesToString(ServiceReference ref, List<String> exclude) {
        StringBuffer buf = new StringBuffer();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) { 
                buf.append(','); 
            }
            buf.append(keys[i]);
            buf.append('=');
            Object val = ref.getProperty(keys[i]);
            if (exclude == null || !exclude.contains(val)) {
                if (val instanceof String[]) {
                    String[] valArray = (String[]) val;
                    StringBuffer valBuf = new StringBuffer();
                    valBuf.append('{');
                    for (int j = 0; j < valArray.length; j++) {
                        if (valBuf.length() > 1) {
                            valBuf.append(',');
                        }
                        valBuf.append(valArray[j].toString());
                    }
                    valBuf.append('}');
                    buf.append(valBuf);
                }
                else {
                    buf.append(val.toString());
                }
            }
        }
        return buf.toString();
    }
    
    /**
     * Wraps ServiceReference properties behind a Dictionary object.
     * @param ref the ServiceReference to wrap
     * @return a new Dictionary used to wrap the ServiceReference properties
     */
    public static Dictionary<String, Object> propertiesToDictionary(final ServiceReference ref) {
        return new Dictionary<String, Object>() {
            private Dictionary<String, Object> m_wrapper;
            
            @Override
            public int size() {
                return getWrapper().size();
            }

            @Override
            public boolean isEmpty() {
                return getWrapper().isEmpty();
            }

            @Override
            public Enumeration<String> keys() {
                return getWrapper().keys();
            }

            @Override
            public Enumeration<Object> elements() {
                return getWrapper().elements();
            }

            @Override
            public Object get(Object key) {
                return ref.getProperty(key.toString());
            }

            @Override
            public Object put(String key, Object value) {
                throw new UnsupportedOperationException("Unmodified Dictionary.");
            }

            @Override
            public Object remove(Object key) {
                throw new UnsupportedOperationException("Unmodified Dictionary.");
            }
            
            @Override
            public String toString() {
                return getWrapper().toString();
            }
            
            private synchronized Dictionary<String, Object> getWrapper() {
                if (m_wrapper == null) {
                    m_wrapper = new Hashtable<String, Object>();
                    String[] keys = ref.getPropertyKeys();
                    for (String key : keys) {
                        m_wrapper.put(key, ref.getProperty(key));
                    }                    
                }
                return m_wrapper;
            }
        };
    }
}
