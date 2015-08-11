/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.felix.scrplugin;

/**
 * An enumeration for all available spec versions.
 *
 * The versions in the enumeration have to be ordered, higher versions must have
 * a higher ordinal.
 */
public enum SpecVersion {

    VERSION_1_0("1.0", "http://www.osgi.org/xmlns/scr/v1.0.0"),                        // R4.1
    VERSION_1_1("1.1", "http://www.osgi.org/xmlns/scr/v1.1.0"),                        // R4.2
    VERSION_1_1_FELIX("1.1_FELIX", "http://felix.apache.org/xmlns/scr/v1.1.0-felix"),  // R4.2 + FELIX-1893
    VERSION_1_2("1.2", "http://www.osgi.org/xmlns/scr/v1.2.0");                        // R4.3

    /**
     * internal human readable name
     */
    private final String name;

    /**
     * Namespace url
     */
    private final String namespaceUrl;

    /**
     * Create a type
     *
     * @param name A human readable name
     * @param namespaceUrl The namespace URL for this spec version
     */
    private SpecVersion(final String name, final String namespaceUrl) {
        this.name = name;
        this.namespaceUrl = namespaceUrl;
    }

    /**
     * Returns the human readable type name of this type.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the namespace url.
     * @return The namespace url.
     */
    public String getNamespaceUrl() {
        return this.namespaceUrl;
    }

    /**
     * Creates a version for the given name. If the name cannot be mapped
     * to a enum type or if it's <code>null</code>, <code>null</code> is
     * returned.
     *
     * @param n the name
     * @return the type or <code>null</code>
     */
    public static SpecVersion fromName(final String n) {
        if (n == null) {
            return null;
        }
        try {
            return SpecVersion.valueOf(n.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        for(final SpecVersion sv : SpecVersion.values() ) {
            if ( sv.getName().equals(n)) {
                return sv;
            }
        }
        return null;
    }

    /**
     * Creates a version for the given url. If the url cannot be mapped
     * to a enum type or if it's <code>null</code>, <code>null</code> is
     * returned.
     *
     * @param n the url
     * @return the type or <code>null</code>
     */
    public static SpecVersion fromNamespaceUrl(final String n) {
        if (n == null) {
            return null;
        }
        for(final SpecVersion sv : SpecVersion.values() ) {
            if ( sv.getNamespaceUrl().equals(n)) {
                return sv;
            }
        }
        return null;
    }
}
