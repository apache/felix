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
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


@RunWith(JUnit4TestRunner.class)
public class MutablePropertiesTest extends ComponentTestBase
{

    static
    {
        // uncomment to enable debugging of this test class
        //paxRunnerVmOption = DEBUG_VM_OPTION;

        descriptorFile = "/integration_test_mutable_properties.xml";
    }


    @Test
    public void test_mutable_properties() throws InvalidSyntaxException
    {
        final Component component = findComponentByName( "components.mutable.properties" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=components.mutable.properties)" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        checkProperties( serviceReference, 8, "otherValue", "p1", "p2" );

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService(serviceReference );
        Assert.assertNotNull(s);
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        checkProperties(serviceReference, 5, "anotherValue", "p1", "p2");

        //configure with configAdmin
        configure( "components.mutable.properties" );
        delay();
        //no change
        checkProperties(serviceReference, 5, "anotherValue", "p1", "p2");

        //check that removing config switches back to defaults modified by config admin
        s.updateProperties(null);
        checkProperties( serviceReference, 8, "theValue", "p1", "p2" );

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void test_mutable_properties_returned() throws InvalidSyntaxException
    {
        final Component component = findComponentByName( "components.mutable.properties.return" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=components.mutable.properties.return)" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        checkProperties( serviceReference, 8, "otherValue", "p1", "p2" );

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService( serviceReference );
        Assert.assertNotNull(s);
        checkProperties( serviceReference, 8, "anotherValue1", "p1", "p2" );
        TestCase.assertEquals( Component.STATE_ACTIVE, component.getState() );
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        checkProperties(serviceReference, 5, "anotherValue", "p1", "p2");

        //configure with configAdmin
        configure( "components.mutable.properties.return" );
        delay();
        delay();
        //no change
        checkProperties(serviceReference, 8, "anotherValue2", "p1", "p2");

        //check that removing config switches back to defaults modified by config admin
        s.updateProperties(null);
        checkProperties( serviceReference, 8, "theValue", "p1", "p2" );

        bundleContext.ungetService(serviceReference);
    }
    @Test
    public void test_mutable_properties_bind_returned() throws InvalidSyntaxException
    {
        final Component component = findComponentByName( "components.mutable.properties.bind" );
        TestCase.assertNotNull( component );
        TestCase.assertEquals( Component.STATE_REGISTERED, component.getState() );

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=components.mutable.properties.bind)" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        checkProperties( serviceReference, 8, "otherValue", "p1", "p2" );
        MutatingService s = ( MutatingService ) bundleContext.getService( serviceReference );

        SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );
        checkProperties( serviceReference, 5, null, "p1", "p2" );
        Assert.assertEquals("bound", serviceReference.getProperty("SimpleService"));

        srv1.update( "foo" );
        checkProperties( serviceReference, 5, null, "p1", "p2" );
        Assert.assertEquals("updated", serviceReference.getProperty("SimpleService"));

        srv1.drop();
        checkProperties( serviceReference, 5, null, "p1", "p2" );
        Assert.assertEquals("unbound", serviceReference.getProperty("SimpleService"));

        bundleContext.ungetService(serviceReference);
    }

    private void checkProperties(ServiceReference serviceReference, int count, String otherValue, String p1, String p2) {
        Assert.assertEquals("wrong property count", count, serviceReference.getPropertyKeys().length);
        if ( otherValue != null )
        {
            Assert.assertEquals(otherValue, serviceReference.getProperty(PROP_NAME));
        }
        if ( count > 5 ) {
            Assert.assertEquals(p1, serviceReference.getProperty("p1"));
            Assert.assertEquals(p2, serviceReference.getProperty("p2"));
        }
    }


}