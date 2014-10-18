package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.Component;

@Component(factorySet="DeviceParameter", factoryConfigure="configure")
public class DeviceParameterImpl implements DeviceParameter {
    int id;
        
    void configure(Dictionary<String, Object> configuration) {
        this.id = (Integer) configuration.get("device.id");
    }

    @Override
    public int getDeviceId() {
        return id;
    }
}
