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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception { 
        createDeviceAndParameter(dm, 1);
        createDeviceAndParameter(dm, 2);

        dm.add(createAdapterService(Device.class, null)
            .setImplementation(DeviceAccessImpl.class)
            .setInterface(DeviceAccess.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(DeviceAccessConsumer.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(createServiceDependency().setService(DeviceAccess.class).setRequired(true).setCallbacks("add", null)));
    }
    
    private void createDeviceAndParameter(DependencyManager dm, int id) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("device.id", id);
        dm.add(createComponent()
          .setImplementation(new DeviceImpl(id)).setInterface(Device.class.getName(), props));
           
        props = new Hashtable<>();
        props.put("device.id", id);
        dm.add(createComponent()
          .setImplementation(new DeviceParameterImpl(id)).setInterface(DeviceParameter.class.getName(), props));        
    }
}
