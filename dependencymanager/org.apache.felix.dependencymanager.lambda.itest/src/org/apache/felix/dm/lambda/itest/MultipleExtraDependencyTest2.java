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
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;

/**
 * Tests for extra dependencies which are declared from service's init method.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MultipleExtraDependencyTest2 extends TestBase {
    public void testMultipleExtraDependencies() {
        DependencyManager m = getDM();
        Ensure e = new Ensure();
        
        Component sp2 = component(m)
            .impl(ServiceProvider2.class)
            .provides(ServiceProvider2.class)
            .init("init").start("start").stop("stop")
            .composition("getComposition").build();
        
        Component sp = component(m)
            .impl(ServiceProvider.class)
            .provides(ServiceInterface.class, "foo", "bar")                            
            .init("init").start("start").stop("stop")
            .build();
        
        Component sc = component(m)
            .impl(ServiceConsumer.class)
            .init("init").start("start").stop("stop")
            .build();
        
        // Provide the Sequencer service to the MultipleAnnotationsTest class.
        Component sequencer =  component(m)
            .impl(new SequencerImpl(e))
            .provides(Sequencer.class)
            .build();
        
        m.add(sp2);
        m.add(sp);
        m.add(sc);
        m.add(sequencer);

        // Check if the test.annotation components have been initialized orderly
        e.waitForStep(7, 10000);
        
        // Stop the test.annotation bundle
        m.remove(sequencer);
        m.remove(sp);
        m.remove(sp2);
        m.remove(sc);
        
//        m.remove(sp2);
//        m.remove(sc);
//        m.remove(sp);
//        m.remove(sequencer);
        

        
        // And check if the test.annotation bundle has been deactivated orderly
        e.waitForStep(11, 10000);
        m.clear();
    }
    
    public interface Sequencer
    {
        void step();
        void step(int step);
        void waitForStep(int step, int timeout);
    }
    
    public static class SequencerImpl implements Sequencer {
        final Ensure m_ensure;
        
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
    
    public static class ServiceConsumer {
        volatile Sequencer m_sequencer;
        volatile ServiceInterface m_service;
        volatile Dependency m_d1, m_d2;

        public void init(Component s) {
            component(s, comp->comp
                .withSvc(Sequencer.class, srv->srv.autoConfig("m_sequencer"))
                .withSvc(ServiceInterface.class, srv->srv.filter("(foo=bar)").autoConfig("m_service")));
        }
        
        void start() {
            m_sequencer.step(6);
            m_service.doService();
        }

        void stop() {
            m_sequencer.step(8);
        }
    }
    
    public static class ServiceProvider implements ServiceInterface
    {
        volatile Sequencer m_sequencer;
        volatile ServiceProvider2 m_serviceProvider2;
        volatile ServiceDependency m_d1, m_d2;

        public void init(Component c)
        {
            component(c, comp->comp
                .withSvc(Sequencer.class, srv->srv.autoConfig("m_sequencer"))
                .withSvc(ServiceProvider2.class, srv->srv.add("bind").remove("unbind")));
        }
        
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
        final Composite m_composite = new Composite();
        volatile Sequencer m_sequencer;
        volatile Runnable m_runnable;
        volatile ServiceDependency m_d1, m_d2;

        public void init(Component c)
        {
            component(c, comp->comp
		      .withSvc(Runnable.class, srv->srv.optional().filter("(foo=bar)"))
		      .withSvc(Sequencer.class, srv->srv.add("bind")));
        }
        
        void bind(Sequencer seq)
        {
            System.out.println("ServiceProvider2.bind(" + seq + ")");
            m_sequencer = seq;
            m_sequencer.step(1);
        }

        void start()
        {
            System.out.println("ServiceProvider2.start: m_runnable=" + m_runnable + ", m_sequencer = " + m_sequencer);
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
