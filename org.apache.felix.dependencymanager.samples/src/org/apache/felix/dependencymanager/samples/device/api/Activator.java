package org.apache.felix.dependencymanager.samples.device.api;

import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception { 
        createDeviceAndParameter(dm, 1);
        createDeviceAndParameter(dm, 2);

        dm.add(createAdapterService(Device.class, null)
            .setImplementation(DeviceAccessImpl.class)
            .setInterface(DeviceAccess.class.getName(), null));
        
        dm.add(createComponent()
            .setImplementation(DeviceAccessConsumer.class)
            .add(createServiceDependency().setService(LogService.class).setRequired(true))
            .add(createServiceDependency().setService(DeviceAccess.class).setRequired(true).setCallbacks("add", null)));
    }
    
    private void createDeviceAndParameter(DependencyManager dm, int id) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("device.id", id);
        dm.add(createComponent()
            .setImplementation(new DeviceImpl(id)).setInterface(Device.class.getName(), props));
           
        props = new Hashtable<>();
        props.put("device.id", id);
        dm.add(createComponent()
            .setImplementation(new DeviceParameterImpl(id)).setInterface(DeviceParameter.class.getName(), props));        
    }
}
