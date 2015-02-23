/**
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"rawtypes"})
public class DynamicProxyAspectTest extends TestBase {
    public void testImplementGenericAspectWithDynamicProxy() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        // create two service providers, each providing a different service interface
        Component sp1 = m.createComponent().setImplementation(new ServiceProvider(e)).setInterface(ServiceInterface.class.getName(), null);
        Component sp2 = m.createComponent().setImplementation(new ServiceProvider2(e)).setInterface(ServiceInterface2.class.getName(), null);
        
        // create a dynamic proxy based aspect and hook it up to both services
        Component a1 = m.createAspectService(ServiceInterface.class, null, 10, "m_service")
            .setFactory(new Factory(e, ServiceInterface.class, "ServiceInterfaceProxy"), "create");
        Component a2 = m.createAspectService(ServiceInterface2.class, null, 10, "m_service")
            .setFactory(new Factory(e, ServiceInterface2.class, "ServiceInterfaceProxy2"), "create");

        // create a client that invokes a method on boths services, validate that it goes
        // through the proxy twice
        Component sc = m.createComponent()
            .setImplementation(new ServiceConsumer(e))
            .add(m.createServiceDependency().setService(ServiceInterface.class).setRequired(true))
            .add(m.createServiceDependency().setService(ServiceInterface2.class).setRequired(true))
            ;
        
        // register both producers, validate that both services are started
        m.add(sp1);
        e.waitForStep(1, 2000);
        m.add(sp2);
        e.waitForStep(2, 2000);
        
        // add both aspects, and validate that both instances have been created
        m.add(a1);
        m.add(a2);
        e.waitForStep(4, 4000);
        
        // add the client, which will automatically invoke both services
        m.add(sc);
        
        // wait until both services have been invoked
        e.waitForStep(6, 4000);
        
        // make sure the proxy has been called twice
        Assert.assertEquals("Proxy should have been invoked this many times.", 2, DynamicProxyHandler.getCounter());
        
        m.remove(sc);
        m.remove(a2);
        m.remove(a1);
        m.remove(sp2);
        m.remove(sp1);
        m.remove(a2);
        m.remove(a1);
        
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    
    static interface ServiceInterface {
        public void invoke(Runnable run);
    }
    
    static interface ServiceInterface2 {
        public void invoke(Runnable run);
    }
    
    static class ServiceProvider implements ServiceInterface {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        public void start() {
            m_ensure.step(1);
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceProvider2 implements ServiceInterface2 {
        private final Ensure m_ensure;
        public ServiceProvider2(Ensure ensure) {
            m_ensure = ensure;
        }
        public void start() {
            m_ensure.step(2);
        }
        public void invoke(Runnable run) {
            run.run();
        }
    }
    
    static class ServiceConsumer implements Runnable {
        private volatile ServiceInterface m_service;
        private volatile ServiceInterface2 m_service2;
        private final Ensure m_ensure;

        public ServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            Thread t = new Thread(this);
            t.start();
        }
        
        public void run() {
            m_service.invoke(Ensure.createRunnableStep(m_ensure, 5));
            m_service2.invoke(Ensure.createRunnableStep(m_ensure, 6));
        }
    }
    
    static class DynamicProxyHandler implements InvocationHandler {
        public volatile Object m_service; // ISSUE, we cannot inject into "Object" at the moment
        private final String m_label;
        private static volatile int m_counter = 0;

        public DynamicProxyHandler(String label) {
            m_label = label;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("IIIIIIINVOKE--------------------------" + method.getName());
            if (m_service == null) {
                Assert.fail("No service was injected into dynamic proxy handler " + m_label);
            }
            Method m = m_service.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (m == null) {
                Assert.fail("No method " + method.getName() + " was found in instance " + m_service + " in dynamic proxy handler " + m_label);
            }
            if (method.getName().equals("invoke")) {
                // only count methods called 'invoke' because those are actually the ones
                // both interfaces implement (and the dynamic proxy might be invoked for
                // other methods, such as toString() as well)
                m_counter++;
            }
            return m.invoke(m_service, args);
        }
        
        public static int getCounter() {
            return m_counter;
        }
    }
    
    static class Factory {
        private final String m_label;
        private Class m_class;
        private final Ensure m_ensure;
        
        public Factory(Ensure ensure, Class clazz, String label) {
            m_ensure = ensure;
            m_class = clazz;
            m_label = label;
        }
        
        public Object create() {
            m_ensure.step();
            return Proxy.newProxyInstance(m_class.getClassLoader(), new Class[] { m_class }, new DynamicProxyHandler(m_label));
        }
    }
}
