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
package org.apache.felix.scrplugin.description;


public enum SpecVersion {

    VERSION_1_0("1.0"),              // R4.1
    VERSION_1_1("1.1"),              // R4.2
    VERSION_1_1_FELIX("1.1_FELIX");  // R4.2 + FELIX-1893

    /**
     * internal human readable name
     */
    private final String name;

    /**
     * Create a type
     *
     * @param name name
     */
    private SpecVersion(final String name) {
        this.name = name;
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
     * Creates a version for the given name. if the name cannot be mapped
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
}
