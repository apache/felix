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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.aspect;
import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectBaseTest extends TestBase {    

	public void testSingleAspect() {
	    DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider("a");
        ServiceConsumer c = new ServiceConsumer(e);
        
        Component sp = component(m).impl(p).provides(ServiceInterface.class).properties(name -> "a").build();
        Component sc = component(m).impl(c).withSvc(ServiceInterface.class, srv -> srv.add("add").remove("remove").autoConfig("m_service")).build();
        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();
            
        m.add(sc);
        m.add(sp);
        // after the provider was added, the consumer's add should have been invoked once
        e.waitForStep(1, 2000);
        Assert.assertEquals("a", c.invoke());
        m.add(sa);
        // after the aspect was added, the consumer should get and add for the aspect and a remove
        // for the original service
        e.waitForStep(3, 2000);
        Assert.assertEquals("aa", c.invoke());
        m.remove(sa);
        // removing the aspect again should give a remove and add
        e.waitForStep(5, 2000);
        Assert.assertEquals("a", c.invoke());
        m.remove(sp);
        // finally removing the original service should give a remove
        e.waitForStep(6, 2000);
        m.remove(sc);
        e.step(7);
        clearComponents();
    }
    
    public void testSingleAspectRef() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider("a");
        ServiceConsumer c = new ServiceConsumer(e);
        
        Component sp = component(m).impl(p).provides(ServiceInterface.class).properties(name -> "a").build();        
        Component sc = component(m)
            .impl(c).withSvc(ServiceInterface.class, srv -> srv.add(c::addRef).remove(c::removeRef).autoConfig("m_service")).build();
        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();
        
        m.add(sc);
        m.add(sp);
        // after the provider was added, the consumer's add should have been invoked once
        e.waitForStep(1, 2000);
        Assert.assertEquals("a", c.invoke());
        m.add(sa);
        // after the aspect was added, the consumer should get and add for the aspect and a remove
        // for the original service
        e.waitForStep(3, 2000);
        Assert.assertEquals("aa", c.invoke());
        m.remove(sa);
        // removing the aspect again should give a remove and add
        e.waitForStep(5, 2000);
        Assert.assertEquals("a", c.invoke());
        m.remove(sp);
        // finally removing the original service should give a remove
        e.waitForStep(6, 2000);
        m.remove(sc);
        e.step(7);
        clearComponents();
    }
    
    public void testSingleAspectThatAlreadyExisted() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider("a");
        ServiceConsumer c = new ServiceConsumer(e);
        Component sp = component(m).impl(p).provides(ServiceInterface.class).properties(name -> "a").build();            
        Component sc = component(m).impl(c).withSvc(ServiceInterface.class, srv -> srv.add("add").remove("remove").autoConfig("m_service")).build();
        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();

        // we first add the aspect
        m.add(sa);
        // then the service provider
        m.add(sp);
        // finally the consumer
        m.add(sc);

        Assert.assertEquals("aa", c.invoke());
        
        // now the consumer's added should be invoked once, as the aspect is already available and should
        // directly hide the original service
        e.waitForStep(1, 2000);
        e.step(2);

        m.remove(sa);
        // after removing the aspect, the consumer should get the original service back, so
        // remove and add will be invoked
        e.waitForStep(4, 2000);
        
        Assert.assertEquals("a", c.invoke());
        
        m.remove(sp);
        // after removing the original service, the consumer's remove should be called once
        e.waitForStep(5, 2000);
        
        m.remove(sc);
        e.step(6);
    }

    public void testSingleAspectThatAlreadyExistedRef() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider("a");
        ServiceConsumer c = new ServiceConsumer(e);
        
        Component sp = component(m).impl(p).provides(ServiceInterface.class).properties(name -> "a").build();
        Component sc = component(m).impl(c).withSvc(ServiceInterface.class, srv -> srv.add(c::addRef).remove(c::removeRef).autoConfig("m_service")).build();
        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();

        // we first add the aspect
        m.add(sa);
        // then the service provider
        m.add(sp);
        // finally the consumer
        m.add(sc);

        Assert.assertEquals("aa", c.invoke());
        
        // now the consumer's added should be invoked once, as the aspect is already available and should
        // directly hide the original service
        e.waitForStep(1, 2000);
        e.step(2);

        m.remove(sa);
        // after removing the aspect, the consumer should get the original service back, so
        // remove and add will be invoked
        e.waitForStep(4, 2000);
        
        Assert.assertEquals("a", c.invoke());
        
        m.remove(sp);
        // after removing the original service, the consumer's remove should be called once
        e.waitForStep(5, 2000);
        
        m.remove(sc);
        e.step(6);
    }

    public void testMultipleAspects() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create service providers and consumers
        ServiceConsumer c = new ServiceConsumer(e);
        Component sp = component(m).impl(new ServiceProvider("a")).provides(ServiceInterface.class).properties(name -> "a").build();
        Component sp2 = component(m).impl(new ServiceProvider("b")).provides(ServiceInterface.class).properties(name -> "b").build();
        Component sc = component(m).impl(c).withSvc(ServiceInterface.class, srv -> srv.add("add").remove("remove")).build();

        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();
        Component sa2 = aspect(m, ServiceInterface.class).rank(10).impl(ServiceAspect.class).build();

        m.add(sp);
        m.add(sp2);
        m.add(sa);
        m.add(sa2);
        m.add(sc);
        // the consumer will monitor progress, it should get it's add invoked twice, once for every
        // (highest) aspect
        e.waitForStep(2, 2000);
        e.step(3);
        
        // now invoke all services the consumer collected
        List<String> list = c.invokeAll();
        // and make sure both of them are correctly invoked
        Assert.assertTrue(list.size() == 2);
        Assert.assertTrue(list.contains("aaa"));
        Assert.assertTrue(list.contains("bbb"));
        
        m.remove(sc);
        // removing the consumer now should get its removed method invoked twice
        e.waitForStep(5, 2000);
        e.step(6);
        m.remove(sa2);
        m.remove(sa);
        m.remove(sp2);
        m.remove(sp);
        e.step(7);
    }
    
    public void testMultipleAspectsRef() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create service providers and consumers
        ServiceConsumer c = new ServiceConsumer(e);
        Component sp = component(m).impl(new ServiceProvider("a")).provides(ServiceInterface.class).properties(name -> "a").build();
        Component sp2 = component(m).impl(new ServiceProvider("b")).provides(ServiceInterface.class).properties(name -> "b").build();
        Component sc = component(m).impl(c).withSvc(ServiceInterface.class, srv -> srv.add(c::addRef).remove(c::removeRef)).build();

        Component sa = aspect(m, ServiceInterface.class).rank(20).impl(ServiceAspect.class).build();
        Component sa2 = aspect(m, ServiceInterface.class).rank(10).impl(ServiceAspect.class).build();

        m.add(sp);
        m.add(sp2);
        m.add(sa);
        m.add(sa2);
        m.add(sc);
        // the consumer will monitor progress, it should get it's add invoked twice, once for every
        // (highest) aspect
        e.waitForStep(2, 2000);
        e.step(3);
        
        // now invoke all services the consumer collected
        List<String> list = c.invokeAll();
        // and make sure both of them are correctly invoked
        Assert.assertTrue(list.size() == 2);
        Assert.assertTrue(list.contains("aaa"));
        Assert.assertTrue(list.contains("bbb"));
        
        m.remove(sc);
        // removing the consumer now should get its removed method invoked twice
        e.waitForStep(5, 2000);
        e.step(6);
        m.remove(sa2);
        m.remove(sa);
        m.remove(sp2);
        m.remove(sp);
        e.step(7);
    }
    
    public static interface ServiceInterface {
        public String invoke(String input);
    }
    
    public static class ServiceProvider implements ServiceInterface {
        private final String m_name;
        public ServiceProvider(String name) {
            m_name = name;
        }
        public String invoke(String input) {
            return input + m_name;
        }
    }
    
    public static class ServiceAspect implements ServiceInterface {
        private volatile ServiceInterface m_originalService;
        private volatile ServiceRegistration<?> m_registration;
        
        public String invoke(String input) {
            String result = m_originalService.invoke(input);
            String property = (String) m_registration.getReference().getProperty("name");
            return result + property;
        }
    }
    
    public static class ServiceConsumer {
        private final Ensure m_ensure;
        private volatile ServiceInterface m_service;
        private List<ServiceInterface> m_services = new ArrayList<ServiceInterface>();

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void addRef(ServiceInterface si, ServiceReference<ServiceInterface> ref) { // method ref callback
            add(ref, si);
        }
        
        public void add(ServiceReference<ServiceInterface> ref, ServiceInterface si) { // reflection callback
            System.out.println("add: " + ServiceUtil.toString(ref));
            m_services.add(si);
            m_ensure.step();
        }
        
        public void removeRef(ServiceInterface si, ServiceReference<ServiceInterface> ref) { // method ref callback
            remove(ref, si);
        }
        
        public void remove(ServiceReference<ServiceInterface> ref, ServiceInterface si) { // reflection callback
            System.out.println("rem: " + ServiceUtil.toString(ref));
            m_services.remove(si);
            m_ensure.step();
        }
        
        public String invoke() {
            return m_service.invoke("");
        }
        
        public List<String> invokeAll() {
            List<String> results = new ArrayList<String>();
            for (ServiceInterface si : m_services) {
                results.add(si.invoke(""));
            }
            return results;
        }
    }
}
