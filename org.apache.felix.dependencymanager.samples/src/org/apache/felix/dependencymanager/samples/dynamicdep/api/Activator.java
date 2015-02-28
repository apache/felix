package org.apache.felix.dependencymanager.samples.dynamicdep.api;

import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext bc, DependencyManager dm)throws Exception {
		Properties props = new Properties();
		props.put("type", "mapdb");
		dm.add(createComponent()
				.setImplementation(MapDBStorage.class).setInterface(Storage.class.getName(), props)
				.add(createServiceDependency().setService(LogService.class).setRequired(true)));
		
		props = new Properties();
		props.put("type", "file");
		dm.add(createComponent()
				.setImplementation(FileStorage.class).setInterface(Storage.class.getName(), props)
				.add(createServiceDependency().setService(LogService.class).setRequired(true)));

		dm.add(createComponent()
				.setImplementation(DynamicDependency.class)
				.add(createServiceDependency().setService(LogService.class).setRequired(true))
				.add(createConfigurationDependency().setPid(DynamicDependencyConfiguration.class.getName()))
				.add(createServiceDependency().setService(EventAdmin.class).setRequired(true)));
	}

}
