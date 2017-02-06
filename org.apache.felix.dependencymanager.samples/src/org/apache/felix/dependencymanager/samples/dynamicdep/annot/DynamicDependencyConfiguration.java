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
package org.apache.felix.dependencymanager.samples.dynamicdep.annot;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.*;

/**
 * This interface describes the configuration for our DynamicDependencyComponent component. We are using the metatype
 * annotations, allowing to configure our component  from web console.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@ObjectClassDefinition(
		name = "Dynamic Dependency Configuration",
		description = "Declare here the configuration for the DynamicDependency component.",
		pid="org.apache.felix.dependencymanager.samples.dynamicdep.annot.DynamicDependencyConfiguration")
public interface DynamicDependencyConfiguration {
    
    @AttributeDefinition(description = "Enter the storage type to use", 
        defaultValue = "mapdb",
        options={
        	@Option(label="Map DB Storage implementation", value="mapdb"),
        	@Option(label="File Storage implementation", value="file")
        })
    String storageType();

    @AttributeDefinition(
    	description = "Specifies here is the storage dependency is required or not (if false, a null object will be used)", 
    	defaultValue = "true")
    boolean storageRequired();

}
