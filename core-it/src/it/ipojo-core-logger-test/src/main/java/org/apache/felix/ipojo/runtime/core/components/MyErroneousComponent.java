package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.MyService;

public class MyErroneousComponent implements MyService {

	public MyErroneousComponent() {
		throw new NullPointerException("bad");
	}

    public void foo() {
        // Nothing to do.
    }

}
