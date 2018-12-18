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

@ObjectClassDefinition(name = "Apache Felix Health Check Executor Servlet", description = "Serializes health check results into html, json or txt format")
@interface HealthCheckExecutorServletConfiguration {

    String SERVLET_PATH_DEFAULT = "/system/health";

    @AttributeDefinition(name = "Disabled", description = "Allows to disable the servlet if required for security reasons")
    boolean disabled() default false;

    @AttributeDefinition(name = "Path", description = "Servlet path (defaults to " + SERVLET_PATH_DEFAULT
            + " in order to not be accessible via Apache/Internet)")
    String servletPath() default SERVLET_PATH_DEFAULT;

    @AttributeDefinition(name = "CORS Access-Control-Allow-Origin", description = "Sets the Access-Control-Allow-Origin CORS header. If blank no header is sent.")
    String cors_accessControlAllowOrigin() default "*";

}
