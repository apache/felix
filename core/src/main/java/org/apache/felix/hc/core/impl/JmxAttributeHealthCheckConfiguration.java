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

@ObjectClassDefinition(name = "Apache Felix JMX Attribute Health Check", description = "Checks the value of a single JMX attribute.")
@interface JmxAttributeHealthCheckConfiguration {

    @AttributeDefinition(name = "Name", description = "Name of this health check.")
    String hc_name() default "";

    @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
    String[] hc_tags() default {};

    @AttributeDefinition(name = "MBean Name", description = "Name of the MBean to create for this health check. If empty, no MBean is registered.")
    String hc_mbean_name() default "";

    //

    @AttributeDefinition(name = "Check MBean Name", description = "The name of the MBean to check by this health check.")
    String mbean_name() default "";

    @AttributeDefinition(name = "Check Attribute Name", description = "The name of the MBean attribute to check by this health check.")
    String attribute_name() default "";

    @AttributeDefinition(name = "Check Attribute Constraint", description = "Constraint on the MBean attribute value.")
    String attribute_value_constraint() default "";

}
