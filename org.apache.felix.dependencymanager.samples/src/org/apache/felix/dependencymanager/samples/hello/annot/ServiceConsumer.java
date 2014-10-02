package org.apache.felix.dependencymanager.samples.hello.annot;

import java.util.Dictionary;

import org.apache.felix.dependencymanager.samples.util.Helper;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class ServiceConsumer {
    @ServiceDependency
	volatile ServiceProvider service;
	
    @ConfigurationDependency
    protected void update(Dictionary<?, ?> conf) {
        Helper.log("hello.annot", "ServiceConsumer.update: " + conf);
    }
    
    @Start
	public void start() {
        Helper.log("hello.annot", "ServiceConsumer.start: calling service.hello() ...");
		this.service.hello();
	}
}
