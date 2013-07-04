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

package org.apache.felix.ipojo.bnd;

import java.util.jar.Attributes;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

/**
 * A {@code Manifests} is a utility class for extracting data from Bundle's Manifest.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Manifests {
    private Manifests() {}

    public static String getComponents(Attributes main) {
        String value = main.getValue("iPOJO-Components");
        if (value == null) {
            value = main.getValue("IPOJO-Components");
        }

        return value;
    }

    public static boolean hasEmbedComponents(Analyzer analyzer) throws Exception {
        for (Jar jar : analyzer.getClasspath()) {
            // Check if the bundle was manipulated before
            Attributes attr = jar.getManifest().getMainAttributes();
            if (getComponents(attr) != null) {
                return true;
            }
        }
        return false;
    }


}
