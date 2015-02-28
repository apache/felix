package org.apache.felix.dependencymanager.samples.dynamicdep.api;

import java.util.Dictionary;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.metatype.Configurable;

/**
 * This Component depends on the following services declared from the Activator:
 * - LogService
 * - Configuration with PID="org.apache.felix.dependencymanager.samples.dynamicdep.api.DynamicDependencyConfiguration"
 * 
 * We the define a dynamic dependency on a Storage Service from our init method and we configure the dependency filter and
 * required from using the injected configuration in our updated method.
 */
public class DynamicDependency {

	volatile EventAdmin eventAdmin; // injected and defined in Activator
	volatile LogService log; // injected and defined in Activator
	volatile Storage storage; // dependency defined dynamically from our init() method
	private String storageType; // type of Storage to depend on (we get that from configadmin)
	private boolean storageRequired; // is our Storage dependency required or not (we get that from configadmin)

	/**
	 * This is the first callback: we are injected with our configuration.
	 */
	public void updated(Dictionary<String, Object> properties) throws ConfigurationException {
        // We use the bnd "Configurable" helper in order to get an implementation for our DictionaryConfiguration interface.
		DynamicDependencyConfiguration cnf = Configurable.createConfigurable(DynamicDependencyConfiguration.class, properties);
		storageType = cnf.storageType();
		storageRequired = cnf.storageRequired();
	}

	/**
	 * The configuration has been injected and also other required dependencies defined from the Activator.
	 * Now, define some dynamic dependencies (here we use the configuration injected from our updated method in 
	 * order to configure the filter and required flag for the "Storage" dependency).
	 */
	public void init(Component c) {
		log.log(LogService.LOG_WARNING, "init: storage type=" + storageType + ", storageRequired=" + storageRequired);
		DependencyManager dm = c.getDependencyManager();
		// all dynamic dependencies must be declared atomically in the Component.add(...) method, which accepts varargs.
		c.add(dm.createServiceDependency().setService(Storage.class, "(type=" + storageType + ")").setRequired(storageRequired));
	}

	/**
	 * All dependencies injected, including dynamic dependencies defined from init method.
	 */
	void start() {
		log.log(LogService.LOG_WARNING, "start");
		// Use storage to load/store some key-value pairs ...
		storage.store("gabu", "zo");
	}
	
}
