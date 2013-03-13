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

package org.apache.felix.ipojo.manipulator.render;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Defines a filter to be tested against Element before rendering them into the Manifest.
 * If one filter accept the element, the given Element will be filtered, other filters will be ignored.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface MetadataFilter {

    /**
     * Tests if the given {@link Element} is accepted by the filter.
     * @param element the tested element.
     * @return {@literal true} if the filter accept the value (element will
     *         be omitted from the rendered metadata), {@literal false} otherwise.
     */
    public boolean accept(final Element element);
}
