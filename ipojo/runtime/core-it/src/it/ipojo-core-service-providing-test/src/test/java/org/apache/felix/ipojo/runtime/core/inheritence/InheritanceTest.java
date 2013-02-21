package org.apache.felix.ipojo.runtime.core.inheritence;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.components.inheritance.a.IA;
import org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB;
import org.apache.felix.ipojo.runtime.core.components.inheritance.c.C;
import org.apache.felix.ipojo.runtime.core.components.inheritance.d.D;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public class InheritanceTest extends Common {

    @Configuration
    public Option[] config() throws IOException {
        deployTestedBundle = false;
        Option[] options = super.config();

        return OptionUtils.combine(
                options,
                streamBundle(
                        // Bundle A
                        TinyBundles.bundle()
                                .add(IA.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "A")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components.inheritance.a")
                                .build(withBnd())
                ),
                streamBundle(
                        // Bundle B
                        TinyBundles.bundle()
                                .add(IB.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "B")
                                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components.inheritance.a")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.components" +
                                        ".inheritance.b")
                                .build(withBnd())
                ),
                streamBundle(
                        // Bundle C
                        TinyBundles.bundle()
                                .add(C.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "C")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.apache.felix.ipojo.runtime.core.components.inheritance.a, " +
                                                "org.apache.felix.ipojo.runtime.core.components.inheritance.b")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/inheritance/provider" +
                                        ".xml")))
                ),
                streamBundle(
                        // Bundle D
                        TinyBundles.bundle()
                                .add(D.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "D")
                                .set(Constants.IMPORT_PACKAGE,
                                        "org.apache.felix.ipojo.runtime.core.components.inheritance.a, " +
                                                "org.apache.felix.ipojo.runtime.core.components.inheritance.b")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/inheritance/cons" +
                                        ".xml")))
                )


        );
    }

    @Test
    public void testDeploy() {
        Bundle[] bundles = bc.getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Assert.assertEquals(bundles[i].getSymbolicName() + " is not active", Bundle.ACTIVE, bundles[i].getState());
        }

        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=c)", 2000);
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 2000);

        Object[] arch = osgiHelper.getServiceObjects(Architecture.class.getName(), null);
        for (Object o : arch) {
            Architecture a = (Architecture) o;
            if (a.getInstanceDescription().getState() != ComponentInstance.VALID) {
                Assert.fail("Instance " + a.getInstanceDescription().getName() + " not valid : " + a.getInstanceDescription().getDescription());
            }
        }
    }

    @Test
    public void testArchitecture() {
        osgiHelper.waitForService(Architecture.class.getName(), "(architecture.instance=d)", 2000);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), "d");
        Assert.assertNotNull(ref);

        Architecture arch = (Architecture) osgiHelper.getServiceObject(ref);

        System.out.println(arch.getInstanceDescription().getDescription());

        Assert.assertEquals(ComponentInstance.VALID, arch.getInstanceDescription().getState());
        DependencyDescription dd = getDependency(arch, "org.apache.felix.ipojo.runtime.core.components.inheritance.b.IB");

        Assert.assertTrue(!dd.getServiceReferences().isEmpty());

        ServiceReference dref = (ServiceReference) dd.getServiceReferences().get(0);
        Assert.assertEquals(dref.getBundle().getSymbolicName(), "C");

    }

    private DependencyDescription getDependency(Architecture arch, String id) {
        DependencyHandlerDescription hd = (DependencyHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:requires");
        Assert.assertNotNull(hd);
        for (DependencyDescription dd : hd.getDependencies()) {
            if (dd.getId().equals(id)) {
                return dd;
            }
        }
        Assert.fail("Dependency " + id + " not found");
        return null;
    }

}
