package org.apache.felix.ipojo.runtime.core.test.components.inner;

import org.apache.felix.ipojo.runtime.core.test.services.Call;

public class C1 implements Call {

	public String callMe() {
		return "called";
	}

}
