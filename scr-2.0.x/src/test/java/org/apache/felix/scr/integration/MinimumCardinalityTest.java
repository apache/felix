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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class MinimumCardinalityTest extends ComponentTestBase
{
    
    private static final String pid = "MinimumCardinality";
    
    static
    {
        descriptorFile = "/integration_test_min_cardinality.xml";
        // uncomment to enable debugging of this test class
//         paxRunnerVmOption = DEBUG_VM_OPTION;
        COMPONENT_PACKAGE = COMPONENT_PACKAGE;
    }
    
    @Test
    public void testMinCardinality() throws Exception
    {
        ServiceTracker<SimpleComponent, SimpleComponent> tracker = new ServiceTracker<SimpleComponent, SimpleComponent>(bundleContext, SimpleComponent.class, null);
        tracker.open();
        //configuration-policy require
        assertNull(tracker.getService());
        onePresent( tracker, null );
        onePresent( tracker, -1 );
        onePresent( tracker, 2 );
        onePresent( tracker, "-1" );
        onePresent( tracker, 'c' );
        onePresent( tracker, "2" );
        onePresent( tracker, new int[] {4, 0} );
        onePresent( tracker, null );
        
        configureOne(1);
        required(tracker, 1);
        onePresent( tracker, null );
        
        getConfigurationAdmin().getConfiguration( pid, null ).delete();
        delay();
        assertNull(tracker.getService());
        manyPresent( tracker, null );
        manyPresent( tracker, -1 );
        manyPresent( tracker, "-1" );
        manyPresent( tracker, new int[] {-4, 0} );
        manyPresent( tracker, null );
        
        configureMany(1);
        required(tracker, 1);
        configureMany(5);
        required(tracker, 5);
        manyPresent( tracker, null );
    }
    
    private void required(ServiceTracker<SimpleComponent, SimpleComponent> tracker, int count)
    {
        delay();
        List<SimpleServiceImpl> services = new ArrayList<SimpleServiceImpl>();
        for (int i = 0; i < count; i++)
        {
            assertNull("Expected no tracked with " + i + " services present, count " + count, tracker.getService());
            services.add(SimpleServiceImpl.create( bundleContext, String.valueOf( i ) ));
        }
        assertNotNull(tracker.getService());
        for (SimpleServiceImpl service: services)
        {
            service.drop();
            assertNull(tracker.getService());
        }
    }

    private void onePresent(ServiceTracker<SimpleComponent, SimpleComponent> tracker, Object value) throws IOException
    {
        configureOne(value);
        delay();
        assertNotNull(tracker.getService());
        assertEquals(1, tracker.getServices().length);
    }
    
    private void configureOne(Object value ) throws IOException
    {
        configureTarget( "one.cardinality.minimum", value );
    }

    private void manyPresent(ServiceTracker<SimpleComponent, SimpleComponent> tracker, Object value) throws IOException
    {
        configureMany(value);
        delay();
        assertNotNull(tracker.getService());
        assertEquals(1, tracker.getServices().length);
    }
    
    private void configureMany(Object value ) throws IOException
    {
        configureTarget( "many.cardinality.minimum", value );
    }

    private void configureTarget(final String targetKey, Object value) throws IOException
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        if ( value != null )
        {
            props.put( targetKey, value );
        }
        Configuration config = getConfigurationAdmin().getConfiguration( pid, null );
        config.update(props);
    }

}
