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
package org.apache.felix.dm.test.bundle.annotation.publisher;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * This test validates that a basic "ProviderImpl" which is instantiated from a FactorySet can register/unregister its
 * service using the Publisher annotation.
 */
public class FactoryServiceTestWthPublisher
{
    @Service
    public static class Consumer
    {
        @ServiceDependency(filter="(test=testFactoryService)")
        Sequencer m_sequencer;
        
        @ServiceDependency(required=false, removed = "unbind")
        void bind(Map properties, Provider provider)
        {
            m_sequencer.step(1);
            if ("bar".equals(properties.get("foo"))) 
            {
                m_sequencer.step(2);
            }
            if ("bar2".equals(properties.get("foo2"))) 
            {
                m_sequencer.step(3);
            }
            if ("bar3".equals(properties.get("foo3"))) 
            {
                m_sequencer.step(4);
            }
        }

        void unbind(Provider provider)
        {
            m_sequencer.step(5);
        }
    }
   
    @Service(publisher="m_publisher", unpublisher="m_unpublisher", factorySet="MyFactory", properties={@Property(name="foo", value="bar")})
    public static class ProviderImpl implements Provider
    {
        Runnable m_publisher; // injected and used to register our service
        Runnable m_unpublisher; // injected and used to unregister our service
        
        @ServiceDependency(filter="(test=testFactoryService)")
        Sequencer m_sequencer;
        
        @Start
        Map start()
        {
            // register service in 1 second
            schedule(m_publisher, 1000);
            // unregister the service in 2 seconds
            schedule(m_unpublisher, 2000);
            // At this point, our service properties are the one specified in our @Service annotation + the one specified by our Factory.
            // We also append an extra service property here:
            return new HashMap() {{ put("foo3", "bar3"); }};
        }

        private void schedule(final Runnable task, final long n)
        {
            Thread t = new Thread() {
                public void run()
                {
                    try
                    {
                        sleep(n);
                    }
                    catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    task.run();
                }
            };
            t.start();
        }
    }
    
    @Service
    public static class ProviderImplFactory 
    {
        @ServiceDependency(filter="(dm.factory.name=MyFactory)")
        void bind(Set<Dictionary> m_providerImplFactory)
        {
            m_providerImplFactory.add(new Hashtable() {{ put("foo2", "bar2"); }});
        }
    }
}
