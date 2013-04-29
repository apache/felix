/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

/**
 * This class defines the classloader attached to a factory.
 * This class loader is used to load the implementation (e.g. manipulated)
 * class.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @see ClassLoader
 */
class FactoryClassloader extends ClassLoader {

    /**
     * The map of defined classes [Name, Class Object].
     */
    private final Map<String, Class<?>> m_definedClasses = new HashMap<String, Class<?>>();
    private ComponentFactory factory;

    public FactoryClassloader(ComponentFactory factory) {
        this.factory = factory;
    }

    /**
     * The defineClass method.
     *
     * @param name   name of the class
     * @param clazz  the byte array of the class
     * @param domain the protection domain
     * @return the defined class.
     */
    public Class<?> defineClass(String name, byte[] clazz, ProtectionDomain domain) {
        if (m_definedClasses.containsKey(name)) {
            return m_definedClasses.get(name);
        }
        Class clas = super.defineClass(name, clazz, 0, clazz.length, domain);
        m_definedClasses.put(name, clas);
        return clas;
    }

    /**
     * Returns the URL of the required resource.
     *
     * @param arg the name of the resource to find.
     * @return the URL of the resource.
     * @see ClassLoader#getResource(String)
     */
    public URL getResource(String arg) {
        return factory.m_context.getBundle().getResource(arg);
    }

    /**
     * Loads the given class.
     *
     * @param name    the name of the class
     * @param resolve should be the class resolve now ?
     * @return the loaded class object
     * @throws ClassNotFoundException if the class to load is not found
     * @see ClassLoader#loadClass(String, boolean)
     * @see ClassLoader#loadClass(String, boolean)
     */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return factory.m_context.getBundle().loadClass(name);
    }
}
