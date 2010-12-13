package org.apache.felix.ipojo.test.scenarios.component.inner;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.Call;

public class C1 implements Call {

	public String callMe() {
		return "called";
	}

}
