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


import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.MutatingService;
import org.apache.felix.scr.integration.components.MutatingServiceConsumer;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;


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
        String componentName = "components.mutable.properties";
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.SATISFIED);

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=" + componentName + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Assert.assertEquals("otherValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService(serviceReference );
        Assert.assertNotNull(s);
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.ACTIVE);
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        Assert.assertEquals("anotherValue", serviceReference.getProperty(PROP_NAME));
        checkPropertiesNotPresent(serviceReference, "p1", "p2");

        //configure with configAdmin
        configure( componentName );
        delay();
        //no change
        Assert.assertEquals("anotherValue", serviceReference.getProperty(PROP_NAME));
        checkPropertiesNotPresent(serviceReference, "p1", "p2");

        //check that removing config switches back to defaults modified by config admin
        s.updateProperties(null);
        Assert.assertEquals("theValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void test_mutable_properties_returned() throws InvalidSyntaxException
    {
        String componentName = "components.mutable.properties.return";
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.SATISFIED);

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=" + componentName + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Assert.assertEquals("otherValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService( serviceReference );
        Assert.assertNotNull(s);
        Assert.assertEquals("anotherValue1", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.ACTIVE);
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        Assert.assertEquals("anotherValue", serviceReference.getProperty(PROP_NAME));
        checkPropertiesNotPresent(serviceReference, "p1", "p2");

        //configure with configAdmin
        configure( componentName );
        delay();
        delay();
        //no change
        Assert.assertEquals("anotherValue2", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        //check that removing config switches back to defaults modified by config admin
        s.updateProperties(null);
        Assert.assertEquals("theValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void test_mutable_properties_returned_public() throws InvalidSyntaxException
    {
        String componentName = "components.mutable.properties.return.public";
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.SATISFIED);

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=" + componentName + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Assert.assertEquals("otherValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        //update theValue
        MutatingService s = ( MutatingService ) bundleContext.getService( serviceReference );
        Assert.assertNotNull(s);
        Assert.assertEquals("anotherValue1", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.ACTIVE);
        Dictionary d = new Hashtable(Collections.singletonMap( PROP_NAME, "anotherValue" ));
        s.updateProperties(d);
        Assert.assertEquals("anotherValue", serviceReference.getProperty(PROP_NAME));
        checkPropertiesNotPresent(serviceReference, "p1", "p2");

        //configure with configAdmin
        configure( componentName );
        delay();
        delay();
        //no change
        Assert.assertEquals("anotherValue2", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        //check that removing config switches back to defaults modified by config admin
        s.updateProperties(null);
        Assert.assertEquals("theValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void test_mutable_properties_bind_returned() throws InvalidSyntaxException
    {
        String componentName = "components.mutable.properties.bind";
        findComponentConfigurationByName(componentName, ComponentConfigurationDTO.SATISFIED);

        ServiceReference[] serviceReferences = bundleContext.getServiceReferences( MutatingService.class.getName(), "(service.pid=" + componentName + ")" );
        TestCase.assertEquals( 1, serviceReferences.length );
        ServiceReference serviceReference = serviceReferences[0];
        Assert.assertEquals("otherValue", serviceReference.getProperty(PROP_NAME));
        Assert.assertEquals("p1", serviceReference.getProperty("p1"));
        Assert.assertEquals("p2", serviceReference.getProperty("p2"));
        MutatingService s = ( MutatingService ) bundleContext.getService( serviceReference );

        SimpleServiceImpl srv1 = SimpleServiceImpl.create( bundleContext, "srv1" );
        checkPropertiesNotPresent(serviceReference, "p1", "p2");
        Assert.assertEquals("bound", serviceReference.getProperty("SimpleService"));

        srv1.update( "foo" );
        checkPropertiesNotPresent(serviceReference, "p1", "p2");
        Assert.assertEquals("updated", serviceReference.getProperty("SimpleService"));

        srv1.drop();
        checkPropertiesNotPresent(serviceReference, "p1", "p2");
        Assert.assertEquals("unbound", serviceReference.getProperty("SimpleService"));

        bundleContext.ungetService(serviceReference);
    }

    private void checkPropertiesNotPresent(ServiceReference<?> serviceReference, String ... props) {
        for (String p : props) {
            Assert.assertFalse("Should not contain property " + p, Arrays.asList(serviceReference.getPropertyKeys()).contains(p));
        }
    }

    @Test
    public void test_mutable_properties_consumer() throws Exception
    {
        ServiceReference<MutatingServiceConsumer> mscRef = bundleContext.getServiceReference(MutatingServiceConsumer.class);
        MutatingServiceConsumer msc = bundleContext.getService(mscRef);
        assertMsc(msc, null, null, null);

        String componentName = "components.mutable.properties.return2";
        ComponentDescriptionDTO cd = findComponentDescriptorByName(componentName);

        enableAndCheck(cd);

        assertMsc(msc, true, true, null);
    }

    private void assertMsc(MutatingServiceConsumer msc, Boolean set, Boolean updated, Boolean unset)
    {
        Assert.assertEquals("set ", set, msc.isUpdatedInSet());
        Assert.assertEquals("updated ", updated, msc.isUpdatedInUpdated());
        Assert.assertEquals("unset ", unset, msc.isUpdatedInUnset());
    }
}