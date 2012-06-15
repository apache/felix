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
package org.apache.felix.dm.test.bundle.annotation.simple;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.Unregistered;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.ServiceRegistration;

/**
 * Provides a <code>Runnable</code> service, which is required by the {@link Consumer} class.
 */
@Component(properties={@Property(name="foo", value="bar")})
public class Producer implements Runnable
{
    @ServiceDependency
    volatile Sequencer m_sequencer;

    @Init
    protected void init()
    {
        // Our component is initializing (at this point: all required dependencies are injected).
        m_sequencer.step(1);  
    }
    
    @Start
    protected void start()
    {
        // We are about to be registered in the OSGi registry.
        m_sequencer.step(2);
    }

    public void run()
    {
        // the Consumer has been injected with our service, and is invoking our run() method.
        m_sequencer.step(4);
    }

    @Registered
    protected void started(ServiceRegistration sr)
    {
        // We are registered in the OSGi registry
        if (sr == null)
        {
            m_sequencer.throwable(new Exception("ServiceRegistration is null"));
        }
        if (!"bar".equals(sr.getReference().getProperty("foo")))
        {
            m_sequencer.throwable(new Exception("Invalid Service Properties"));
        }
        m_sequencer.step(5);
    }

    @Stop
    protected void stop()
    {
        // We are about to be unregistered from the OSGi registry, and we must stop.
        m_sequencer.step(7);
    }

    @Unregistered
    protected void stopped()
    {
        // We are unregistered from the OSGi registry.
        m_sequencer.step(8);
    }    
    
    @Destroy
    public void destroy()
    {
        // Our component is shutting down.
        m_sequencer.step(9); 
    }
}
