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

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * This test validate that a basic "ProviderImpl" services can register/unregister its
 * service using the Publisher annotation.
 */
public class ServiceTestWthPublisher
{
    @Service
    public static class Consumer
    {
        @ServiceDependency(filter="(test=testService)")
        Sequencer m_sequencer;
        
        @ServiceDependency(required=false, removed = "unbind")
        void bind(Provider provider)
        {
            m_sequencer.step(1);
        }

        void unbind(Provider provider)
        {
            m_sequencer.step(2);
        }
        
        @Destroy
        void destroy() 
        {
            m_sequencer.step(3);
        }
    }
    
    @Service(publisher="m_publisher", unpublisher="m_unpublisher")
    public static class ProviderImpl implements Provider
    {
        Runnable m_publisher; // injected and used to register our service
        Runnable m_unpublisher; // injected and used to unregister our service
        
        @ServiceDependency(filter="(test=testService)")
        Sequencer m_sequencer;

        @Start
        void start()
        {
            // register service in 1 second
            schedule(m_publisher, 1000);
            // unregister the service in 2 seconds
            schedule(m_unpublisher, 2000);
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
}
