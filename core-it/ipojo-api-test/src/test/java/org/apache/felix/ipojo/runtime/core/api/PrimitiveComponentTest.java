/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.api;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.api.Dependency;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.api.Service;
import org.apache.felix.ipojo.api.SingletonComponentType;
import org.apache.felix.ipojo.runtime.core.api.components.FooImpl;
import org.apache.felix.ipojo.runtime.core.api.components.MyComponentImpl;
import org.apache.felix.ipojo.runtime.core.api.components.PlainHelloImpl;
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
        assertThat(ipojoHelper.isServiceAvailableByName(Foo.class.getName(), ci
                .getInstanceName()), is(true));
        type.stop();
        assertThat("Ci is disposed", ci.getState(),
                is(ComponentInstance.DISPOSED));
        assertThat(ipojoHelper.isServiceAvailableByName(Foo.class.getName(), ci
                .getInstanceName()), is(false));

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

    @Test
    public void notManipulatedComponent() throws Exception {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci;

        PrimitiveComponentType x= new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(PlainHelloImpl.class.getName())
                .setValidateMethod("start")
                .setInvalidateMethod("stop");

        x.start();

        assertThat(x, is(notNullValue()));
        assertThat(x.getFactory().getState(), is(Factory.VALID));

        ci = x.createInstance();
        ci.start();
        assertThat(ci.getState(), is(ComponentInstance.VALID));

        x.stop();
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
