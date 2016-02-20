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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;

/**
 * Test which validates multi-dependencies combination.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MultipleExtraDependencyTest extends TestBase {
    public void testMultipleExtraDependencies()
    {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        Component sp2 = component(m)
              .impl(ServiceProvider2.class).provides(ServiceProvider2.class)
              .withSvc(Runnable.class, srv->srv.filter("(foo=bar)").required(false).autoConfig("m_runnable"))
              .withSvc(Sequencer.class, srv->srv.add("bind"))
              .composition("getComposition")
              .build();

        Component sp = component(m)
              .impl(ServiceProvider.class)
              .provides(ServiceInterface.class, foo -> "bar")
              .start("start").stop("stop")
              .withSvc(Sequencer.class, srv->srv.autoConfig("m_sequencer"))
              .withSvc(ServiceProvider2.class, srv->srv.add("bind").remove("unbind"))
              .build();
        
        Component sc = component(m)
              .impl(ServiceConsumer.class)
              .start("start").stop("stop")
              .withSvc(Sequencer.class, srv->srv.autoConfig("m_sequencer"))
              .withSvc(ServiceInterface.class, srv->srv.filter("(foo=bar)").autoConfig("m_service"))
              .build();
        
        Component sequencer = component(m)
           .impl(new SequencerImpl(e))
           .provides(Sequencer.class.getName())
           .build();
           
        m.add(sp2);
        m.add(sp);
        m.add(sc);
        m.add(sequencer);
        
        // Check if ServiceProvider component have been initialized orderly
        e.waitForStep(7, 5000);
        
        // Stop the test.annotation bundle
        m.remove(sequencer);
        m.remove(sp);
        m.remove(sp2);
        m.remove(sc);
        
        // And check if ServiceProvider2 has been deactivated orderly
        e.waitForStep(11, 5000);
    }
    
    public interface Sequencer
    {
        void step();
        void step(int step);
        void waitForStep(int step, int timeout);
    }
    
    public static class SequencerImpl implements Sequencer {
        Ensure m_ensure;
        
        public SequencerImpl(Ensure e)
        {
            m_ensure = e;
        }
        
        public void step()
        {
            m_ensure.step();
        }

        public void step(int step)
        {
            m_ensure.step(step);
        }

        public void waitForStep(int step, int timeout)
        {
            m_ensure.waitForStep(step, timeout);
        }  
    }
    
    public interface ServiceInterface
    {
        public void doService();
    }
    
    public static class ServiceConsumer
    {
        volatile Sequencer m_sequencer;
        volatile ServiceInterface m_service;

        void start()
        {
            m_sequencer.step(6);
            m_service.doService();
        }

        void stop()
        {
            m_sequencer.step(8);
        }
    }
    
    public static class ServiceProvider implements ServiceInterface
    {
        Sequencer m_sequencer;
        ServiceProvider2 m_serviceProvider2;

        void bind(ServiceProvider2 provider2)
        {
            m_serviceProvider2 = provider2;
        }

        void start()
        {
            m_serviceProvider2.step(4);
            m_sequencer.step(5);
        }

        void stop()
        {
            m_sequencer.step(9);
        }

        void unbind(ServiceProvider2 provider2)
        {
            m_sequencer.step(10);
        }

        public void doService()
        {
            m_sequencer.step(7);
        }
    }

    public static class ServiceProvider2
    {
        Composite m_composite = new Composite();
        Sequencer m_sequencer;
        Runnable m_runnable;

        void bind(Sequencer seq)
        {
            m_sequencer = seq;
            m_sequencer.step(1);
        }

        void start()
        {
            m_sequencer.step(3);
            m_runnable.run(); // NullObject
        }

        public void step(int step) // called by ServiceProvider.start() method 
        { 
            m_sequencer.step(step);
        }
        
        void stop()
        {
            m_sequencer.step(11);
        }

        Object[] getComposition()
        {
            return new Object[] { this, m_composite };
        }
    }
    
    public static class Composite
    {
        void bind(Sequencer seq)
        {
            seq.step(2);
        }
    }
}
