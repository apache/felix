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
package org.apache.felix.rootcause.util;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundle;

/**
 * This must be in its own bundle and static to avoid that TinyBundles has to be deployed in OSGi
 */
public class BndDSOptions {

    private BndDSOptions() {
    }

    /**
     * Create a bundle with DS support and automatically generated exports and imports
     */
    public static Option dsBundle(String symbolicName, TinyBundle bundleDef) {
        return streamBundle(bundleDef
                .symbolicName(symbolicName)
                .build(withBnd()));
    }
}
