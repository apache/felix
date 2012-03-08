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


import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.MutatingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceReference;


@RunWith(JUnit4TestRunner.class)
public class MutablePropertiesTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
//        paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_mutable_properties.xml";
    }


    @Test
    public void test_mutable_properties()
    {
        final Component component = findComponentByName( "components.mutable.properties" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference serviceReference = bundleContext.getServiceReference( MutatingService.class.getName() );
        checkProperties( serviceReference, 8, "otherValue", "p1", "p2" );

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService(serviceReference );
        Assert.assertNotNull(s);
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        checkProperties(serviceReference, 8, "anotherValue", "p1", "p2");

        //configure with configAdmin
        configure( "components.mutable.properties" );
        delay();
        checkProperties(serviceReference, 8, PROP_NAME, "p1", "p2");

        //check that a property from config admin can't be changed
        s.updateProperties(d);
        checkProperties(serviceReference, 8, PROP_NAME, "p1", "p2");

        //check that another one can
        s.updateProperties(new Hashtable(Collections.singletonMap( "p1", "changed" )));
        checkProperties(serviceReference, 8, PROP_NAME, "changed", "p2");

        bundleContext.ungetService(serviceReference);
    }

    private void checkProperties(ServiceReference serviceReference, int count, String otherValue, String p1, String p2) {
        Assert.assertEquals("wrong property count", count, serviceReference.getPropertyKeys().length);
        Assert.assertEquals(otherValue, serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals(p1, serviceReference.getProperty("p1"));
        Assert.assertEquals(p2, serviceReference.getProperty("p2"));
    }


}