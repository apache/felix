package org.apache.felix.ipojo.runtime.core.test.components.inner;

public class C3 {

	private C2 c2;

	public MyFilter getFilter()	{
		return new MyFilter() {
			public String authenticate() {
				System.out.println("My Filter ...");
				String r = c2.authenticate();
				System.out.println(" ... " + r);
				return r;
			}
		};

	}

}
