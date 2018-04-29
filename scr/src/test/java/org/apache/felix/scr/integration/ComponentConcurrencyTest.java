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

<<<<<<< HEAD
import java.util.Iterator;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
=======
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import junit.framework.TestCase;

@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class ComponentConcurrencyTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
<<<<<<< HEAD
//        paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_component_concurrency.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".concurrency";
    }

    @Inject
    protected BundleContext bundleContext;

=======
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

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
    public void test_concurrent_component_activation_using_componentFactories()
    {


        final Component AFactory =
                findComponentByName( "org.apache.felix.scr.integration.components.concurrency.AFactory" );
        TestCase.assertNotNull( AFactory );
        AFactory.enable();

        final Component CFactory =
                findComponentByName( "org.apache.felix.scr.integration.components.concurrency.CFactory" );
        TestCase.assertNotNull( CFactory );
        CFactory.enable();

        delay( 30 );
        for ( Iterator it = log.foundWarnings().iterator(); it.hasNext();)
        {
            String message = ( String ) it.next();
            if ( message.contains( "FrameworkEvent ERROR" ) ||
                    message.contains( "Could not get service from ref" ) ||
                    message.contains( "Failed creating the component instance; see log for reason" ) ||
                    message.contains( "Cannot create component instance due to failure to bind reference" ))
            {
                continue;
            }
=======
    public void test_concurrent_component_activation_using_componentFactories() throws Exception
    {


        getDisabledConfigurationAndEnable( "org.apache.felix.scr.integration.components.concurrency.AFactory", ComponentConfigurationDTO.ACTIVE );
        getDisabledConfigurationAndEnable( "org.apache.felix.scr.integration.components.concurrency.CFactory", ComponentConfigurationDTO.ACTIVE );

        delay( 30 );
        if ( ! log.foundWarnings().isEmpty() )
        {
            TestCase.fail( "unexpected warning or error logged: " + log.foundWarnings() );
        }
        for ( String message: log.foundWarnings() )
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            TestCase.fail( "unexpected warning or error logged: " + message );
        }
    }
}
