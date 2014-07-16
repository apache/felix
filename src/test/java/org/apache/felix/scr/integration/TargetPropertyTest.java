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

import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@RunWith(JUnit4TestRunner.class)
public class TargetPropertyTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//         paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_target_properties.xml";
    }

    @Test
    public void test_1() throws Exception
    {
        String pid = "components.target.properties.1";
        singleTest( pid, "foo" );
    }

    @Test
    public void test_1_2() throws Exception
    {
        String pid = "components.target.properties.1.2";
        singleTest( pid, "foo" );
    }

    @Test
    public void test_1_3() throws Exception
    {
        String pid = "components.target.properties.1.3";
        singleTest( pid, "foo" );
    }

    @Test
    public void test_2() throws Exception
    {
        String pid = "components.target.properties.2";
        singleTest( pid, "foo" );
    }

    @Test
    public void test_3() throws Exception
    {
        String pid = "components.target.properties.3";
        singleTest( pid, "bar" );
    }


    private void singleTest(String pid, String expected) throws Exception
    {
        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, expected );
        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "baz" );

		getDisabledConfigurationAndEnable(pid, ComponentConfigurationDTO.ACTIVE);
        checkTarget(expected, srv1);
        
        //configuration not setting target property does not change it
        configure( pid );
        delay();//all cm event to complete
        checkTarget(expected, srv1);
        
        // update configuration to target srv2
        theConfig.put("one.target", "(value=baz)");
        configure( pid );
        delay();
        checkTarget("baz", srv2);
        
        //update configuration removing target property
        theConfig.remove("one.target");
        configure( pid );
        delay();//all cm event to complete
        checkTarget(expected, srv1);
        
    }

    void checkTarget(String expected, final SimpleServiceImpl srv1)
    {
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals("(value=" + expected + ")", comp10.getProperty("one.target"));
        TestCase.assertEquals( srv1, comp10.m_singleRef );
    }


}
