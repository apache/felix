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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class Util {

    public static final String NS_OSGI_EXTENDER = "osgi.extender";

    private static final String PROP_CONFIGURATIONS = "configurations";

    private static final String DEFAULT_PATH = "OSGI-INF/configurator";

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
        final List<BundleRequirement> requirements = bundleWiring.getRequirements(NS_OSGI_EXTENDER);
        if ( requirements == null || requirements.isEmpty() ) {
            return null;
        }
        // get all wires for the implementation namespace
        final List<BundleWire> wires = bundleWiring.getRequiredWires(NS_OSGI_EXTENDER);
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
