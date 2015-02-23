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
package org.apache.felix.dm.runtime.api;

import java.util.Dictionary;

/**
 * When a Component is annotated with a DM "Component" annotation with a "factoryName" attribute, a corresponding
 * ComponentFactory is registered in the OSGi service registry with a @link {@link ComponentFactory#FACTORY_NAME} 
 * servie property with the Component "factoryName" value.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentFactory {
    /**
     * Instantiates a Component instance. Any properties starts with a "." are considered as private. Other properties will be
     * published as the component instance service properties (if the component provides a services).
     * @param conf the properties passed to the component "configure" method which is specified with the "configure" attribute
     * of the @Component annotation.  
     * @return the component instance.
     */
    ComponentInstance newInstance(Dictionary<String, ?> conf);
}
