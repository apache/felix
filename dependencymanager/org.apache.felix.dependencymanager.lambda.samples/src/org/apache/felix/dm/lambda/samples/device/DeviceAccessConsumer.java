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
package org.apache.felix.dm.lambda.samples.device;

import java.util.Map;

import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DeviceAccessConsumer {
    volatile LogService log;

    void add(DeviceAccess deviceAccess, Map<String, Object> props) {
        log.log(LogService.LOG_WARNING, "DeviceAccessConsumer: Handling device access: id=" + props.get("device.access.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
