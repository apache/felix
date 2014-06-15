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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
//import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
//import org.osgi.service.cm.ConfigurationEvent;
//import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@RunWith(JUnit4TestRunner.class)
public class TargetedPIDTest extends ComponentTestBase
{
    
    private static final String TARGETED_PID = "targetedPID";
    private static final String COMPONENT_NAME = "SimpleComponent.configuration.require";
    private static final String REGION = "?foo";

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
        
        delay();
        
        //Add more and more specific components to check that they pick up the appropriate configuration
        Set<ComponentConfigurationDTO> known = new HashSet<ComponentConfigurationDTO>();
        
        final ComponentConfigurationDTO component = findComponentConfigurationByName( COMPONENT_NAME, ComponentConfigurationDTO.ACTIVE );
        known.add( component );

        TestCase.assertNotNull( SimpleComponent.INSTANCE );
        SimpleComponent sc = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pid, sc.getProperty( TARGETED_PID ) );
        
        
        Bundle bSN = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.11", null );
        bSN.start();
        findComponentConfigurationByName( bSN, pid, ComponentConfigurationDTO.ACTIVE );

        
        SimpleComponent scSN = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSN, scSN.getProperty( TARGETED_PID ) );
        
        Bundle bSNV = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.12", null );
        bSNV.start();
        findComponentConfigurationByName( bSNV, pid, ComponentConfigurationDTO.ACTIVE );
        SimpleComponent scSNV = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSNV, scSNV.getProperty( TARGETED_PID ) );
        
        Bundle bSNVL = installBundle( descriptorFile, COMPONENT_PACKAGE, "simplecomponent2", "0.0.12", "bundleLocation" );
        bSNVL.start();
        findComponentConfigurationsByName( bSNVL, pid, ComponentConfigurationDTO.ACTIVE );

        SimpleComponent scSNVL = SimpleComponent.INSTANCE;
        TestCase.assertEquals( pidSNVL, scSNVL.getProperty( TARGETED_PID ) );
        
        //remove configurations to check that the components now use the less specific configurations.
        
        configSNVL.delete();
        delay();
        findComponentConfigurationsByName( bSNVL, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pidSNV, scSNVL.getProperty( TARGETED_PID ) );
        
        configSNV.delete();
        delay();
        findComponentConfigurationsByName( bSNVL, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pidSN, scSNVL.getProperty( TARGETED_PID ) );
        findComponentConfigurationByName( bSNV, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pidSN, scSNV.getProperty( TARGETED_PID ) );
        
        configSN.delete();
        delay();
        findComponentConfigurationsByName( bSNVL, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pid, scSNVL.getProperty( TARGETED_PID ) );
        findComponentConfigurationByName( bSNV, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pid, scSNV.getProperty( TARGETED_PID ) );
        findComponentConfigurationByName( bSN, pid, ComponentConfigurationDTO.ACTIVE );
        TestCase.assertEquals( pid, scSN.getProperty( TARGETED_PID ) );
        
        
    }

}
