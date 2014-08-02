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
package org.apache.felix.scr.integration;


import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.EnableComponent;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;


@RunWith(JUnit4TestRunner.class)
public class ComponentEnableTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_enable.xml";
    }


    @Test
    public void test_Component_Enable() throws Exception
    {
        final String enable = "org.apache.felix.scr.integration.components.enable";
        final String name = "org.apache.felix.scr.integration.components.SimpleComponent";
        
        ComponentConfigurationDTO dto = findComponentConfigurationByName(enable, ComponentConfigurationDTO.SATISFIED);
        
        EnableComponent ec = getServiceFromConfiguration(dto, EnableComponent.class);
        
        TestCase.assertEquals(0, SimpleComponent.INSTANCES.size());

        ec.enable(name);
        delay();
        TestCase.assertEquals(1, SimpleComponent.INSTANCES.size());
        ec.enable(name);
        delay();
        TestCase.assertEquals(1, SimpleComponent.INSTANCES.size());

    }


}
