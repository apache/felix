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
package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@AdapterService(adapteeService = Device.class)
public class DeviceAccessImpl implements DeviceAccess {
    volatile Device device;

    @ServiceDependency(name = "deviceparam")
    volatile DeviceParameter deviceParameter;

    @ServiceDependency
    volatile LogService log;

    @Init
    Map<String, String> init() {
        log.log(LogService.LOG_WARNING, "DeviceAccessImpl.init: device id=" + device.getDeviceId());
        // Dynamically configure our "deviceparam" dependency, using the already injected device service.
        Map<String, String> filters = new HashMap<>();
        filters.put("deviceparam.filter", "(device.id=" + device.getDeviceId() + ")");
        filters.put("deviceparam.required", "true");
        return filters;
    }

    @Start
    Map<?, ?> start() {
        log.log(LogService.LOG_WARNING, "DeviceAccessImpl.start");
        // Dynamically add a service property, using the device.id
        Map<String, Object> props = new Hashtable<>();
        props.put("device.access.id", device.getDeviceId());
        return props;
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public DeviceParameter getDeviceParameter() {
        return deviceParameter;
    }
}
