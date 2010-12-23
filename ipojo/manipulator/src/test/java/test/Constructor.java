package test;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;

@Component
public class Constructor {

	public Constructor(@Property(name="foo") String s, @Requires(id="t") Thread t) {
		// plop

	}

}
