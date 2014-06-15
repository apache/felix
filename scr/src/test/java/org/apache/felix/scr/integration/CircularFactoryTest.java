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

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.circularFactory.FactoryClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
public class CircularFactoryTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_circularFactory.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".circularFactory";
   }
    
    @Test
    public void testCircularFactory() throws Exception
    {
        ServiceReference<FactoryClient> sr = bundle.getBundleContext().getServiceReference( FactoryClient.class );
        FactoryClient fc = bundle.getBundleContext().getService( sr );

        for ( String message: log.foundWarnings() )
        {
            TestCase.fail( "unexpected warning or error logged: " + message );
        }

    }

}
