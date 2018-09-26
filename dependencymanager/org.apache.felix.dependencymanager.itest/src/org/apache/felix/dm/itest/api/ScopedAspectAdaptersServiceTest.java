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

import java.util.Dictionary;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component.ServiceScope;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.service.cm.ConfigurationAdmin;

public class ScopedAspectAdaptersServiceTest extends TestBase {
    final Ensure m_e = new Ensure();
        
    public void testPrototypeAspect() {
        DependencyManager m = getDM();        
        Component provider = m.createComponent()
            .setFactory(this, "createServiceImpl")
            .setInterface(Service.class.getName(), null);
        
        Component consumer1 = m.createComponent()
            .setFactory(this, "createServiceConsumer")
            .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("bind", null));
        
        Component consumer2 = m.createComponent()
            .setFactory(this, "createServiceConsumer")
            .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("bind", null));
        
        Component aspect = m.createAspectComponent()
        	.setAspect(Service.class, null, 10)
        	.setScope(ServiceScope.PROTOTYPE)
            .setFactory(this, "createAspectService")
            .setCallbacks(null, null, null, null);
        
        m.add(provider);
        m_e.waitForStep(1, 5000); // provider started        

        m.add(aspect);

        m.add(consumer1);
        m_e.waitForStep(3, 5000); // consumer1 is bound to a clone of the aspect

        m.add(consumer2);  
        m_e.waitForStep(5, 5000); // consumer2 is bound to a clone of the aspect
        
        ServiceConsumer consumer1Impl = (ServiceConsumer) consumer1.getInstance();
        Assert.assertNotNull(consumer1Impl.getService());
        ServiceConsumer consumer2Impl = (ServiceConsumer) consumer2.getInstance();
        Assert.assertNotNull(consumer2Impl.getService());
        Assert.assertNotEquals(consumer1Impl.getService(), consumer2Impl.getService());
        Assert.assertEquals(consumer1Impl.getService().getClass(), AspectService.class);
        m.clear();        
    }
    
    public void testPrototypeAdapter() {
        DependencyManager m = getDM();        
        Component provider = m.createComponent()
            .setFactory(this, "createServiceImpl")
            .setInterface(Service.class.getName(), null);
        
        Component consumer1 = m.createComponent()
            .setFactory(this, "createAdapterServiceConsumer")
            .add(m.createServiceDependency().setService(AdapterService.class).setRequired(true).setCallbacks("bind", null));
        
        Component consumer2 = m.createComponent()
            .setFactory(this, "createAdapterServiceConsumer")
            .add(m.createServiceDependency().setService(AdapterService.class).setRequired(true).setCallbacks("bind", null));

        Component adapter = m.createAdapterComponent()
        	.setAdaptee(Service.class, null)
        	.setPropagate(false)
        	.setScope(ServiceScope.PROTOTYPE)
            .setInterface(AdapterService.class.getName(), null)
            .setFactory(this, "createAdapterServiceImpl");
        
        m.add(provider);
        m_e.waitForStep(1, 5000); // provider started        

        m.add(adapter);

        m.add(consumer1);
        m_e.waitForStep(3, 5000); // consumer1 is bound to a clone of the adapter

        m.add(consumer2);  
        m_e.waitForStep(5, 5000); // consumer2 is bound to a clone of the adapter
        
        AdapterServiceConsumer consumer1Impl = (AdapterServiceConsumer) consumer1.getInstance();
        Assert.assertNotNull(consumer1Impl.getService());
        AdapterServiceConsumer consumer2Impl = (AdapterServiceConsumer) consumer2.getInstance();
        Assert.assertNotNull(consumer2Impl.getService());
        Assert.assertNotEquals(consumer1Impl.getService(), consumer2Impl.getService());
        m.clear();        
    }
    
    /**
     * Test for a factory configuration adapter: a component is created when its configuration pid is created,
     * and when two consumers declares a dependendency on the component, then both consumers will get their own instance
     * of the component.
     */
    public void testPrototypeFactoryAdapter() {
        DependencyManager m = getDM();      
        
        // Create a Configuration 
        Component configurator = m.createComponent()
            .setImplementation(new FactoryConfigurationCreator(m_e, "prototype.factory", 1, "key", "value"))
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));        

        // Create a config adapter component
        Component provider = m.createFactoryComponent()
        	.setFactoryPid("prototype.factory")
        	.setUpdated("update")
        	.setScope(ServiceScope.PROTOTYPE)
        	.setFactory(this, "createServiceImpl")
            .setInterface(Service.class.getName(), null);
        
        Component consumer1 = m.createComponent()
            .setFactory(this, "createServiceConsumer")
            .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("bind", null));
        
        Component consumer2 = m.createComponent()
            .setFactory(this, "createServiceConsumer")
            .add(m.createServiceDependency().setService(Service.class).setRequired(true).setCallbacks("bind", null));
                
        m.add(configurator);
        m.add(provider);
        
        m.add(consumer1); // add first consumer
        m_e.waitForStep(2, 5000); // provider prototype started        
        m_e.waitForStep(3, 5000); // the first consumer is injected with the prototype instance
        
        m.add(consumer2); // add second consumer
        m_e.waitForStep(5, 5000); // a clone of the provider is instantiated and the consumer1 is bound with it.
        
        // make sure both consumers have a different provider instances.
        ServiceConsumer consumer1Impl = (ServiceConsumer) consumer1.getInstance();
        Assert.assertNotNull(consumer1Impl.getService());
        ServiceConsumer consumer2Impl = (ServiceConsumer) consumer2.getInstance();
        Assert.assertNotNull(consumer2Impl.getService());
        Assert.assertNotEquals(consumer1Impl.getService(), consumer2Impl.getService());
        
        m.clear();        
    }
    
    @SuppressWarnings("unused")
    private ServiceImpl createServiceImpl() { 
        return new ServiceImpl();
    }
    
    @SuppressWarnings("unused")
    private AdapterServiceImpl createAdapterServiceImpl() {
        return new AdapterServiceImpl();
    }
    
    @SuppressWarnings("unused")
    private AdapterServiceConsumer createAdapterServiceConsumer() {
        return new AdapterServiceConsumer();
    }
    
    @SuppressWarnings("unused")
    private AspectService createAspectService() {
        return new AspectService();
    }
    
    @SuppressWarnings("unused")
    private ServiceConsumer createServiceConsumer() {
        return new ServiceConsumer();
    }

    
    public interface Service { 
    }
        
    public class ServiceImpl implements Service {
        void update(Dictionary<String, Object> conf) {      
        }
        
        void start() {
            m_e.step();
        }
    }
    
    public class AspectService implements Service {
        public AspectService() {
            m_e.step();
        }
    }
    
    public class ServiceConsumer {
        volatile Service m_myService;
        
        public void bind(Service service) {
            m_myService = service;
            m_e.step();
        }
        
        public Service getService() {
            return m_myService;
        }
    }
    
    public interface AdapterService {
        
    }
    
    public class AdapterServiceImpl implements AdapterService {
        Service m_service;
        
        public AdapterServiceImpl() {
        }
        
        void start() {
            Assert.assertNotNull(m_service);
            m_e.step();
        }
    }
    
    public class AdapterServiceConsumer {
        volatile AdapterService m_service;
        
        public void bind(AdapterService service) {
            m_service = service;
            m_e.step();
        }
        
        public AdapterService getService() {
            return m_service;
        }
    }
}
