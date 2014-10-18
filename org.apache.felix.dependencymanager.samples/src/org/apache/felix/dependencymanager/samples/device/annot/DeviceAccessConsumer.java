package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.service.log.LogService;

@Component
public class DeviceAccessConsumer {
    @ServiceDependency
    volatile LogService log;
    
    // Injected afer all required dependencies have been injected (including our logger)
    @ServiceDependency(required=false)
    void add(Map<String, Object> props, DeviceAccess deviceAccess) {
        log.log(LogService.LOG_INFO, "Handling device access: id=" + props.get("device.id") 
            + "\n\t device=" + deviceAccess.getDevice() 
            + "\n\t device parameter=" + deviceAccess.getDeviceParameter()
            + "\n\t device access properties=" + props);
    }
}
