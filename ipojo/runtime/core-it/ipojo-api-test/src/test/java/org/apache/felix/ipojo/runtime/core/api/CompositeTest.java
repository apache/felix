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
import org.apache.felix.ipojo.api.composite.*;
import org.apache.felix.ipojo.runtime.core.api.components.FooImpl;
import org.apache.felix.ipojo.runtime.core.api.components.MyComponentImpl;
import org.apache.felix.ipojo.runtime.core.api.services.Foo;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;


public class CompositeTest extends Common {

    private BundleContext context;

    @Before
    public void setUp() {
        context = getContext();
    }

    @Test
    public void createACompositeWithcontainedInstance() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        PrimitiveComponentType cons = createAConsumer();

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("comp1")
                .addInstance(new Instance(prov.getFactory().getName()))
                .addInstance(new Instance(cons.getFactory().getName()));

        ComponentInstance ci = type.createInstance();

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

        // Stop cons
        cons.stop();
        assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

        // Restart cons
        cons.start();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createACompositeWithAnInstantiatedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        prov.start();
        PrimitiveComponentType cons = createAConsumer();

        ServiceReference[] refs = osgiHelper.getServiceReferences(Factory.class.getName(),
                "(component.providedServiceSpecifications=" + Foo.class.getName() + ")");
        assertThat(refs.length, is(not(0)));

        Factory factory = (Factory) osgiHelper.getRawServiceObject(refs[0]);
        System.out.println(factory.getComponentDescription().getDescription());

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("comp2")
                .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()))
                .addInstance(new Instance(cons.getFactory().getName()));

        ComponentInstance ci = type.createInstance();

        System.out.println(ci.getInstanceDescription().getDescription());

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

        // Stop prov
        prov.stop();
        assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

        // Restart prov
        prov.start();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createACompositeWithAnOptionalInstantiatedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        prov.start();

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("comp3")
                .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()).setOptional(true));

        ComponentInstance ci = type.createInstance();

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

        // Stop prov
        prov.stop();
        assertThat("ci is valid - 1", ci.getState(), is(ComponentInstance.VALID));

        // Restart prov
        prov.start();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createACompositeWithAnImportedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        prov.createInstance();
        PrimitiveComponentType cons = createAConsumer();

        ServiceReference[] refs = osgiHelper.getServiceReferences(Factory.class.getName(),
                "(component.providedServiceSpecifications=" + Foo.class.getName() + ")");
        assertThat(refs.length, is(not(0)));

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("comp2")
                .addSubService(new ImportedService().setSpecification(Foo.class.getName()))
                .addInstance(new Instance(cons.getFactory().getName()));

        ComponentInstance ci = type.createInstance();

        System.out.println(ci.getInstanceDescription().getDescription());

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

        // Stop prov
        prov.stop();
        assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));

        // Restart prov
        prov.start();
        prov.createInstance();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createACompositeWithAnOptionalImportedService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        prov.createInstance();

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("comp3")
                .addSubService(new ImportedService().setSpecification(Foo.class.getName()).setOptional(true));

        ComponentInstance ci = type.createInstance();

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));

        // Stop prov
        prov.stop();
        assertThat("ci is valid - 1", ci.getState(), is(ComponentInstance.VALID));

        // Restart prov
        prov.start();
        prov.createInstance();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));

    }

    @Test
    public void createACompositeWithExportingAService() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Define the component types
        PrimitiveComponentType prov = createAProvider();
        prov.start();
        PrimitiveComponentType cons = createAConsumer();
        ComponentInstance c = cons.createInstance();

        CompositeComponentType type = new CompositeComponentType()
                .setBundleContext(context)
                .setComponentTypeName("compExport")
                .addSubService(new InstantiatedService().setSpecification(Foo.class.getName()))
                .addService(new ExportedService().setSpecification(Foo.class.getName()));

        ComponentInstance ci = type.createInstance();

        assertThat("ci is valid", ci.getState(), is(ComponentInstance.VALID));
        assertThat("c is valid", c.getState(), is(ComponentInstance.VALID));


        // Stop prov
        prov.stop();
        assertThat("ci is invalid", ci.getState(), is(ComponentInstance.INVALID));
        assertThat("c is invalid", c.getState(), is(ComponentInstance.INVALID));


        // Restart prov
        prov.start();
        assertThat("ci is valid - 2", ci.getState(), is(ComponentInstance.VALID));
        assertThat("c is valid - 2", c.getState(), is(ComponentInstance.VALID));


    }

    private PrimitiveComponentType createAProvider() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(FooImpl.class.getName())
                .setPublic(true)
                .addService(new Service()); // Provide the FooService
    }

    private PrimitiveComponentType createAConsumer() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(MyComponentImpl.class.getName())
                .addDependency(new Dependency().setField("myFoo"))
                .setValidateMethod("start");
    }


}
