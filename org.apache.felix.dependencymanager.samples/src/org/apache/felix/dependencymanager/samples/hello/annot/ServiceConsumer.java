package org.apache.felix.dependencymanager.samples.hello.annot;

import java.util.Dictionary;

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
        System.out.println("ServiceConsumer updated with conf " + conf);
    }
    
    @Start
	public void start() {
        System.out.println(Thread.currentThread().getName() + ": Starting " + this.getClass().getName());
		this.service.hello();
	}
}
