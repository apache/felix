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

import org.apache.felix.ipojo.handler.transaction.components.ComponentUsingAnnotations;
import org.apache.felix.ipojo.handler.transaction.components.FooImpl;
import org.apache.felix.ipojo.handler.transaction.services.CheckService;
import org.apache.felix.ipojo.handler.transaction.services.Foo;
import org.apache.felix.ipojo.metadata.Element;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy.withiPOJO;

public class TestAnnotations extends Common {

    public static final File TEST = new File("src/main/resources");

    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();

        InputStream service = TinyBundles.bundle()
                .add(CheckService.class)
                .add(Foo.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Service")
                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build(TinyBundles.withBnd());

        InputStream fooimpl = TinyBundles.bundle()
                .add(FooImpl.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Foo Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services")
                .build(withiPOJO(new File(TEST, "foo.xml")));

        InputStream test = TinyBundles.bundle()
                .add(ComponentUsingAnnotations.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "TransactionAnnotationTest")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.handler.transaction.services, javax.transaction;version=1.1")
                .build(withiPOJO(new File(TEST, "annotation.xml")));


        return OptionUtils.combine(
                options,
                provision(
                        service,
                        fooimpl,
                        test
                ),
                repository("http://maven.ow2.org/maven2-snapshot/")
        );
    }

    @Test
    public void annotations() {
        Element elem = IPOJOHelper.getMetadata(getBundle(), "org.apache.felix.ipojo.handler.transaction.components.ComponentUsingAnnotations");
        Assert.assertNotNull(elem);

        Element tr = elem.getElements("transaction", "org.apache.felix.ipojo.transaction")[0];
        Assert.assertEquals("transaction", tr.getAttribute("field"));

        Assert.assertNull(tr.getAttribute("oncommit"));
        Assert.assertNull(tr.getAttribute("onrollback"));

        Element[] methods = tr.getElements();
        Assert.assertEquals(4, methods.length);

        Element m1 = getElementByMethod(methods, "doSomethingBad");
        Assert.assertNotNull(m1);

        Element m2 = getElementByMethod(methods, "doSomethingBad2");
        Assert.assertNotNull(m2);
        Assert.assertEquals("required", m2.getAttribute("propagation"));

        Element m3 = getElementByMethod(methods, "doSomethingGood");
        Assert.assertNotNull(m3);
        Assert.assertEquals("supported", m3.getAttribute("propagation"));
        Assert.assertEquals("{java.lang.Exception}", m3.getAttribute("norollbackfor"));

        Element m4 = getElementByMethod(methods, "doSomethingLong");
        Assert.assertNotNull(m4);
        Assert.assertEquals("1000", m4.getAttribute("timeout"));
        Assert.assertEquals("true", m4.getAttribute("exceptiononrollback"));
    }

    private Element getElementByMethod(Element[] e, String m) {
        for (Element elem : e) {
            if (m.equals(elem.getAttribute("method"))) {
                return elem;
            }
        }
        Assert.fail("Method " + m + " not found");
        return null;
    }

    private Bundle getBundle() {
        for (Bundle b : context.getBundles()) {
            System.out.println(b.getSymbolicName());
            if ("TransactionAnnotationTest".equals(b.getSymbolicName())) {
                return b;
            }
        }
        Assert.fail("Cannot find the tested bundle");
        return null;
    }


}
