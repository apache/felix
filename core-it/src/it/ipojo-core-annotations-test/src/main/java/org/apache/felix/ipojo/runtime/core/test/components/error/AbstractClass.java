package org.apache.felix.ipojo.runtime.core.test.components.error;

import org.apache.felix.ipojo.annotations.Requires;

public abstract class AbstractClass {

	@Requires
	private Runnable run;

}
