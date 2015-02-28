package org.apache.felix.dependencymanager.samples.device.api;

import java.util.Map;

import org.osgi.service.log.LogService;

public class DeviceAccessConsumer {
    volatile LogService log;

    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        log.log(LogService.LOG_INFO, "DeviceAccessConsumer: Handling device access: id=" + props.get("device.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
