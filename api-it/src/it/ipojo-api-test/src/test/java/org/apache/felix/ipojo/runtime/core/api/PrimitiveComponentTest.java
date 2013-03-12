package org.apache.felix.ipojo.runtime.core.api;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.SingletonComponentType;
import org.apache.felix.ipojo.runtime.core.api.components.FooImpl;
import org.apache.felix.ipojo.runtime.core.api.components.MyComponentImpl;
import org.apache.felix.ipojo.runtime.core.api.services.Foo;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class PrimitiveComponentTest extends Common {

    private BundleContext context;

    @Before
    public void setUp() {
        context = getContext();
    }

    @Configuration
    public Option[] config() throws IOException {
        return super.config();
    }

    @Test
    public void createAServiceProvider() throws Exception {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci;

        PrimitiveComponentType type = createAProvider();
        ci = type.createInstance();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));

    }

    @Test
    public void killTheFactory() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci = null;

        PrimitiveComponentType type = createAProvider();
        ci = type.createInstance();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));
        type.stop();
        assertThat("Ci is disposed", ci.getState(),
                is(ComponentInstance.DISPOSED));
        ref = ipojoHelper.getServiceReferenceByName(Foo.class.getName(), ci
                .getInstanceName());
        assertThat(ref, is(nullValue()));

    }

    @Test
    public void createAServiceCons() throws Exception {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci = null;

        PrimitiveComponentType type = createAConsumer();
        ci = type.createInstance();
        assertThat("Ci is invalid", ci.getState(),
                is(ComponentInstance.INVALID));

    }

    @Test
    public void createBoth() throws Exception {
        ComponentInstance cons = createAConsumer().createInstance();
        // cons is invalid
        assertThat("cons is invalid", cons.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().createInstance();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createTwoCons() throws Exception {
        ComponentInstance cons1 = createAConsumer().createInstance();
        // cons is invalid
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().createInstance();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons1.getState(), is(ComponentInstance.VALID));

        ComponentInstance cons2 = createAnOptionalConsumer().createInstance();

        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));

        prov.stop();
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));
        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));
    }

    private PrimitiveComponentType createAProvider() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(FooImpl.class.getName())
                .addService(new Service()); // Provide the FooService
    }

    private PrimitiveComponentType createAConsumer() {
        return new SingletonComponentType()
                .setBundleContext(context)
                .setClassName(MyComponentImpl.class.getName())
                .addDependency(new Dependency().setField("myFoo"))
                .setValidateMethod("start");
    }

    private PrimitiveComponentType createAnOptionalConsumer() {
        return new SingletonComponentType()
                .setBundleContext(context)
                .setComponentTypeName("cons.optional")
                .setClassName(MyComponentImpl.class.getName())
                .addDependency(new Dependency().setField("myFoo").setOptional(true))
                .setValidateMethod("start");
    }


}
