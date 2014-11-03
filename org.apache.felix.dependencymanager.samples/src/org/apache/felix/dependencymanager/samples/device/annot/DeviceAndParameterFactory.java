package org.apache.felix.dependencymanager.samples.device.annot;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.runtime.api.ComponentFactory;
import org.osgi.service.log.LogService;

/**
 * Component used to instantiate Device and DeviceParameter services, using DM annotation "factory set".
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class DeviceAndParameterFactory {
    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=Device)")
    volatile ComponentFactory m_deviceFactory;

    @ServiceDependency(filter = "(" + Component.FACTORY_NAME + "=DeviceParameter)")
    volatile ComponentFactory m_deviceParameterFactory;

    @ServiceDependency
    volatile LogService log;

    @Start
    public void start() {
        log.log(LogService.LOG_INFO, "DeviceAndParameterFactory.start");
        for (int i = 0; i < 2; i++) {
            createDeviceAndParameter(i);
        }
    }

    private void createDeviceAndParameter(int id) {
        log.log(LogService.LOG_INFO, "DeviceAndParameterFactory: creating Device/DeviceParameter with id=" + id);

        Dictionary<String, Object> device = new Hashtable<>();
        device.put("device.id", new Integer(id));
        m_deviceFactory.newInstance(device);

        Dictionary<String, Object> param = new Hashtable<>();
        param.put("device.id", new Integer(id));
        m_deviceParameterFactory.newInstance(param);
    }
}
