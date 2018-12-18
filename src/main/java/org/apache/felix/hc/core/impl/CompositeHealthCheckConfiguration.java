/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Felix Composite Health Check", description = "Executes a set of health checks, selected by tags.")
@interface CompositeHealthCheckConfiguration {

    @AttributeDefinition(name = "Name", description = "Name of this health check.")
    String hc_name() default "";

    @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
    String[] hc_tags() default {};

    @AttributeDefinition(name = "MBean Name", description = "Name of the MBean to create for this health check. If empty, no MBean is registered.")
    String hc_mbean_name() default "";

    //

    @AttributeDefinition(name = "Filter Tags", description = "Tags used to select which health checks the composite health check executes.")
    String[] filter_tags() default {};

    @AttributeDefinition(name = "Combine Tags With Or", description = "Tags used to select which health checks the composite health check executes.")
    boolean filter_combineTagsWithOr() default false;

}
