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
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;


public class SingletonComponentTest extends Common {

    private BundleContext context;

    @Before
    public void setUp() {
        context = getContext();
    }

    @Test
    public void createAServiceProvider() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci = null;

        SingletonComponentType type = createAProvider();
        ci = type.create();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));
        type.disposeInstance(ci);

    }

    @Test
    public void killTheFactory() throws Exception {
        assertThat(context, is(notNullValue()));
        ComponentInstance ci = null;
        SingletonComponentType type = createAProvider();
        ci = type.create();
        assertThat("Ci is valid", ci.getState(), is(ComponentInstance.VALID));
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Foo.class
                .getName(), ci.getInstanceName());
        assertThat(ref, is(notNullValue()));
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

        SingletonComponentType type = createAConsumer();
        ci = type.create();
        assertThat("Ci is invalid", ci.getState(),
                is(ComponentInstance.INVALID));
        type.stop();
    }

    @Test
    public void createBoth() throws Exception {
        SingletonComponentType consFactory = createAConsumer();
        ComponentInstance cons = consFactory.create();
        // cons is invalid
        assertThat("cons is invalid", cons.getState(), is(ComponentInstance.INVALID));

        SingletonComponentType provFactory = createAProvider();
        ComponentInstance prov = provFactory.create();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons.getState(), is(ComponentInstance.VALID));
        consFactory.stop();
        provFactory.stop();
    }

    @Test
    public void createTwoCons() throws Exception {
        SingletonComponentType consFactory = createAConsumer();
        ComponentInstance cons1 = createAConsumer().create();
        // cons is invalid
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().create();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons1.getState(), is(ComponentInstance.VALID));

        ComponentInstance cons2 = createAnOptionalConsumer().create();

        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));

        prov.stop();
        assertThat("cons is invalid", cons1.getState(), is(ComponentInstance.INVALID));
        assertThat("cons2 is valid", cons2.getState(), is(ComponentInstance.VALID));
    }

    @Test
   // @Ignore("We can't test as the MyComponentImpl must be manipulated before creating the object")
    public void setObject() throws Exception {
        ComponentInstance cons = createAConsumer().setObject(new MyComponentImpl(5)).create();
        // cons is invalid
        assertThat("cons is invalid", cons.getState(), is(ComponentInstance.INVALID));

        ComponentInstance prov = createAProvider().create();
        assertThat("prov is valid", prov.getState(), is(ComponentInstance.VALID));
        assertThat("cons is valid", cons.getState(), is(ComponentInstance.VALID));

    }

    private SingletonComponentType createAProvider() {
        PrimitiveComponentType type = new SingletonComponentType()
                .setBundleContext(context)
                .setClassName(FooImpl.class.getName())
                .addService(new Service()); // Provide the FooService

        return (SingletonComponentType) type;
    }

    private SingletonComponentType createAConsumer() {
        PrimitiveComponentType type = new SingletonComponentType()
                .setBundleContext(context)
                .setClassName(MyComponentImpl.class.getName())
                .setComponentTypeName("singleton.cons")
                .addDependency(new Dependency().setField("myFoo"))
                .setValidateMethod("start");

        return (SingletonComponentType) type;
    }

    private SingletonComponentType createAnOptionalConsumer() {
        PrimitiveComponentType type = new SingletonComponentType()
                .setBundleContext(context)
                .setClassName(MyComponentImpl.class.getName())
                .addDependency(new Dependency().setField("myFoo").setOptional(true))
                .setComponentTypeName("singleton.optional.consumer")
                .setValidateMethod("start");

        return (SingletonComponentType) type;

    }


}
