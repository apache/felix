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
package org.apache.felix.dm.itest.api;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.ServiceUtil;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class AspectBaseTest extends TestBase {    

	public void testSingleAspect() {
	    DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider(e, "a");
        ServiceConsumer c = new ServiceConsumer(e);
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("name", "a");
        Component sp = m.createComponent()
            .setInterface(ServiceInterface.class.getName(), props)
            .setImplementation(p);
        Component sc = m.createComponent()
            .setImplementation(c)
            .add(m.createServiceDependency()
                .setService(ServiceInterface.class)
                .setRequired(true)
                .setCallbacks("add", "remove")
                .setAutoConfig("m_service")
            );
        Component sa = m.createAspectService(ServiceInterface.class, null, 20, null)
            .setImplementation(ServiceAspect.class);
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
    }
    
    @SuppressWarnings("serial")
    public void testSingleAspectThatAlreadyExisted() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create a service provider and consumer
        ServiceProvider p = new ServiceProvider(e, "a");
        ServiceConsumer c = new ServiceConsumer(e);
        Component sp = m.createComponent().setImplementation(p).setInterface(ServiceInterface.class.getName(), new Hashtable() {{ put("name", "a"); }});
        Component sc = m.createComponent().setImplementation(c).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true).setCallbacks("add", "remove").setAutoConfig("m_service"));
        Component sa = m.createAspectService(ServiceInterface.class, null, 20, null).setImplementation(ServiceAspect.class);
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

    @SuppressWarnings("serial")
    public void testMultipleAspects() {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create service providers and consumers
        ServiceConsumer c = new ServiceConsumer(e);
        Component sp = m.createComponent().setImplementation(new ServiceProvider(e, "a")).setInterface(ServiceInterface.class.getName(), new Hashtable() {{ put("name", "a"); }});
        Component sp2 = m.createComponent().setImplementation(new ServiceProvider(e, "b")).setInterface(ServiceInterface.class.getName(), new Hashtable() {{ put("name", "b"); }});
        Component sc = m.createComponent().setImplementation(c).add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true).setCallbacks("add", "remove"));
        Component sa = m.createAspectService(ServiceInterface.class, null, 20, null).setImplementation(ServiceAspect.class);
        Component sa2 = m.createAspectService(ServiceInterface.class, null, 10, null).setImplementation(ServiceAspect.class);
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
        private final Ensure m_ensure;
        private final String m_name;
        public ServiceProvider(Ensure e, String name) {
            m_ensure = e;
            m_name = name;
        }
        public String invoke(String input) {
            return input + m_name;
        }
    }
    
    public static class ServiceAspect implements ServiceInterface {
        private volatile ServiceInterface m_originalService;
        private volatile ServiceRegistration m_registration;
        
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
        
        public void add(ServiceReference ref, ServiceInterface si) {
            System.out.println("add: " + ServiceUtil.toString(ref));
            m_services.add(si);
            m_ensure.step();
        }
        
        public void remove(ServiceReference ref, ServiceInterface si) {
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
