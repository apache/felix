package org.apache.felix.dependencymanager.samples.device;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

public class DeviceAccessImpl implements DeviceAccess {
    volatile Device device;
    volatile DeviceParameter deviceParameter;

    void init(Component c) {
        // Dynamically add an extra dependency on a DeviceParameter.
        DependencyManager dm = c.getDependencyManager();
        c.add(dm.createServiceDependency()
            .setService(DeviceParameter.class, "(device.id=" + device.getDeviceId() + ")")
            .setRequired(true));
        
        // Dynamically add a service property, using the device.id
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("device.access.id", device.getDeviceId());
        c.setServiceProperties(props);
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
