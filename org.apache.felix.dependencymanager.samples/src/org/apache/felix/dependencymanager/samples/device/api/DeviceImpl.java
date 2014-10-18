package org.apache.felix.dependencymanager.samples.device.api;


public class DeviceImpl implements Device {
    final int id;
    
    public DeviceImpl(int id) {
        this.id = id;
    }
    
    @Override
    public int getDeviceId() {
        return id;
    }
}
