package org.apache.felix.dependencymanager.samples.hello;

import java.util.Dictionary;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceConsumer {
	volatile ServiceProvider service;
	
    protected void update(Dictionary<?, ?> conf) {
        System.out.println("ServiceConsumer updated with conf " + conf);
    }
    
	public void start() {
        System.out.println(Thread.currentThread().getName() + ": Starting " + this.getClass().getName());
		this.service.hello();
	}
}
