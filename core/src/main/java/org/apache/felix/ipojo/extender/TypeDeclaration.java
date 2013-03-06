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

package org.apache.felix.ipojo.extender;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Service exposed to instruct a factory creation.
 */
public interface TypeDeclaration extends Declaration {

    /**
     * Get the component metadata description.
     *
     * @return the component metadata description.
     */
    Element getComponentMetadata();

    /**
     * Returns {@literal true} if the type is public
     *
     * @return {@literal true} if the type is public
     */
    boolean isPublic();

    /**
     * Gets the component type's name.
     *
     * @return the component type's name.
     */
    String getComponentName();

    /**
     * Gets the component type's version.
     *
     * @return the component type's version
     */
    String getComponentVersion();

    /**
     * Gets the targeted iPOJO Extension (primitive, composite, handler...)
     *
     * @return the targeted extension
     */
    String getExtension();
}
