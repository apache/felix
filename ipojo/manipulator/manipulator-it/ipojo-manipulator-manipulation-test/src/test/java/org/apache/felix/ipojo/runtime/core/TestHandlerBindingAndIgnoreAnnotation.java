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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.runtime.core.services.BazService;
import org.apache.felix.ipojo.runtime.core.services.HandlerBindingTestService;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ow2.chameleon.testing.helpers.BaseTest;

import junit.framework.Assert;

public class TestHandlerBindingAndIgnoreAnnotation extends BaseTest {

    public static final String FACTORY_NAME = "org.apache.felix.ipojo.runtime.core.components.HandlerBindingTestComponent";

    @Test
    public void testFooHandlerBinding() {

/*
        HandlerFactory handlerFactory = ipojoHelper.getHandlerFactory("com.acme:foo");
        assertNotNull(handlerFactory);
        assertEquals(Factory.VALID, handlerFactory.getState());
*/

        // verify component's factory is here
        // verify BazService has been published
        // --> verify instance has been created

        Factory factory = ipojoHelper.getFactory(FACTORY_NAME);
        assertNotNull(factory);
        assertEquals(Factory.VALID, factory.getState());


        List<HandlerBindingTestService> services = osgiHelper.getServiceObjects(HandlerBindingTestService.class);
        assertEquals(1, services.size());

        HandlerBindingTestService baz = services.get(0);
        assertEquals("Bonjour", baz.get("greeting"));
        assertEquals("Welcome", baz.get("welcome"));
        assertNull(baz.get("ignored"));
        ipojoHelper.dispose();
    }
    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }

    @Override
    protected Option[] getCustomOptions() {
        return new Option[] {CoreOptions.vmOption("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000")};
        //return new Option[] {CoreOptions.vmOptions("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000")};
    }
}
