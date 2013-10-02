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

package org.apache.felix.ipojo.runtime.core.test.dependencies.leak;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.apache.felix.ipojo.runtime.core.test.services.HelloService;
import org.apache.felix.ipojo.runtime.core.test.services.LeakingService;
import org.apache.felix.ipojo.runtime.core.test.services.Listener;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Check the fix of the leak FELIX-4247 Memory leak with ServiceUsage and inner class (Listener style).
 */
public class ListenerStyleLeakTest extends Common {

    public static final String DEFAULT_HELLO_SERVICE = "org.apache.felix.ipojo.runtime.core.test.components.leak" +
            ".DefaultHelloService";
    public static final String DEFAULT_LEAKING_SERVICE = "org.apache.felix.ipojo.runtime.core.test.components.leak" +
            ".DefaultLeakingService";

    /**
     * This test does not use the listener-style.
     * <ol>
     *     <li>Gets the hello message from the leaking instance using the hello instance</li>
     *     <li>Stops the hello instance</li>
     *     <li>Get the hello message again, must be {@code null}</li>
     * </ol>
     *
     */
    @Test
    public void testNormalStyle() {

        ComponentInstance hello = ipojoHelper.createComponentInstance(DEFAULT_HELLO_SERVICE);
        ComponentInstance leaking = ipojoHelper.createComponentInstance(DEFAULT_LEAKING_SERVICE);

        assertThat(hello.getState()).isEqualTo(ComponentInstance.VALID);
        assertThat(leaking.getState()).isEqualTo(ComponentInstance.VALID);

        LeakingService service = osgiHelper.waitForService(LeakingService.class,
                "(instance.name=" + leaking.getInstanceName() + ")", 1000);
        osgiHelper.waitForService(HelloService.class, "(instance.name=" + hello.getInstanceName() + ")", 1000);

        String result = service.executeListener();
        assertThat(result).isEqualToIgnoringCase("hello iPOJO");

        hello.stop();

        result = service.executeListener();
        assertThat(result).isNull();
    }

    /**
     * This test do use the listener-style.
     * <ol>
     *     <li>Gets the listener object provided by the leaking instance</li>
     *     <li>Gets the hello message from this listener. As the hello service is bound,
     *     it relies on the hello instance</li>
     *     <li>Stops the hello instance</li>
     *     <li>Get the hello message again, must be {@code null}</li>
     * </ol>
     *
     */
    @Test
    public void testListenerStyle() {

        ComponentInstance hello = ipojoHelper.createComponentInstance(DEFAULT_HELLO_SERVICE);
        ComponentInstance leaking = ipojoHelper.createComponentInstance(DEFAULT_LEAKING_SERVICE);

        assertThat(hello.getState()).isEqualTo(ComponentInstance.VALID);
        assertThat(leaking.getState()).isEqualTo(ComponentInstance.VALID);

        LeakingService service = osgiHelper.waitForService(LeakingService.class,
                "(instance.name=" + leaking.getInstanceName() + ")", 1000);
        osgiHelper.waitForService(HelloService.class, "(instance.name=" + hello.getInstanceName() + ")", 1000);

        Listener listener = service.getListener();
        String result = listener.doSomething();
        assertThat(result).isEqualToIgnoringCase("hello iPOJO");

        hello.stop();

        result = listener.doSomething();
        assertThat(result).isNull();
    }
}
