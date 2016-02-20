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
package org.apache.felix.dependencymanager.samples.hello.annot;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class ServiceConsumer {
    @ServiceDependency
    volatile ServiceProvider service;

    @ServiceDependency
    volatile LogService log;

    ServiceConsumerConf conf;

    @ConfigurationDependency 
    protected void update(ServiceConsumerConf conf) { // type safe config
        this.conf = conf;
    }

    @Start
    public void start() {
        log.log(LogService.LOG_WARNING, "ServiceConsumer.start: configured key=" + conf.getKey());
        log.log(LogService.LOG_WARNING, "ServiceConsumer.start: calling service.hello() ...");
        this.service.hello();
    }
}
