/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.scr.integration;

import java.util.Iterator;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

@RunWith(JUnit4TestRunner.class)
public class ComponentConcurrencyTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_component_concurrency.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".concurrency";
        restrictedLogging = true;
        ignoredWarnings = new String[] {"FrameworkEvent: ERROR",
        		"FrameworkEvent ERROR",
        		"Could not get service from ref",
        		"Failed creating the component instance; see log for reason",
        		"Cannot create component instance due to failure to bind reference",
        		"DependencyManager : invokeBindMethod : Service not available from service registry for ServiceReference"};
        DS_LOGLEVEL = "warn";
    }

    protected static void delay(int secs)
    {
        try
        {
            Thread.sleep(secs * 1000);
        }
        catch (InterruptedException ie)
        {
        }
    }

    @Test
    public void test_concurrent_component_activation_using_componentFactories() throws Exception
    {


    	ComponentConfigurationDTO ccA = getDisabledConfigurationAndEnable( "org.apache.felix.scr.integration.components.concurrency.AFactory", ComponentConfigurationDTO.ACTIVE );
    	ComponentConfigurationDTO ccC = getDisabledConfigurationAndEnable( "org.apache.felix.scr.integration.components.concurrency.CFactory", ComponentConfigurationDTO.ACTIVE );

        delay( 30 );
        if ( ! log.foundWarnings().isEmpty() )
        {
            TestCase.fail( "unexpected warning or error logged: " + log.foundWarnings() );
        }
        for ( String message: log.foundWarnings() )
        {
            TestCase.fail( "unexpected warning or error logged: " + message );
        }
    }
}
