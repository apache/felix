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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.runtime.api.ComponentFactory;
import org.osgi.service.log.LogService;

/**
 * Component used to instantiate Device and DeviceParameter services, using DM component factory annotation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class DeviceAndParameterFactory {
    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=Device)")
    volatile ComponentFactory m_deviceFactory;

    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=DeviceParameter)")
    volatile ComponentFactory m_deviceParameterFactory;

    @ServiceDependency
    volatile LogService log;

    @Start
    public void start() {
        log.log(LogService.LOG_INFO, "DeviceAndParameterFactory.start");
        for (int i = 0; i < 2; i++) {
            createDeviceAndParameter(i);
        }
    }

    private void createDeviceAndParameter(int id) {
        log.log(LogService.LOG_INFO, "DeviceAndParameterFactory: creating Device/DeviceParameter with id=" + id);

        Dictionary<String, Object> device = new Hashtable<>();
        device.put("device.id", new Integer(id));
        m_deviceFactory.newInstance(device);

        Dictionary<String, Object> param = new Hashtable<>();
        param.put("device.id", new Integer(id));
        m_deviceParameterFactory.newInstance(param);
    }
}
