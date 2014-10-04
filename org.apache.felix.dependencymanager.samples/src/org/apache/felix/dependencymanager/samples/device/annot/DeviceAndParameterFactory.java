package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.dependencymanager.samples.util.Helper;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

/**
 * Component used to instantiate Device and DeviceParameter services, using DM annotation "factory set".
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class DeviceAndParameterFactory {
    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=Device)")
    volatile Set<Dictionary<?,?>> m_deviceFactory;
    
    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=DeviceParameter)")
    volatile Set<Dictionary<?,?>> m_deviceParameterFactory;

    @Start
    public void start() {
        Helper.log("device.annot", "DeviceAndParameterFactory.start");
        for (int i = 0; i < 2; i ++) {
            createDeviceAndParameter(i);
        }
    }
    
    private void createDeviceAndParameter(int id) {
        Helper.log("device.annot", "DeviceAndParameterFactory: creating Device/DeviceParameter with id=" + id);

        Dictionary<String,Object> device = new Hashtable<>();
        device.put("device.id", new Integer(id));
        m_deviceFactory.add(device);
        
        Dictionary<String, Object> param = new Hashtable<>();
        param.put("device.id", new Integer(id));
        m_deviceParameterFactory.add(param);
    }
}
