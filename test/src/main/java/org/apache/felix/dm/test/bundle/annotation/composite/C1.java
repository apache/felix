/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test.bundle.annotation.composite;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.ServiceRegistration;

/**
 * This service is also composed of the Component object.
 */
@Component
public class C1 implements C1Service
{
    /* We are composed of this object, which will also be injected with our dependencies */
    private C2 m_c2 = new C2();

    /* This dependency filter will be configured from our init method */
    @ServiceDependency(name = "D")
    public volatile Runnable m_runnable;
    
    /* Object used to check that methods are called in the proper sequence */
    @ServiceDependency(filter="(name=C1)")
    private volatile Sequencer m_sequencer;

    /**
     *  Dynamically configure our "D" dependency, using a dependency customization map 
     */
    @Init
    Map<String, String> init()
    {
        m_sequencer.step(1);
        // Configure a filter for our dependency whose name is "D"
        Map<String, String> customization = new HashMap<String, String>();
        customization.put("D.filter", "(foo=bar2)");
        customization.put("D.required", "true");
        return customization;
    }

    /**
     * Return the list of object our service is composed of
     */
    @Composition
    Object[] getComposition()
    {
        return new Object[] { this, m_c2 };
    }

    /** 
     * Our Service is starting, and our Composites will also be 
     */
    @Start
    void start()
    {
        System.out.println("start: m_runnable=" + m_runnable);
        m_sequencer.step(3);
        m_runnable.run(); /* step 4 */
        // Our Component.start() method should be called once this method returns.
    }

    /**
     * Our provided service has been registered into the OSGi service registry.
     */
    @Registered
    void registered(ServiceRegistration sr)
    {
        m_sequencer.step(7);
    }
    
    /**
     *  Our Service is stopping, and our Composites will also be 
     */
    @Stop
    void stop()
    {
        m_sequencer.step(9);
        // Our Component.stop() method should be called once this method returns.
    }

    /**
     * Our Service is destroying, and our Composites will also be.
     */
    @Destroy
    void destroy()
    {
        m_sequencer.step(11);
        // Our Component.destroy() method should be called once this method returns.
    }
}
