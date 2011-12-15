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
package org.apache.felix.httplite.osgi.test;


import java.util.HashMap;
import java.util.ServiceLoader;

import junit.framework.TestCase;

import org.apache.felix.httplite.osgi.Activator;

import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;


/**
 * Common functionality for PojoSR-based tests.
 * 
 */
public abstract class AbstractPojoSRTestCase extends TestCase
{

    protected PojoServiceRegistry registry;
    protected Activator activator;


    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        //Initialize service registry
        ServiceLoader loader = ServiceLoader.load( PojoServiceRegistryFactory.class );

        registry = ( ( PojoServiceRegistryFactory ) loader.iterator().next() ).newPojoServiceRegistry( new HashMap() );

        assertNotNull( registry );

        //Initialize bundle
        activator = new Activator();
        activator.start( registry.getBundleContext() );
    }


    protected void tearDown() throws Exception
    {
        if ( activator != null && registry != null )
        {
            activator.stop( registry.getBundleContext() );
        }
        super.tearDown();
    }
}
