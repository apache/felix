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

import org.apache.felix.ipojo.context.ServiceReferenceImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import java.util.Collection;
import java.util.List;

/**
 * Some utility methods to handle service references.
 */
public class ServiceReferenceUtils {
    /**
     * Checks if the given service reference match the current filter.
     * This method aims to avoid calling {@link org.osgi.framework.Filter#match(org.osgi.framework.ServiceReference)}
     * method when manipulating a composite reference. In fact, this method thrown
     * a {@link ClassCastException} on Equinox.
     *
     * @param ref the service reference to check.
     * @return <code>true</code> if the service reference matches.
     */
    public static boolean match(Filter filter, ServiceReference ref) {
        boolean match = true;
        if (filter != null) {
            if (ref instanceof ServiceReferenceImpl) {
                // Can't use the match(ref) as it throw a class cast exception on Equinox.
                //noinspection unchecked
                match = filter.match(((ServiceReferenceImpl) ref).getProperties());
            } else { // Non composite reference.
                match = filter.match(ref);
            }
        }
        return match;
    }

    /**
     * Checks whether a list of service references contains a reference with the same {@literal service.id} as the
     * given reference.
     * @param references the list of reference
     * @param ref the reference
     * @return {@literal true} if references contains a reference with the same service.id as ref.
     */
    public static boolean containsReferenceById(List<? extends ServiceReference> references, ServiceReference ref) {
        return getServiceReferenceById(references, ref) != null;
    }

    /**
     * Gets a service reference with the same service.id as the given reference from the given list.
     * @param references the list of references
     * @param ref the reference
     * @return the service reference from references having the same service.id as ref. {@literal null} if there is
     * no such reference in the list.
     */
    public static ServiceReference getServiceReferenceById(List<? extends ServiceReference> references,
                                                           ServiceReference ref) {
        Object id = ref.getProperty(Constants.SERVICE_ID);
        for (ServiceReference reference : references) {
            if (reference.getProperty(Constants.SERVICE_ID).equals(id)) {
                return reference;
            }
        }
        return null;
    }

    /**
     * Checks whether the two references has the same properties and their value are equals.
     * @param ref1 first reference
     * @param ref2 second reference
     * @return {@literal true} if the two references have the same properties and their values are equals.
     */
    public static boolean haveSameProperties(ServiceReference ref1, ServiceReference ref2) {
        if (ref2 == null  && ref1 == null) {
            return true;
        }

        if ((ref1 == null) || (ref2 == null)) {
            return false;
        }

        String[] keys = ref2.getPropertyKeys();

        if (ref2.getPropertyKeys().length != keys.length) {
            return false;
        }

        for (String key : keys) {
            if (! ref2.getProperty(key).equals(ref1.getProperty(key))) {
                return false;
            }
        }

        return true;


    }

    /**
     * Checks whether two service references have the same service id.
     * @param ref1 first reference
     * @param ref2 second reference
     * @return {@literal true} if the two references have the same service.id, {@literal false} otherwise.
     */
    public static boolean haveSameServiceId(ServiceReference ref1, ServiceReference ref2) {
        return !(ref1 == null || ref2 == null)
                && ref1.getProperty(Constants.SERVICE_ID).equals(ref2.getProperty(Constants.SERVICE_ID));
    }

    public static String toString(Collection<? extends ServiceReference> references) {
        if (references == null  || references.isEmpty()) {
            return "[]";
        } else {
            StringBuilder buffer = new StringBuilder("[");
            for (ServiceReference reference : references) {
                if (buffer.length() == 1) {
                    buffer.append(reference.getProperty(Constants.SERVICE_ID));
                } else {
                    buffer.append(", ").append(reference.getProperty(Constants.SERVICE_ID));
                }
            }
            buffer.append("]");
            return buffer.toString();
        }
    }
}
