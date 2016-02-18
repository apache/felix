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
package org.apache.felix.dm.lambda.samples.factory;

import org.osgi.service.log.LogService;

/**
 * This is the main implementation for our "Provider" service.
 * This service is using a composition of two participants, which are used to provide the service
 * (ProviderParticipant1, and ProviderParticipant2).
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProviderImpl implements Provider {
    private volatile LogService m_log;
            
    void set(LogService log) { m_log = log; }

    void start() {
        m_log.log(LogService.LOG_WARNING, "ProviderImpl.start()");
    }
}
