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
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestSwitches extends BaseTest {

    private ComponentInstance instance;
    private CheckService service;

    @Before
    public void setUp() {
        instance = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.Switches");

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check service availability", ref);
        service = (CheckService) osgiHelper.getServiceObject(ref);
    }

    @After
    public void tearDown() {
        service = null;
    }

    @ProbeBuilder
    public TestProbeBuilder probe(TestProbeBuilder builder) {
        builder.setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework, org.apache.felix.ipojo, " +
                "org.ow2.chameleon.testing.helpers," +
                "org.apache.felix.ipojo.architecture, org.apache.felix.ipojo.handlers.dependency," +
                "org.apache.felix.ipojo.runtime.core.services, org.apache.felix.ipojo.runtime.core.components");
        builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.ops4j.pax.exam,org.junit,javax.inject," +
                "org.ops4j.pax.exam.options,junit.framework");
        builder.setHeader("Bundle-ManifestVersion", "2");
        return builder;
    }

    @Test
    public void testSwitches() {
        Properties properties = service.getProps();
        assertEquals(properties.get("switchOnInteger1"), "1");
        assertEquals(properties.get("switchOnInteger4"), "3");


        assertEquals(properties.get("switchOnEnumRed"), "RED");
    }

    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }


}
