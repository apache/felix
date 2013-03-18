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

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class ConfigurationChangeTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
//         paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_simple_components_configuration_change.xml";
    }
/*
    @Test
    public void test_optional_single_dynamic()
    {
        String pid = "test_optional_single_dynamic";
        singleTest( pid, true );
    }

    @Test
    public void test_required_single_dynamic()
    {
        String pid = "test_required_single_dynamic";
        singleTest( pid, true );
    }

    @Test
    public void test_optional_single_static()
    {
        String pid = "test_optional_single_static";
        singleTest( pid, false );
    }

    @Test
    public void test_required_single_static()
    {
        String pid = "test_required_single_static";
        singleTest( pid, false );
    }

    @Test
    public void test_optional_single_dynamic_greedy()
    {
        String pid = "test_optional_single_dynamic_greedy";
        singleTest( pid, true );
    }

    @Test
    public void test_required_single_dynamic_greedy()
    {
        String pid = "test_required_single_dynamic_greedy";
        singleTest( pid, true );
    }

    @Test
    public void test_optional_single_static_greedy()
    {
        String pid = "test_optional_single_static_greedy";
        singleTest( pid, false );
    }

    @Test
    public void test_required_single_static_greedy()
    {
        String pid = "test_required_single_static_greedy";
        singleTest( pid, false );
    }
*/
    private void singleTest(String pid, boolean dynamic)
    {
        final Component component = findComponentByName( pid );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );
        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );

        theConfig.put("ref.target", "(value=srv1)");
        configure( pid );
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( srv1, comp10.m_singleRef );
        TestCase.assertTrue( comp10.m_multiRef.isEmpty() );
        TestCase.assertEquals( 1, comp10.m_singleRefBind );
        TestCase.assertEquals( 0, comp10.m_singleRefUnbind);

        // update configuration to target srv2
        theConfig.put("ref.target", "(value=srv2)");
        configure( pid );

        delay();
        // should bind to srv2
        SimpleComponent comp20;
        if ( dynamic )
        {
            TestCase.assertEquals( 1, comp10.m_modified );
            comp20 = comp10;
            TestCase.assertEquals( 2, comp20.m_singleRefBind );
            TestCase.assertEquals( 1, comp20.m_singleRefUnbind);
        } 
        else
        {
            TestCase.assertEquals( 0, comp10.m_modified );
            comp20 = SimpleComponent.INSTANCE;
            TestCase.assertNotSame( comp10, comp20 );
            TestCase.assertEquals( 0, comp20.m_modified );
            TestCase.assertEquals( 1, comp20.m_singleRefBind );
            TestCase.assertEquals( 0, comp20.m_singleRefUnbind);
            TestCase.assertEquals( 1, comp10.m_singleRefUnbind);
        }
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( srv2, comp20.m_singleRef );
        TestCase.assertTrue( comp20.m_multiRef.isEmpty() );
    }

    @Test
    public void test_optional_multiple_dynamic()
    {
        String pid = "test_optional_multiple_dynamic";
        multipleTest( pid, true );
    }

    private void multipleTest(String pid, boolean dynamic)
    {
        final Component component = findComponentByName( pid );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_DISABLED, component.getState() );

        final SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );
        final SimpleServiceImpl srv2 = SimpleServiceImpl.create( bundleContext, "srv2" );

        theConfig.put("ref.target", "(value=srv1)");
        configure( pid );
        // async enabling
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        final SimpleComponent comp10 = SimpleComponent.INSTANCE;
        TestCase.assertNotNull( comp10 );
        TestCase.assertEquals( 1, comp10.m_multiRef.size() );
        TestCase.assertEquals( srv1, comp10.m_multiRef.iterator().next() );
        TestCase.assertEquals( 1, comp10.m_multiRefBind );
        TestCase.assertEquals( 0, comp10.m_multiRefUnbind);

        // update configuration to target srv2
        theConfig.put("ref.target", "(value=srv2)");
        configure( pid );

        delay();
        // should bind to srv2
        SimpleComponent comp20;
        if ( dynamic )
        {
            TestCase.assertEquals( 1, comp10.m_modified );
            comp20 = comp10;
            TestCase.assertEquals( 2, comp20.m_multiRefBind );
            TestCase.assertEquals( 1, comp20.m_multiRefUnbind);
        } 
        else
        {
            TestCase.assertEquals( 0, comp10.m_modified );
            comp20 = SimpleComponent.INSTANCE;
            TestCase.assertNotSame( comp10, comp20 );
            TestCase.assertEquals( 0, comp20.m_modified );
            TestCase.assertEquals( 1, comp20.m_multiRefBind );
            TestCase.assertEquals( 0, comp20.m_multiRefUnbind);
            TestCase.assertEquals( 1, comp10.m_multiRefUnbind);
        }
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertEquals( 1, comp20.m_multiRef.size() );
        TestCase.assertEquals( srv2, comp20.m_multiRef.iterator().next() );
    }
    
}
