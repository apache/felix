package org.apache.felix.dependencymanager.samples.hello;

import java.util.Dictionary;

import org.apache.felix.dependencymanager.samples.util.Helper;

/**
 * Our service consumer. We depend on a ServiceProvider, and on a configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceConsumer {
	volatile ServiceProvider service;
	
    protected void update(Dictionary<?, ?> conf) {
        Helper.log("hello", "ServiceConsumer.update: conf=" + conf);
    }
    
	public void start() {
        Helper.log("hello", "ServiceConsumer.start: calling service.hello()");
		this.service.hello();
	}
}
