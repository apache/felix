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

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.dependency.interceptors.ServiceTrackingInterceptor;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.Log;
import org.osgi.framework.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Builds the properties used to checks if an interceptor matches a specific dependency.
 */
public class DependencyProperties {

    //TODO Externalize and use constants
    // TODO Cache dependency properties.

    public static  Dictionary<String, ?> getDependencyProperties(DependencyModel dependency) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        // Instance, and Factory and Bundle (name, symbolic name, version)
        properties.put(Factory.INSTANCE_NAME_PROPERTY, dependency.getComponentInstance().getInstanceName());
        properties.put("instance.state", dependency.getComponentInstance().getState());
        properties.put("factory.name", dependency.getComponentInstance().getFactory().getFactoryName());
        final Bundle bundle = dependency.getBundleContext().getBundle();
        properties.put("bundle.symbolicName", bundle.getSymbolicName());
        if (bundle.getVersion() != null) {
            properties.put("bundle.version", bundle.getVersion().toString());
        }

        // Dependency specification, and id
        final Class specification = dependency.getSpecification();
        properties.put("dependency.specification", specification.getName());
        properties.put("dependency.id", dependency.getId());
        properties.put("dependency.state", dependency.getState());

        // We also provide the objectclass property, and to be compliant with the osgi specification,
        // all interfaces are collected to this array.
        List<String> classes = new ArrayList<String>();
        classes.add(specification.getName());
        for (Class clazz : specification.getInterfaces()) {
            classes.add(clazz.getName());
        }
        properties.put(Constants.OBJECTCLASS, classes.toArray(new String[classes.size()]));

        return properties;
    }


    /**
     * Checks that the 'target' property of the service reference matches the dependency.
     * @param reference the reference
     * @param dependency the dependency
     * @param context a bundle context used to build the filter
     * @return {@literal true} if the target's property of reference matches the dependency.
     */
    public static boolean match(ServiceReference reference, DependencyModel dependency, BundleContext context) {
        Object v = reference.getProperty(ServiceTrackingInterceptor.TARGET_PROPERTY);
        Filter filter = null;
        if (v == null) {
            return false; // Invalid interceptor
        }
        if (v instanceof Filter) {
            filter = (Filter) v;
        } else if (v instanceof String) {
            try {
                filter = context.createFilter((String) v);
            } catch (InvalidSyntaxException e) {
                dependency.getComponentInstance().getFactory().getLogger().log(Log.ERROR,
                        "Cannot build filter from the target property : " + v, e);
            }
        }

        if (filter == null) {
            return false; // Invalid interceptor.
        }

        Dictionary<String, ?> properties = getDependencyProperties(dependency);

        return filter.match(properties);
    }

    public static boolean match(ServiceReference reference, DependencyModel dependency) {
        return match(reference, dependency, dependency.getBundleContext());
    }
}
