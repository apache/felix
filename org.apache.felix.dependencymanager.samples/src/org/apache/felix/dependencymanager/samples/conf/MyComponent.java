package org.apache.felix.dependencymanager.samples.conf;

import java.util.Dictionary;

public class MyComponent {
	void updated(Dictionary conf) {
        System.out.println("Updating " + this.getClass().getName());
	}
	
	void start() {
        System.out.println("Starting " + this.getClass().getName());
	}
}
