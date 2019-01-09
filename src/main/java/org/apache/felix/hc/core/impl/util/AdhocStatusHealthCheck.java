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
package org.apache.felix.hc.core.impl.util;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.osgi.framework.ServiceRegistration;

public class AdhocStatusHealthCheck implements HealthCheck {

    private Result result;
    
    private ServiceRegistration<HealthCheck> serviceRegistration;

    public AdhocStatusHealthCheck(Result.Status status, String msg) {
        result = new Result(status, msg);
    }

    @Override
    public Result execute() {
        return result;
    }

    public void updateMessage(String msg) {
        this.result = new Result(result.getStatus(), msg);
    }
    
    public void updateResult(Result result) {
        this.result = result;
    }

    public ServiceRegistration<HealthCheck> getServiceRegistration() {
        return serviceRegistration;
    }

    public void setServiceRegistration(ServiceRegistration<HealthCheck> serviceRegistration) {
        this.serviceRegistration = serviceRegistration;
    }

}