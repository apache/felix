package org.apache.felix.dependencymanager.samples.device.impl;

import java.util.Map;

import org.apache.felix.dependencymanager.samples.device.DeviceAccess;

public class DeviceAccessConsumer {
    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        System.out.println("Handling device access: id=" + props.get("device.id") 
            + ", device=" + deviceAccess.getDevice() 
            + ", device parameter=" + deviceAccess.getDeviceParameter());
    }
}
