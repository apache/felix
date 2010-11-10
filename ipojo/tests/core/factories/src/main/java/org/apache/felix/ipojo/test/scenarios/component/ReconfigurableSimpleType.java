package org.apache.felix.ipojo.test.scenarios.component;

public class ReconfigurableSimpleType {


	private String prop; // Property.

	public void start () {
		if (prop == null || prop.equals("KO")) {
			throw new IllegalStateException("Bad Configuration : " + prop);
		}
		System.out.println("OK !!!!");
	}



}
