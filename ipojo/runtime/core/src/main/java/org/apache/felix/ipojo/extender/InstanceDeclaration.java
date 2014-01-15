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

import org.osgi.framework.Bundle;

import java.util.Dictionary;

/**
 * Service published to instruct an instance creation.
 */
public interface InstanceDeclaration extends Declaration {
    /**
     * Service property specifying the component type's name.
     */
    String COMPONENT_NAME_PROPERTY = "ipojo.component.name";

    /**
     * Service property specifying the component type's version.
     */
    String COMPONENT_VERSION_PROPERTY = "ipojo.component.version";

    /**
     * Service property specifying the instance name.
     */
    String INSTANCE_NAME = "ipojo.instance.name";

    /**
     * Value used when an instance configuration does not declare its name.
     */
    String UNNAMED_INSTANCE = "unnamed";

    /**
     * The instance configuration.
     *
     * @return the instance configuration
     */
    Dictionary<String, Object> getConfiguration();

    /**
     * @return the component type's name.
     */
    String getComponentName();

    /**
     * @return the component type's version, {@literal null} if not set.
     */
    String getComponentVersion();

    /**
     * Gets the instance name.
     *
     * @return the instance name, {@literal unnamed} if not specified.
     */
    String getInstanceName();

    /**
     * Gets the bundle that is declaring this instance.
     * @return the bundle object
     * @since 1.11.2
     */
    Bundle getBundle();
}
