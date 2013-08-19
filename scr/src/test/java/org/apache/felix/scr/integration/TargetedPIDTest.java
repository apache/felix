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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;

@RunWith(JUnit4TestRunner.class)
public class TargetedPIDTest extends ComponentTestBase
{
    
    private static final String TARGETED_PID = "targetedPID";
    private static final String COMPONENT_NAME = "SimpleComponent.configuration.require";
    private static final String REGION = "?foo";
    private boolean eventReceived;

    static
    {
        bsnVersionUniqueness = "multiple";
        descriptorFile = "/integration_test_simple_components_location.xml";
        // uncomment to enable debugging of this test class
//         paxRunnerVmOption = DEBUG_VM_OPTION;
    }


    @Test
    public void testTargetedPID() throws Exception
    {
        try
        {
            new ConfigurationPermission(REGION, ConfigurationPermission.TARGET);
        }
        catch (IllegalArgumentException e)
        {
            return;//not an R5 CA
        }
        String pid = COMPONENT_NAME;
        theConfig.put(TARGETED_PID, pid);
        Configuration config = configure( pid );
        config.setBundleLocation( REGION );
        
        String pidSN = pid + "|simplecomponent2";
        theConfig.put(TARGETED_PID, pidSN);
        Configuration configSN = configure( pidSN );
        configSN.setBundleLocation( REGION );
        
        String pidSNV = pidSN + "|0.0.12";
        theConfig.put(TARGETED_PID, pidSNV);
        Configuration configSNV = configure( pidSNV );
        configSNV.setBundleLocation( REGION );
        
        String pidSNVL = pidSNV + "|bundleLocation";
        theConfig.put(TARGETED_PID, pidSNVL);
        Configuration configSNVL = configure( pidSNVL );
        configSNVL.setBundleLocation( REGION );
        
        //Add more and more specific components to check that they pick up the appropriate configuration
        Set<Component> known = new HashSet<Component>();
        
        final Component component = findComponentByName( COMPONENT_NAME );
        known.add( component );
        component.enable();
        delay();

        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        SimpleComponent sc = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pid, sc.getProperty( TARGETED_PID ) );
        
        
        Bundle bSN = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.11", null );
        bSN.start();
        Component[] components = findComponentsByName( pid );
        TestCase.assertEquals( 2, components.length );
        Component cSN = getNewComponent( known, components ); 

        
        cSN.enable();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSN.getState() );
        SimpleComponent scSN = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSN, scSN.getProperty( TARGETED_PID ) );
        
        Bundle bSNV = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.12", null );
        bSNV.start();
        components = findComponentsByName( pid );
        TestCase.assertEquals( 3, components.length );
        Component cSNV = getNewComponent( known, components ); 
        
        cSNV.enable();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNV.getState() );
        SimpleComponent scSNV = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSNV, scSNV.getProperty( TARGETED_PID ) );
        
        Bundle bSNVL = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.12", "bundleLocation" );
        bSNVL.start();
        components = findComponentsByName( pid );
        TestCase.assertEquals( 4, components.length );
        Component cSNVL = getNewComponent( known, components ); 
        
        cSNVL.enable();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNVL.getState() );
        SimpleComponent scSNVL = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSNVL, scSNVL.getProperty( TARGETED_PID ) );
        
        //remove configurations to check that the components now use the less specific configurations.
        
        configSNVL.delete();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNVL.getState() );
        TestCase.assertEquals( pidSNV, scSNVL.getProperty( TARGETED_PID ) );
        
        configSNV.delete();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNVL.getState() );
        TestCase.assertEquals( pidSN, scSNVL.getProperty( TARGETED_PID ) );
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNV.getState() );
        TestCase.assertEquals( pidSN, scSNV.getProperty( TARGETED_PID ) );
        
        configSN.delete();
        delay();
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNVL.getState() );
        TestCase.assertEquals( pid, scSNVL.getProperty( TARGETED_PID ) );
        TestCase.assertEquals( Component.STATE_ACTIVE, cSNV.getState() );
        TestCase.assertEquals( pid, scSNV.getProperty( TARGETED_PID ) );
        TestCase.assertEquals( Component.STATE_ACTIVE, cSN.getState() );
        TestCase.assertEquals( pid, scSN.getProperty( TARGETED_PID ) );
        
        
    }


    private Component getNewComponent(Set<Component> known, Component[] components)
    {
        List<Component> cs = new ArrayList(Arrays.asList( components )); 
        cs.removeAll( known );
        Component c = cs.get( 0 );
        known.add(c);
        return c;
    }


}
