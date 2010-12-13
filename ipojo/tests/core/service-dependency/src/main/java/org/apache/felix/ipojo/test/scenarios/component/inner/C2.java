package org.apache.felix.ipojo.test.scenarios.component.inner;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.Call;

public class C2 {

	private Call c1;

	public String authenticate() {
		return c1.callMe();
	}

}
