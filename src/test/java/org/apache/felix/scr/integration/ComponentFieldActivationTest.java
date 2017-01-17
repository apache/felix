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
import java.util.Collection;

import org.apache.felix.scr.integration.components.FieldActivatorComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.Assert;
import junit.framework.TestCase;


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

        final String msg = cmp.test();
        assertNull(msg, msg);
        disableAndCheck( cc );
    }

    protected <S> void failGetServiceFromConfiguration(ComponentConfigurationDTO dto, Class<S> clazz)
    {
        long id = dto.id;
        String filter = "(component.id=" + id + ")";
        Collection<ServiceReference<S>> srs;
        try
        {
            srs = bundleContext.getServiceReferences( clazz, filter );
            Assert.assertEquals( "Nothing for filter: " + filter, 1, srs.size() );
            ServiceReference<S> sr = srs.iterator().next();
            assertNull(bundleContext.getService( sr ));
        }
        catch ( InvalidSyntaxException e )
        {
            TestCase.fail( e.getMessage() );
        }
    }

    @Test
    public void test_field_activator_failure() throws Exception
    {
        final String componentname = "FieldActivatorComponent.unsatisfied";

        ComponentConfigurationDTO cc = getDisabledConfigurationAndEnable(componentname, ComponentConfigurationDTO.SATISFIED);
        assertEquals(1, cc.description.activationFields.length);
        assertTrue(Arrays.asList(cc.description.activationFields).contains("foo"));

        this.failGetServiceFromConfiguration(cc, FieldActivatorComponent.class);

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

        this.failGetServiceFromConfiguration(cc, FieldActivatorComponent.class);

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
