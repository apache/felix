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

import static java.lang.System.out;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	out.println("type \"log warn\" to see the logs emitted by this test.");

    	// Create a pair of Device/DeviceParameter service with id=1
        createDeviceAndParameter(1);
        
        // Create a pair of Device/DeviceParameter service with id=2
        createDeviceAndParameter(2);
        
        // Create a DeviceParameter adapter: for each pair of Device/DeviceParameter services having the same id,
        // a DeviceParameter adapter service will be created.
        adapter(Device.class, adpt -> adpt.provides(DeviceAccess.class).impl(DeviceAccessImpl.class));
        
        // Creates a component that simply displays all available DeviceParameter adapter services.
        component(comp -> comp
            .impl(DeviceAccessConsumer.class)
            .withSvc(LogService.class, true)
            .withSvc(DeviceAccess.class, device -> device.required().add(DeviceAccessConsumer::add)));       
    }
    
    private void createDeviceAndParameter(int id) {
        // Creates a Device service with the provided id.
        component(comp -> comp.factory(() -> new DeviceImpl(id)).provides(Device.class, "device.id", id));
        
        // Creates a DeivceParameter with the provided id.
        component(comp -> comp.factory(() -> new DeviceParameterImpl(id)).provides(DeviceParameter.class, "device.id", id));
    }
}
