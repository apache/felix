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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.transaction.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy.withiPOJO;


public class TestMandatory extends Common {


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
                .set(Constants.BUNDLE_SYMBOLICNAME, "MandatoryTransactionPropagation")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services, javax.transaction;version=1.1")
                .build(withiPOJO(new File(TEST, "mandatory.xml")));


        return OptionUtils.combine(
                options,
                provision(
                        service,
                        fooimpl,
                        test
                ));

    }

    @Test(expected = RuntimeException.class)
    public void testOkOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingGood(); // Fail !
    }

    @Test
    public void testOkInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        cs.doSomethingGood();
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        t.commit();
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingBad(); // Fail, RTE thrown before the other exception
    }

    @Test(expected = RollbackException.class)
    public void testExceptionInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad();
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, t.getStatus());

        t.commit(); // Throws a rollback exception.
    }

    @Test
    public void testExceptionInsideTransactionRB() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad();
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, t.getStatus());

        t.rollback();
    }

    @Test(expected = RuntimeException.class)
    public void testExpectedExceptionOutsideTransaction() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        ((CheckService) osgiHelper.getServiceObject(ref)).doSomethingBad2(); // Throws a RTE
    }

    @Test
    public void testExpectedExceptionInsideTransaction() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-ok");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad2();
            Assert.fail("UnsupportedOperationException expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        Assert.assertEquals(Status.STATUS_ACTIVE, t.getStatus());

        t.commit();
    }

    @Test(expected = RuntimeException.class)
    public void testOkOutsideTransactionWithCallback() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);


        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        cs.doSomethingGood(); // Throws a RTE.

    }

    @Test
    public void testOkInsideTransactionWithCallback() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        cs.doSomethingGood();
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        t.commit();

        Assert.assertNull(cs.getLastRolledBack());
        Assert.assertNotNull(cs.getLastCommitted());
        Assert.assertEquals(1, cs.getNumberOfCommit());
        Assert.assertEquals(0, cs.getNumberOfRollback());

        Assert.assertSame(t, cs.getLastCommitted());
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionOutsideTransactionWithCallback() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        cs.doSomethingBad(); // Thows a RTE.

    }

    @Test
    public void testExceptionInsideTransactionWithCallback() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad();
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, t.getStatus());

        try {
            t.commit(); // Throw a rollback exception.
        } catch (RollbackException e) {
            // Expected
        } catch (Throwable e) {
            Assert.fail(e.getMessage()); // Unexpected
        }

        Assert.assertNotNull(cs.getLastRolledBack());
        Assert.assertNull(cs.getLastCommitted());
        Assert.assertEquals(0, cs.getNumberOfCommit());
        Assert.assertEquals(1, cs.getNumberOfRollback());

        Assert.assertSame(t, cs.getLastRolledBack());
    }

    @Test(expected = RuntimeException.class)
    public void testExpectedExceptionOutsideTransactionWithCallback() {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);

        cs.doSomethingBad2();


    }

    @Test
    public void testExpectedExceptionInsideTransactionWithCallback() throws NotSupportedException, SystemException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
        ComponentInstance prov = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.handler.transaction.components.FooImpl");
        ComponentInstance under = ipojoHelper.createComponentInstance("mandatory-cb");

        Assert.assertEquals(ComponentInstance.VALID, prov.getState());
        Assert.assertEquals(ComponentInstance.VALID, under.getState());

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), under.getInstanceName());
        Assert.assertNotNull(ref);

        osgiHelper.waitForService(TransactionManager.class.getName(), null, 5000);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        TransactionManager tm = (TransactionManager) osgiHelper.getServiceObject(TransactionManager.class.getName(), null);
        tm.begin();
        Transaction t = tm.getTransaction();
        try {
            cs.doSomethingBad2();
            Assert.fail("UnsupportedOperationException expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }
        Transaction t2 = cs.getCurrentTransaction();
        Assert.assertSame(t2, t);
        Assert.assertEquals(Status.STATUS_ACTIVE, t.getStatus());

        t.commit();

        Assert.assertNull(cs.getLastRolledBack());
        Assert.assertNotNull(cs.getLastCommitted());
        Assert.assertEquals(1, cs.getNumberOfCommit());
        Assert.assertEquals(0, cs.getNumberOfRollback());

        Assert.assertSame(t, cs.getLastCommitted());
    }


}
