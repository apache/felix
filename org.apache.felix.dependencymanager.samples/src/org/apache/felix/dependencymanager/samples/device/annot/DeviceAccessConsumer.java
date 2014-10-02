package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.Map;

import org.apache.felix.dependencymanager.samples.util.Helper;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

@Component
public class DeviceAccessConsumer {
    @ServiceDependency
    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        Helper.log("device.annot", "Handling device access: id=" + props.get("device.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
