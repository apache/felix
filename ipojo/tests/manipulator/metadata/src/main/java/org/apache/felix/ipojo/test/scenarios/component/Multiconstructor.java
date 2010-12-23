package org.apache.felix.ipojo.test.scenarios.component;

public class Multiconstructor {

	public Multiconstructor(String s1, String s2) {
		this(s1, s2, -1);
	}

	public Multiconstructor(String s1, int s2) {
		this(s1, "" + s2, s2);
	}

	public Multiconstructor(String s1, String s2, int i) {
		//...
	}

}
