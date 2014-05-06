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
package org.apache.felix.ipojo.online.manipulator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A classloader trying to load classes from a given jar files and then from bundles.
 * This classloader must only be used for the iPOJO manipulator (in order to compute bytecode frames).
 */
public class BridgeClassLoader extends ClassLoader {

    private final URLClassLoader m_loader;
    private final BundleContext m_context;

    public BridgeClassLoader(File original, BundleContext context) throws MalformedURLException {
        m_loader = new URLClassLoader(new URL[]{original.toURI().toURL()}, null);
        m_context = context;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // Try to load it using the url classloader
        try {
            return m_loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            // Not there, try somewhere else.
        }

        for (Bundle bundle : m_context.getBundles()) {
            if (bundle.getState() >= Bundle.RESOLVED) {
                try {
                    return bundle.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // Try next one.
                }
            }
        }

        // Still nothing, delegate to parent
        return super.loadClass(name);
    }
}
