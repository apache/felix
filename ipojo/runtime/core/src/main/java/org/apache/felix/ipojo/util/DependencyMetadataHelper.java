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

package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import java.util.Comparator;

/**
 * A set of methods to simplify the parsing of  dependency attributes.
 */
public class DependencyMetadataHelper {


    /**
     * Helper method parsing the comparator attribute and returning the
     * comparator object. If the 'comparator' attribute is not set, this method
     * returns null. If the 'comparator' attribute is set to 'osgi', this method
     * returns the normal OSGi comparator. In other case, it tries to create
     * an instance of the declared comparator class.
     *
     * @param dep     the Element describing the dependency
     * @param context the bundle context (to load the comparator class)
     * @return the comparator object, <code>null</code> if not set.
     * @throws org.apache.felix.ipojo.ConfigurationException the comparator class cannot be load or the
     *                                comparator cannot be instantiated correctly.
     */
    public static Comparator getComparator(Element dep, BundleContext context) throws ConfigurationException {
        Comparator cmp = null;
        String comp = dep.getAttribute("comparator");
        if (comp != null) {
            if (comp.equalsIgnoreCase("osgi")) {
                cmp = new ServiceReferenceRankingComparator();
            } else {
                try {
                    Class cla = context.getBundle().loadClass(comp);
                    cmp = (Comparator) cla.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot load a customized comparator", e);
                } catch (IllegalAccessException e) {
                    throw new ConfigurationException("Cannot create a customized comparator", e);
                } catch (InstantiationException e) {
                    throw new ConfigurationException("Cannot create a customized comparator", e);
                }
            }
        }
        return cmp;
    }

    /**
     * Loads the given specification class.
     *
     * @param specification the specification class name to load
     * @param context       the bundle context
     * @return the class object for the given specification
     * @throws org.apache.felix.ipojo.ConfigurationException if the class cannot be loaded correctly.
     */
    public static Class loadSpecification(String specification, BundleContext context) throws ConfigurationException {
        Class spec;
        try {
            spec = context.getBundle().loadClass(specification);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("A required specification cannot be loaded : " + specification, e);
        }
        return spec;
    }

    /**
     * Helper method parsing the binding policy.
     * If the 'policy' attribute is not set in the dependency, the method returns
     * the 'DYNAMIC BINDING POLICY'. Accepted policy values are : dynamic,
     * dynamic-priority and static.
     *
     * @param dep the Element describing the dependency
     * @return the policy attached to this dependency
     * @throws org.apache.felix.ipojo.ConfigurationException if an unknown binding policy was described.
     */
    public static int getPolicy(Element dep) throws ConfigurationException {
        String policy = dep.getAttribute("policy");
        if (policy == null || policy.equalsIgnoreCase("dynamic")) {
            return DependencyModel.DYNAMIC_BINDING_POLICY;
        } else if (policy.equalsIgnoreCase("dynamic-priority")) {
            return DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY;
        } else if (policy.equalsIgnoreCase("static")) {
            return DependencyModel.STATIC_BINDING_POLICY;
        } else {
            throw new ConfigurationException("Binding policy unknown : " + policy);
        }
    }
}
