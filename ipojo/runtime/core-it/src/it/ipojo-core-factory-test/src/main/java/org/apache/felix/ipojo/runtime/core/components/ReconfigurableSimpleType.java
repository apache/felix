package org.apache.felix.ipojo.runtime.core.components;

public class ReconfigurableSimpleType {


	private String prop; // Property.

	private String x; // Property.

	boolean controller;

	public void start () {
		if (prop == null || prop.equals("KO")) {
			throw new IllegalStateException("Bad Configuration : " + prop);
		}

		if (x == null) {
			throw new IllegalStateException("x is null");
		}

		System.out.println("OK !!!!");
	}

	public void setX(String v) {
		x = v;
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
