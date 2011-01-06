package org.apache.felix.ipojo.test.scenarios.component.error;

import org.apache.felix.ipojo.annotations.Requires;

public abstract class AbstractClass {

	@Requires
	private Runnable run;

}
