package org.apache.felix.ipojo.tests.core;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.newBundle;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOBuilder.withiPOJO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ErrorHandler;
import org.apache.felix.ipojo.tests.core.component.MyComponent;
import org.apache.felix.ipojo.tests.core.component.MyErroneousComponent;
import org.apache.felix.ipojo.tests.core.service.MyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith( JUnit4TestRunner.class )
public class ErrorHandlerTest {

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;

    private IPOJOHelper ipojo;

    @Before
    public void init() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
    }

    @After
    public void stop() {
        ipojo.dispose();
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {

        File tmp = new File("target/tmp");
        tmp.mkdirs();

        Option[] opt =  options(
                felix(),
//                equinox(),
                provision(
                        // Runtime.
                        mavenBundle().groupId( "org.apache.felix" ).artifactId( "org.apache.felix.log" ).version(asInProject()),
                        mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version(asInProject()),
                        mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject()
                        ),
                provision(
                        newBundle()
                            .add( MyService.class )
                            .set(Constants.BUNDLE_SYMBOLICNAME,"ServiceInterface")
                            .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .build()
                    ),
               provision(
                       // Component
                        newBundle()
                            .add(MyComponent.class)
                            .set(Constants.BUNDLE_SYMBOLICNAME,"MyComponent")
                            .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                            .set("Ipojo-log-level", "info")
                            .build( withiPOJO(new File(tmp, "provider-with-level-in-manifest.jar"), new File("component.xml")))
                            ),
                provision(
                        // Component
                         newBundle()
                             .add(MyErroneousComponent.class)
                             .set(Constants.BUNDLE_SYMBOLICNAME,"MyErroneousComponent")
                             .set("Ipojo-log-level", "debug")
                             .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.tests.core.service")
                             .build( withiPOJO(new File(tmp, "erroneous-provider-with-level-in-manifest.jar"), new File("erroneous-component.xml")))
                             )
                );
        return opt;
    }

    @Test
    public void testErrorHandlerEmpty() throws InterruptedException, InvalidSyntaxException {
    	MyErrorHandler handler = new MyErrorHandler();
    	context.registerService(ErrorHandler.class.getName(), handler, null);

        System.out.println(handler.m_errors);

        Assert.assertTrue(handler.m_errors.isEmpty());
    }

    @Test
    public void testErrorHandler() throws InterruptedException, InvalidSyntaxException {
    	MyErrorHandler handler = new MyErrorHandler();
    	context.registerService(ErrorHandler.class.getName(), handler, null);

    	try {
    		ipojo.createComponentInstance("org.apache.felix.ipojo.tests.core.component.MyErroneousComponent");
    	} catch (Exception e ) {
    		System.out.println(e);
    	}


        System.out.println(handler.m_errors);

        Assert.assertFalse(handler.m_errors.isEmpty());
        Assert.assertTrue(handler.m_errors.contains("org.apache.felix.ipojo.tests.core.component.MyErroneousComponent-0:[org.apache.felix.ipojo.tests.core.component.MyErroneousComponent-0] createInstance -> Cannot invoke the constructor method - the constructor throws an exception : bad:bad"));
    }


   private class MyErrorHandler implements ErrorHandler {

	   private List<String> m_errors = new ArrayList<String>();

	   public void onError(ComponentInstance instance, String message,
				Throwable error) {
		   System.out.println("on Error ! " + instance + " - " + message);
			if (instance == null) {
				if (error == null) {
					m_errors.add("no-instance:" + message);
				} else {
					m_errors.add("no-instance:" + message + ":" + error.getMessage());
				}
			} else {
				if (error == null) {
					m_errors.add(instance.getInstanceName() + ":" + message);
				} else {
					m_errors.add(instance.getInstanceName() + ":" + message + ":" + error.getMessage());
				}
			}
		}

	   public void onWarning(ComponentInstance instance, String message,
				Throwable error) {
		   System.out.println("on warning ! " + instance + " - " + message);
		}

   }


}
