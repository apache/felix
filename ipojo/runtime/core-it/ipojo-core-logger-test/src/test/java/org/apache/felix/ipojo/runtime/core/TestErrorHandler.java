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

package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ErrorHandler;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.runtime.core.components.MyComponent;
import org.apache.felix.ipojo.runtime.core.components.MyErroneousComponent;
import org.apache.felix.ipojo.runtime.core.services.MyService;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

@ExamReactorStrategy(PerMethod.class)
public class TestErrorHandler extends Common {

    @Configuration
    public Option[] config() throws IOException {

        Option[] options = super.config();

        return OptionUtils.combine(options,
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.log").version(asInProject()),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyService.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.services")
                                .build(withBnd())
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyComponent")
                                .set("Ipojo-log-level", "info")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/component.xml")))
                ),
                streamBundle(
                        TinyBundles.bundle()
                                .add(MyErroneousComponent.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "MyErroneousComponent")
                                .set("Ipojo-log-level", "debug")
                                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/erroneous-component.xml")))
                )
        );
    }

    @Test
    public void testErrorHandlerEmpty() throws InterruptedException, InvalidSyntaxException {
        MyErrorHandler handler = new MyErrorHandler();
        bc.registerService(ErrorHandler.class.getName(), handler, null);

        System.out.println(handler.m_errors);

        Assert.assertTrue(handler.m_errors.isEmpty());
    }

    @Test
    public void testErrorHandler() throws InterruptedException, InvalidSyntaxException {
        MyErrorHandler handler = new MyErrorHandler();
        bc.registerService(ErrorHandler.class.getName(), handler, null);

        Factory factory = ipojoHelper.getFactory("org.apache.felix.ipojo.runtime.core.components.MyErroneousComponent");
        assertNotNull(factory);
        try {
            factory.createComponentInstance(null);
            fail("Error expected");
        } catch (Exception e) {
            // Error expected.
        }

        System.out.println(handler.m_errors);

        Assert.assertFalse(handler.m_errors.isEmpty());
        Assert.assertTrue(handler.m_errors.contains("org.apache.felix.ipojo.runtime.core.components.MyErroneousComponent-0:[org.apache.felix.ipojo.runtime.core.components.MyErroneousComponent-0] createInstance -> Cannot invoke the constructor method - the constructor throws an exception : bad:bad"));
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
