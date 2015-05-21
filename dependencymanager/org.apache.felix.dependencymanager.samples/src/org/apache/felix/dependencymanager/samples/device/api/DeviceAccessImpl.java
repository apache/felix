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
package org.apache.felix.dependencymanager.samples.device.api;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DeviceAccessImpl implements DeviceAccess {
    volatile Device device;
    volatile DeviceParameter deviceParameter;

    void init(Component component) {
        // Dynamically add an extra dependency on a DeviceParameter.
    	// We also add a "device.access.id" property dynamically.
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("device.access.id", device.getDeviceId());

        DependencyManager dm = component.getDependencyManager();
        component
        	.setServiceProperties(props)
        	.add(dm.createServiceDependency().setService(DeviceParameter.class, "(device.id=" + device.getDeviceId() + ")").setRequired(true));
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
