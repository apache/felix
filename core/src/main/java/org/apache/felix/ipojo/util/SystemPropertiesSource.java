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

package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ContextListener;
import org.apache.felix.ipojo.ContextSource;

import java.util.Dictionary;

/**
 * A context source giving access to system properties.
 */
public class SystemPropertiesSource implements ContextSource {

    public Object getProperty(String property) {
        return System.getProperty(property);
    }

    public Dictionary getContext() {
        return System.getProperties();
    }

    public void registerContextListener(ContextListener listener, String[] properties) {
        // Ignored, as this source is not dynamic, so won't send notifications
    }

    public void unregisterContextListener(ContextListener listener) {
        // Ignored, as this source is not dynamic, so won't send notifications
    }
}
