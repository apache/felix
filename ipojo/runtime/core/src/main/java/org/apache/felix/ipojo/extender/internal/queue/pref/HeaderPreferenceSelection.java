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

package org.apache.felix.ipojo.extender.internal.queue.pref;

import org.osgi.framework.Bundle;

/**
 * A preference selection strategy based on a manifest header.
 * By default, the {@literal IPOJO-Queue-Preference} header is used.
 */
public class HeaderPreferenceSelection implements PreferenceSelection {

    private final String name;

    public HeaderPreferenceSelection() {
        this("IPOJO-Queue-Preference");
    }

    public HeaderPreferenceSelection(String name) {
        this.name = name;
    }

    public Preference select(Bundle source) {
        String header = source.getHeaders().get(name);

        // No preference specified, return default
        if (header == null) {
            return Preference.DEFAULT;
        }

        header = header.trim();
        try {
            return Preference.valueOf(header.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Preference.DEFAULT;
        }
    }
}
