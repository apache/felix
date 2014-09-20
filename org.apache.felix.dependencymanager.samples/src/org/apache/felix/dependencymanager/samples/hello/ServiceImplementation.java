package org.apache.felix.dependencymanager.samples.hello;

public class ServiceImplementation implements ServiceInterface {
	@Override
	public void hello() {
        System.out.println(this.getClass().getName() + " : Hello");
	}
}
