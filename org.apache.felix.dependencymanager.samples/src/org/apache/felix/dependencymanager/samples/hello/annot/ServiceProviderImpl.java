package org.apache.felix.dependencymanager.samples.hello.annot;

import org.apache.felix.dm.annotation.api.Component;


/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class ServiceProviderImpl implements ServiceProvider {
	@Override
	public void hello() {
        System.out.println(Thread.currentThread().getName() + ": Hello");
	}
}
