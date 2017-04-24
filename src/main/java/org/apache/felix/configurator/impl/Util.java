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
package org.apache.felix.configurator.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class Util {

    private static final String PROP_ENVIRONMENTS = "configurator.environment";

    public static final String NS_OSGI_IMPL = "osgi.implementation";

    public static final String PROP_CONFIGURATIONS = "configurations";

    private static final String DEFAULT_PATH = "OSGI-INF/configurator";

    public static volatile File binDirectory;

    /**
     * Check if the bundle contains configurations for the configurator
     * @param bundle The bundle
     * @param configuratorBundleId The bundle id of the configurator bundle to check the wiring
     * @return Set of locations or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Set<String> isConfigurerBundle(final Bundle bundle, final long configuratorBundleId) {
        // check for bundle wiring
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if ( bundleWiring == null ) {
            return null;
        }

        // check for bundle requirement to implementation namespace
        final List<BundleRequirement> requirements = bundleWiring.getRequirements(NS_OSGI_IMPL);
        if ( requirements == null || requirements.isEmpty() ) {
            return null;
        }
        // get all wires for the implementation namespace
        final List<BundleWire> wires = bundleWiring.getRequiredWires(NS_OSGI_IMPL);
        for(final BundleWire wire : wires) {
            // if the wire is to this bundle (configurator), it must be the correct
            // requirement (no need to do additional checks like version etc.)
            if ( wire.getProviderWiring() != null
                 && wire.getProviderWiring().getBundle().getBundleId() == configuratorBundleId ) {
                final Object val = wire.getRequirement().getAttributes().get(PROP_CONFIGURATIONS);
                if ( val != null ) {
                    if ( val instanceof String ) {
                        return Collections.singleton((String)val);
                    }
                    if ( val instanceof List ) {
                        final List<String> paths = (List<String>)val;
                        final Set<String> result = new HashSet<>();
                        for(final String p : paths) {
                            result.add(p);
                        }
                        return result;
                    }
                    SystemLogger.error("Attribute " + PROP_CONFIGURATIONS + " for configurator requirement has an invalid type: " + val +
                                       ". Using default configuration.");
                }
                return Collections.singleton(DEFAULT_PATH);
            }
        }

        return null;
    }

    /**
     * Get the set of active environments from the framework property.
     *
     * @param bc The bundle context
     * @return A set with the environments, might be empty
     */
    public static Set<String> getActiveEnvironments(final BundleContext bc) {
        final String value = bc.getProperty(PROP_ENVIRONMENTS);
        if ( value == null ) {
            return Collections.emptySet();
        }
        final Set<String> envs = new HashSet<>();
        for(final String name : value.split(",") ) {
            if ( isValidEnvironmentName(name) ) {
                envs.add(name.trim());
            } else {
                SystemLogger.error("Invalid environment name: " + name);
            }
        }
        return envs;
    }

    public static boolean isValidEnvironmentName(final String name) {
        if ( name == null ) {
            return false;
        }
        final String testName = name.trim();
        boolean isValid = !testName.isEmpty();
        for(int i=0; i<testName.length(); i++) {
            final char c = testName.charAt(i);
            if ( c == '-'
                 || c == '_'
                 || (c >= '0' && c <= '9')
                 || (c >= 'a' && c <= 'z')
                 || (c >= 'A' && c <= 'Z')) {
                continue;
            }
            isValid = false;
            break;
        }
        return isValid;
    }

    /**
     * Set a (final) field during deserialization.
     */
    public static void setField(final Object obj, final String name, final Object value)
    throws IOException {
        Class<?> clazz = obj.getClass();
        while ( clazz != null ) {
            try {
                final Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (final NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (final SecurityException | IllegalArgumentException  | IllegalAccessException e) {
                throw (IOException)new IOException().initCause(e);
            }
        }
        throw new IOException("Field " + name + " not found in class " + obj.getClass());
    }

    public static String getSHA256(final String value) {
        try {
            StringBuilder builder = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (byte b : md.digest(value.getBytes("UTF-8")) ) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch ( final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the contents of a resource, encoded as UTF-8
     * @param name The resource name
     * @param url The resource URL
     * @return The contents or {@code null}
     */
    public static String getResource(final String name, final URL url) {
        URLConnection connection = null;
        try {
            connection = url.openConnection();

            try(final BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(), "UTF-8"))) {

                final StringBuilder sb = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }

                return sb.toString();
            }
        } catch ( final IOException ioe ) {
            SystemLogger.error("Unable to read " + name, ioe);
        }
        return null;
    }

    public static File extractFile(final Bundle bundle, final String pid, final String path) {
        final URL url = bundle.getEntry(path);
        if ( url == null ) {
            SystemLogger.error("Entry " + path + " not found in bundle " + bundle);
            return null;
        }
        URLConnection connection = null;
        try {
            connection = url.openConnection();

            final File dir = new File(binDirectory, URLEncoder.encode(pid, "UTF-8"));
            dir.mkdir();
            final File newFile = new File(dir, UUID.randomUUID().toString());

            try(final BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                final FileOutputStream fos = new FileOutputStream(newFile)) {

                int len = 0;
                final byte[] buffer = new byte[16384];

                while ( (len = in.read(buffer)) > 0 ) {
                    fos.write(buffer, 0, len);
                }
            }

            return newFile;
        } catch ( final IOException ioe ) {
            SystemLogger.error("Unable to read " + path +
                    " in bundle " + bundle +
                    " for pid " + pid +
                    " and write to " + binDirectory, ioe);
        }

        return null;
    }
}
