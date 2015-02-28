package org.apache.felix.dependencymanager.samples.dynamicdep.annot;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
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
@Component
public class DynamicDependency {
	@ServiceDependency
	volatile EventAdmin eventAdmin;
	
	@ServiceDependency
	volatile LogService log; 
		
	@ServiceDependency(name="storage")
	volatile Storage storage; // dependency defined dynamically from our init() method

	private String storageType; // type of Storage to depend on (we get that from configadmin)
	private boolean storageRequired; // is our Storage dependency required or not (we get that from configadmin)

	/**
	 * This is the first callback: we are injected with our configuration.
	 */
	@ConfigurationDependency(pidClass=DynamicDependencyConfiguration.class)
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
	@Init
	Map<String, String> init() {
		log.log(LogService.LOG_WARNING, "init: storage type=" + storageType + ", storageRequired=" + storageRequired);
		Map<String, String> props = new HashMap<>();
		props.put("storage.required", Boolean.toString(storageRequired));
		props.put("storage.filter", "(type=" + storageType + ")");
		return props;		
	}
	
	/**
	 * All dependencies injected, including dynamic dependencies defined from init method.
	 */
	@Start
	void start() {
		log.log(LogService.LOG_WARNING, "start");
		// Use storage to load/store some key-value pairs ...
		storage.store("gabu", "zo");
	}
	
}
