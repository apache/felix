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

package org.apache.felix.ipojo.manipulator.store;

/**
 * A {@code ResourceMapper} maps resource name from a reference to another one.
 * Example: In a WAB, class names are to be mapped into {@literal WEB-INF/classes/}.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ResourceMapper {

    /**
     * Adapts the normalized resource name into internal format.
     * @param name original class names (as a resource)
     * @return the transformed resource's name
     */
    String internalize(String name);

    /**
     * Provides a normalized resource name from the store's internal format.
     * @param name resource name in internal format
     * @return normalized resource name
     */
    String externalize(String name);
}
