package org.apache.felix.dependencymanager.samples.device;

import java.util.Map;

public class DeviceAccessConsumer {
    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        System.out.println("Handling device access: id=" + props.get("device.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
