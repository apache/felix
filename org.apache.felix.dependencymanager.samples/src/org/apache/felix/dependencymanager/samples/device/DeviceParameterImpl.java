package org.apache.felix.dependencymanager.samples.device;


public class DeviceParameterImpl implements DeviceParameter {
    final int id;
    
    public DeviceParameterImpl(int id) {
        this.id = id;
    }

    @Override
    public int getDeviceId() {
        return id;
    }
}
