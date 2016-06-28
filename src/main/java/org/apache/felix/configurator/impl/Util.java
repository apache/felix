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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Requirement;

public class Util {

    private static final String PROP_ENVIRONMENTS = "configurator.environment";

    public static final String NS_OSGI_IMPL = "osgi.implementation";

    public static final String PROP_CONFIGURATIONS = "configurations";

    private static final String DEFAULT_PATH = "OSGI-INF/configurator";

    /**
     * Check if the bundle contains configurations for the configurer
     * @param bundle The bundle
     * @return Set of locations or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Set<String> isConfigurerBundle(final Bundle bundle) {
        final BundleRevision bundleRevision = bundle.adapt(BundleRevision.class);
        if ( bundleRevision == null ) {
            return null;
        }

        final List<Requirement> requirements = bundleRevision.getRequirements(NS_OSGI_IMPL);
        if ( requirements == null || requirements.isEmpty() ) {
            return null;
        }
        // TODO check version etc
        for(final Requirement req : requirements) {
            final Map<String, Object> attributes = req.getAttributes();

            final Object val = attributes.get(PROP_CONFIGURATIONS);
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
}
