package org.apache.felix.dependencymanager.samples.hello;


/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProviderImpl implements ServiceProvider {
	@Override
	public void hello() {
        System.out.println(Thread.currentThread().getName() + ": Hello");
	}
}
