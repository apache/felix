package org.apache.felix.dependencymanager.samples.hello;

public class ServiceConsumer {
	volatile ServiceInterface service;
	
	public void start() {
        System.out.println("Starting " + this.getClass().getName());
		this.service.hello();
	}
}
