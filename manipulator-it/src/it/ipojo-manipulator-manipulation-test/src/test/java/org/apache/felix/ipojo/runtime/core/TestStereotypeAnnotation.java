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
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.runtime.core.services.BazService;
import org.junit.Test;
import org.ow2.chameleon.testing.helpers.BaseTest;

import junit.framework.Assert;

public class TestStereotypeAnnotation extends BaseTest {

    public static final String BAZ_FACTORY_NAME = "org.apache.felix.ipojo.runtime.core.components.StereotypedBazComponent";

    public static final String MB_FACTORY_NAME = "org.apache.felix.ipojo.runtime.core.components.StereotypedMultiBind";

    @Test
    public void testTypeStereotype() {

        // verify component's factory is here
        // verify BazService has been published
        // --> verify instance has been created

        Factory factory = ipojoHelper.getFactory(BAZ_FACTORY_NAME);
        Assert.assertNotNull(factory);
        assertEquals(Factory.VALID, factory.getState());


        List<BazService> services = osgiHelper.getServiceObjects(BazService.class);
        assertEquals(1, services.size());

        BazService baz = services.get(0);
        assertEquals("Hello Guillaume", baz.hello("Guillaume"));
        ipojoHelper.dispose();
    }

    @Test
    public void testMethodStereotype() {

        // verify component's factory is here
        // verify that the requires handler has been activated
        // verify that a created instance works

        Factory factory = ipojoHelper.getFactory(MB_FACTORY_NAME);
        Assert.assertNotNull(factory);
        assertEquals(Factory.VALID, factory.getState());

        assertTrue(factory.getRequiredHandlers().contains("org.apache.felix.ipojo:requires"));

        ComponentInstance instance = ipojoHelper.createComponentInstance(MB_FACTORY_NAME, "stereotype-multibind-instance");
        assertTrue(ipojoHelper.isInstanceValid(instance));

        ipojoHelper.dispose();
    }

    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }
}
