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
package org.apache.felix.hc.core.impl.servlet;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Apache Felix Health Check Verbose Text Serializer", description = "Serializes health check results to a verbose text format")
@interface ResultTxtVerboseSerializerConfiguration {

    @AttributeDefinition(name = "Total Width", description = "Total width of all columns in verbose txt rendering (in characters)")
    int totalWidth() default 140;

    @AttributeDefinition(name = "Name Column Width", description = "Column width of health check name (in characters)")
    int colWidthName() default 30;

    @AttributeDefinition(name = "Result Column Width", description = "Column width of health check result (in characters)")
    int colWidthResult() default 9;

    @AttributeDefinition(name = "Timing Column Width", description = "Column width of health check timing (in characters)")
    int colWidthTiming() default 22;

}
