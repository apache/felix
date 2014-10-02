package org.apache.felix.dependencymanager.samples.hello;

import org.apache.felix.dependencymanager.samples.util.Helper;


/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProviderImpl implements ServiceProvider {
	@Override
	public void hello() {
        Helper.log("hello", "ServiceProviderImpl.hello");
	}
}
