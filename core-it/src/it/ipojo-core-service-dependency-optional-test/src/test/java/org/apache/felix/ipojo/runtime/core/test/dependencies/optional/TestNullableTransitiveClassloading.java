package org.apache.felix.ipojo.runtime.core.test.dependencies.optional;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.Test;


/**
 * Reproduces FELIX-2093
 * iPOJO doesn't always use the correct class loader to load nullable object.
 */
public class TestNullableTransitiveClassloading extends Common {


    @Test
    public void testCreation() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Factory factory = ipojoHelper.getFactory("optional-log-cons");
        ComponentInstance ci = factory.createComponentInstance(null);
        ci.dispose();
    }

}
