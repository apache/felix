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

<<<<<<< HEAD
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.circularFactory.FactoryClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
=======
import org.apache.felix.scr.integration.components.circularFactory.FactoryClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceReference;

import junit.framework.TestCase;

@RunWith(PaxExam.class)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
public class CircularFactoryTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
<<<<<<< HEAD
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_circularFactory.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".circularFactory";
   }
    
=======
        //        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_circularFactory.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".circularFactory";
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    @Test
    public void testCircularFactory() throws Exception
    {
        ServiceReference<FactoryClient> sr = bundle.getBundleContext().getServiceReference( FactoryClient.class );
<<<<<<< HEAD
        FactoryClient fc = bundle.getBundleContext().getService( sr );
=======
        bundle.getBundleContext().getService( sr );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        for ( String message: log.foundWarnings() )
        {
            TestCase.fail( "unexpected warning or error logged: " + message );
        }

    }

}
