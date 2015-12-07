package org.apache.felix.bundlerepository;

import org.osgi.framework.Bundle;

public interface LocalResource extends Resource {

    Bundle getBundle();
}
