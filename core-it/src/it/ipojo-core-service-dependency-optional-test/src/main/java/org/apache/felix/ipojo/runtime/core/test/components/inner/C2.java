package org.apache.felix.ipojo.runtime.core.test.components.inner;

import org.apache.felix.ipojo.runtime.core.test.services.Call;

public class C2 {

	private Call c1;

	public String authenticate() {
		return c1.callMe();
	}

}
