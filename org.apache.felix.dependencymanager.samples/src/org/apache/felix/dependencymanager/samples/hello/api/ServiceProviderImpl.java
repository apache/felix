package org.apache.felix.dependencymanager.samples.hello.api;

import org.osgi.service.log.LogService;


/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProviderImpl implements ServiceProvider {
    volatile LogService log;
    
	@Override
	public void hello() {
        log.log(LogService.LOG_INFO, "ServiceProviderImpl.hello");
	}
}
