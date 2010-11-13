package org.apache.felix.ipojo.test.scenarios.component;

public class ReconfigurableSimpleType {


	private String prop; // Property.


	boolean controller;

	public void start () {
		if (prop == null || prop.equals("KO")) {
			throw new IllegalStateException("Bad Configuration : " + prop);
		}
		System.out.println("OK !!!!");
	}

	public void setProp(String p) {
		prop = p;
		if (prop == null || prop.equals("KO")) {
			controller = false;
		} else {
			controller = true;
			System.out.println("OK !!!!");
		}
	}

	public void setController(boolean p) {
		if (p) {
			System.out.println("OK !!!!");
		}
	}

}
