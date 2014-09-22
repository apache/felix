package org.apache.felix.dependencymanager.samples.device;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

public class DeviceConsumer {
    volatile Device device;
    volatile DeviceParameter deviceParameter;
    
    void init(Component c) {
        DependencyManager dm = c.getDependencyManager();
        c.add(dm.createServiceDependency()
            .setService(DeviceParameter.class, "(device.id=" + device.getDeviceId() + ")")
            .setRequired(true));
    }
    
    void start() {
        System.out.println("Created a DeviceConsumer for device id " + device.getDeviceId() + ", device parameter id "
            + deviceParameter.getDeviceId());
    }
}
