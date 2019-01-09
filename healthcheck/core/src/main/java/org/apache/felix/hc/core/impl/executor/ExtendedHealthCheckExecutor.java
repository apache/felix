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
package org.apache.felix.hc.core.impl.executor;

import java.util.List;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.framework.ServiceReference;

/** Internal service used by the JMX and ServiceUnavailableFilter */
public interface ExtendedHealthCheckExecutor extends HealthCheckExecutor {

    /** execute single health check using cache, used by JMX */
    HealthCheckExecutionResult execute(ServiceReference<HealthCheck> ref);

    /** internal interface to retrieve service references */
    ServiceReference<HealthCheck>[] selectHealthCheckReferences(HealthCheckSelector selector, HealthCheckExecutionOptions options);
    
    /** internal interface to execute checks for service references */
    List<HealthCheckExecutionResult> execute(final ServiceReference<HealthCheck>[] healthCheckReferences, HealthCheckExecutionOptions options);

}