package org.apache.felix.dependencymanager.samples.device.impl;

import org.apache.felix.dependencymanager.samples.device.Device;
import org.apache.felix.dependencymanager.samples.device.DeviceAccess;
import org.apache.felix.dependencymanager.samples.device.DeviceParameter;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

public class DeviceAccessImpl implements DeviceAccess {
    volatile Device device;
    volatile DeviceParameter deviceParameter;

    void init(Component c) {
        DependencyManager dm = c.getDependencyManager();
        c.add(dm.createServiceDependency()
            .setService(DeviceParameter.class, "(device.id=" + device.getDeviceId() + ")")
            .setRequired(true));
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
