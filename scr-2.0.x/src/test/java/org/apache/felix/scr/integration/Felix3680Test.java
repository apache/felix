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

import java.util.Iterator;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * This test validates the FELIX-3680 issue.
 */
@RunWith(JUnit4TestRunner.class)
public class Felix3680Test extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
        //        paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_FELIX_3680.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".felix3680";
        restrictedLogging = true;
        //comment to get debug logging if the test fails.
//        DS_LOGLEVEL = "warn";
    }

    @Inject
    protected BundleContext bundleContext;

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
    public void test_concurrent_reference_bindings() throws Exception
    {
        final ComponentDescriptionDTO main = findComponentDescriptorByName( "org.apache.felix.scr.integration.components.felix3680.Main" );
        enableAndCheck(main);

        delay(30);
        disableAndCheck( main );
        delay( ); //async deactivate
        for (Iterator it = log.foundWarnings().iterator(); it.hasNext();)
        {
            String message = (String) it.next();
            if (message.startsWith("Performed ") && message.endsWith(" tests."))
            {
                continue;
            }
            TestCase.fail("unexpected warning or error logged: " + message);
        }        
    }
}
