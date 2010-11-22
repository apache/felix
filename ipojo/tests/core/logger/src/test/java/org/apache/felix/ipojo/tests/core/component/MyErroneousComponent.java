package org.apache.felix.ipojo.tests.core.component;

import org.apache.felix.ipojo.tests.core.service.MyService;

public class MyErroneousComponent implements MyService {

	public MyErroneousComponent() {
		throw new NullPointerException("bad");
	}

    public void foo() {
        // Nothing to do.
    }

}
