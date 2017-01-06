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

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.apache.felix.scr.integration.components.deadlock.TestComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@RunWith(JUnit4TestRunner.class)
public class LocateTest extends ComponentTestBase
{
    
    static
    {
        descriptorFile = "/integration_test_locate.xml";
        // uncomment to enable debugging of this test class
//         paxRunnerVmOption = DEBUG_VM_OPTION;
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".deadlock";
    }
    
    @Test
    public void testAsyncLocate() throws Exception
    {
        bundleContext.registerService( Object.class, new Object(), null );
        
        findComponentConfigurationByName( "Consumer", ComponentConfigurationDTO.ACTIVE );
        
        final String pid = "TestComponent";
        Configuration config = getConfigurationAdmin().getConfiguration( pid, null );
        final Hashtable props = new Hashtable();
        //wrong target property, will not bind
        props.put( "target", "bar" );
        config.update(props);
        delay();
        
        //when deadlock is present the state is actually unsatisfied.
        ComponentConfigurationDTO cc = findComponentConfigurationByName( pid, ComponentConfigurationDTO.SATISFIED );
//        delay();
        //correct target property: will bind as new properties are propagated.
        props.put( "target", "foo" );
        config.update(props);
        delay();
       
        TestComponent tc = getServiceFromConfiguration(cc, TestComponent.class);
        assertTrue(tc.isSuccess1());
        assertTrue(tc.isSuccess2());
    }

}
