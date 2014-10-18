package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

@AdapterService(adapteeService=Device.class)
public class DeviceAccessImpl implements DeviceAccess {
    volatile Device device;
    
    @ServiceDependency(name="deviceparam")
    volatile DeviceParameter deviceParameter;

    @ServiceDependency
    volatile LogService log;

    @Init
    Map<String, String> init() {
        log.log(LogService.LOG_INFO, "DeviceAccessImpl.init: device id=" + device.getDeviceId());
        // Dynamically configure our "deviceparam" dependency, using the already injected device service.
        Map<String, String> filters = new HashMap<>();
        filters.put("deviceparam.filter", "(device.id=" + device.getDeviceId() + ")");
        filters.put("deviceparam.required", "true");
        return filters;
    }
    
    @Start
    Map<?,?> start() {
        log.log(LogService.LOG_INFO, "DeviceAccessImpl.start");
        // Dynamically add a service property, using the device.id
        Map<String, Object> props = new Hashtable<>();
        props.put("device.access.id", device.getDeviceId());
        return props;
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
