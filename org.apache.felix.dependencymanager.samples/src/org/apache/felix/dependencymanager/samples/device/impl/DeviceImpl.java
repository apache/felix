package org.apache.felix.dependencymanager.samples.device.impl;

import org.apache.felix.dependencymanager.samples.device.Device;

public class DeviceImpl implements Device {
    final int id;
    
    DeviceImpl(int id) {
        this.id = id;
    }
    
    @Override
    public int getDeviceId() {
        return id;
    }
}
