package org.apache.felix.ipojo.runtime.core.api;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.runtime.core.api.components.HostImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


public class ExternalHandlerTest extends Common {


    private BundleContext context;

    @Before
    public void setUp() {
        context = getContext();
    }

    @Test
    public void createAHost() throws Exception {
        PrimitiveComponentType type = createAWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat(ci.getState(), is(ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat(hd, is(notNullValue()));
    }

    @Test
    public void createDoubleHost() throws Exception {
        PrimitiveComponentType type = createASecondWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat(ci.getState(), is(ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat(hd, is(notNullValue()));
    }

    private PrimitiveComponentType createAWhiteboardHost() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(HostImpl.class.getName())
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=foo)")
                );
    }

    private PrimitiveComponentType createASecondWhiteboardHost() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(HostImpl.class.getName())
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=foo)")
                )
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=bar)")
                        .onModification("modification")
                );
    }

}
