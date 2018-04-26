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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.felix.scr.integration.components.FieldActivatorComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;


@RunWith(PaxExam.class)
public class ComponentFieldActivationTest extends ComponentTestBase
{

    static
    {
        // use different components
        descriptorFile = "/integration_test_field_activation_components.xml";

        // uncomment to enable debugging of this test class
        // paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void test_field_activator_success() throws Exception
    {
        final String componentname = "FieldActivatorComponent.satisfied";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);
        assertEquals(4, cc.description.activationFields.length);
        assertTrue(Arrays.asList(cc.description.activationFields).contains("bundle"));
        assertTrue(Arrays.asList(cc.description.activationFields).contains("context"));
        assertTrue(Arrays.asList(cc.description.activationFields).contains("config"));
        assertTrue(Arrays.asList(cc.description.activationFields).contains("annotation"));

        FieldActivatorComponent cmp = this.getServiceFromConfiguration(cc, FieldActivatorComponent.class);

        assertTrue(cmp.isActivateCalled());
        assertTrue(cmp.notSetBeforeActivate().isEmpty());
        assertEquals(4, cmp.setBeforeActivate().size());
        assertNull(cmp.additionalError());

        disableAndCheck( cc );
    }

    @Test
    public void test_field_activator_failure() throws Exception
    {
        final String componentname = "FieldActivatorComponent.unsatisfied";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);
        assertEquals(1, cc.description.activationFields.length);
        assertTrue(Arrays.asList(cc.description.activationFields).contains("foo"));

        // we get the component, although the field does not exist!
        FieldActivatorComponent cmp = this.getServiceFromConfiguration(cc, FieldActivatorComponent.class);

        assertTrue(cmp.isActivateCalled());
        assertTrue(cmp.setBeforeActivate().isEmpty());
        assertEquals(4, cmp.notSetBeforeActivate().size());
        assertNull(cmp.additionalError());

        disableAndCheck( cc );
    }

    @Test
    public void test_field_activator_failure_partially() throws Exception
    {
        final String componentname = "FieldActivatorComponent.partiallysatisfied";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);
        assertEquals(3, cc.description.activationFields.length);
        assertTrue(Arrays.asList(cc.description.activationFields).contains("bundle"));
        assertTrue(Arrays.asList(cc.description.activationFields).contains("context"));
        assertTrue(Arrays.asList(cc.description.activationFields).contains("foo"));

        // we get the component, although some fields do not exist!
        FieldActivatorComponent cmp = this.getServiceFromConfiguration(cc, FieldActivatorComponent.class);

        assertTrue(cmp.isActivateCalled());
        assertEquals(2, cmp.setBeforeActivate().size());
        assertTrue(cmp.setBeforeActivate().contains("bundle"));
        assertTrue(cmp.setBeforeActivate().contains("context"));
        assertEquals(2, cmp.notSetBeforeActivate().size());
        assertNull(cmp.additionalError());

        disableAndCheck( cc );
    }

    @Test
    public void test_field_activator_dto_nofields() throws Exception
    {
        final String componentname = "FieldActivatorComponent.nofields";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);
        assertEquals(0, cc.description.activationFields.length);

        disableAndCheck( cc );
    }
}
