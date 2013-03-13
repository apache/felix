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

package org.apache.felix.ipojo.handler.transaction.test;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.handler.transaction.components.FooDelegator;
import org.apache.felix.ipojo.handler.transaction.components.FooImpl;
import org.apache.felix.ipojo.handler.transaction.services.CheckService;
import org.apache.felix.ipojo.handler.transaction.services.Foo;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.transaction.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy.withiPOJO;

public class TestInstallation extends Common {

    public static final File TEST = new File("src/main/resources");


    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();

        InputStream service = TinyBundles.bundle()
                .add(CheckService.class)
                .add(Foo.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Service")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build();

        InputStream fooimpl = TinyBundles.bundle()
                .add(FooImpl.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Foo Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build(withiPOJO(new File(TEST, "foo.xml")));

        InputStream test = TinyBundles.bundle()
                .add(FooDelegator.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "RequiredTransactionPropagation")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services, javax.transaction;version=1.1")
                .build(withiPOJO(new File(TEST, "requires.xml")));


        return OptionUtils.combine(
                options,
                provision(
                        service,
                        fooimpl,
                        test
                ));
    }

    @Test
    public void install() throws NotSupportedException, SystemException, IllegalStateException,
            SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException, InvalidSyntaxException {
        Bundle[] bundles = context.getBundles();
        for (Bundle b : bundles) {
            System.out.println("Bundle : " + b.getSymbolicName() + " - " + b.getState());
            Assert.assertTrue(b.getSymbolicName(), b.getState() == Bundle.ACTIVE);
        }

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        // Transaction Service available
        osgiHelper.isServiceAvailable(TransactionManager.class.getName());
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Assert.assertNotNull(tm.getTransaction());
        tm.commit();

        tm.begin();
        Assert.assertNotNull(tm.getTransaction());
        tm.rollback();

        // Handler exposed
        ServiceReference ref = osgiHelper.getServiceReference(HandlerFactory.class.getName(), "(&(handler.name=transaction)(handler.namespace=org.apache.felix.ipojo.transaction))");
        Assert.assertNotNull(ref);

        // Create an install of the components
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("requires-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference[] refs = context.getAllServiceReferences(CheckService.class.getName(), "(instance.name=" + under.getInstanceName() + ")");

//        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(refs);

        ((CheckService) osgiHelper.getServiceObject(refs[0])).doSomethingGood();
    }

}
