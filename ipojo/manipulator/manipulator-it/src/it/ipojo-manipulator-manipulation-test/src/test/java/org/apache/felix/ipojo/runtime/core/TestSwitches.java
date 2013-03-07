package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestSwitches extends Common {

    private ComponentInstance instance;
    private CheckService service;

    @Before
    public void setUp() {
        instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.Switches");

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check service availability", ref);
        service = (CheckService) osgiHelper.getServiceObject(ref);
    }

    @After
    public void tearDown() {
        service = null;
    }

    @ProbeBuilder
    public TestProbeBuilder probe(TestProbeBuilder builder) {
        builder.setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework, org.apache.felix.ipojo, " +
                "org.ow2.chameleon.testing.helpers," +
                "org.apache.felix.ipojo.architecture, org.apache.felix.ipojo.handlers.dependency," +
                "org.apache.felix.ipojo.runtime.core.services, org.apache.felix.ipojo.runtime.core.components");
        builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.ops4j.pax.exam,org.junit,javax.inject," +
                "org.ops4j.pax.exam.options,junit.framework");
        builder.setHeader("Bundle-ManifestVersion", "2");
        return builder;
    }

    @Test
    public void testSwitches() {
        Properties properties = service.getProps();
        assertEquals(properties.get("switchOnInteger1"), "1");
        assertEquals(properties.get("switchOnInteger4"), "3");


        assertEquals(properties.get("switchOnEnumRed"), "RED");
    }


}
