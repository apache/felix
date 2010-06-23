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
package org.apache.felix.dm.test.bundle.annotation.aspectlifecycledynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@AspectService(ranking = 10, service=ServiceInterface.class, factoryMethod="create")
public class ServiceProviderAspect implements InvocationHandler
{
    protected boolean m_initCalled;
    protected Sequencer m_sequencer;

    @ServiceDependency(filter = "(test=aspectLifecycle.ServiceProviderAspect)")
    protected void bind(Sequencer sequencer)
    {
        m_sequencer = sequencer;
        m_sequencer.step(2);
    }
    
    static Object create() {
        return Proxy.newProxyInstance(ServiceProviderAspect.class.getClassLoader(),
                                      new Class[] { ServiceInterface.class },
                                      new ServiceProviderAspect());
    }

    @Init
    void init()
    {
        m_initCalled = true;
    }

    @Start
    void start()
    {
        if (!m_initCalled)
        {
            throw new IllegalStateException("start method called, but init method was not called");
        }
        m_sequencer.step(3);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("toString"))
        {
            return "ServiceProviderAspect@" + System.identityHashCode(this);
        }
        
        if (! method.getName().equals("run"))
        {
            throw new IllegalStateException("wrong invoked method: " + method);
        }
        m_sequencer.step(4);
        return null;
    }

    @Stop()
    void stop()
    {
        // At this point, the AspectLifecycleTest class has been rebound to the original ServiceProvider.
        m_sequencer.step(6);
    }

    @Destroy
    void destroy()
    {
        m_sequencer.step(7);
    }
}
