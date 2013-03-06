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

import org.apache.felix.ipojo.extender.builder.FactoryBuilder;

/**
 * iPOJO's extension declaration.
 * This service interface is published to instruct the extender to create a new iPOJO extension (like composite or
 * handler).
 */
public interface ExtensionDeclaration extends Declaration {
    /**
     * The service property specifying the extension name.
     */
    String EXTENSION_NAME_PROPERTY = "ipojo.extension.name";

    /**
     * Gets the factory builder to use to create the factories bound to this extension.
     *
     * @return the factory builder.
     */
    FactoryBuilder getFactoryBuilder();

    /**
     * Gets the extension name. This name must be unique.
     *
     * @return the extension name.
     */
    String getExtensionName();
}
