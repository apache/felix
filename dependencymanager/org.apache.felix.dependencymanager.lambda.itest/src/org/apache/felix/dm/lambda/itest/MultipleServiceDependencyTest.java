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
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MultipleServiceDependencyTest extends TestBase {
   public void testMultipleServiceRegistrationAndConsumption() {
       DependencyManager m = getDM();
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Component provider = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
       Component providerWithHighRank = component(m).impl(new ServiceProvider2(e)).provides(ServiceInterface.class.getName(), Constants.SERVICE_RANKING, Integer.valueOf(5)).build();
       Component consumer = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, true).build();
       m.add(provider);
       m.add(providerWithHighRank);
       m.add(consumer);
       e.waitForStep(3, 5000);
       m.remove(providerWithHighRank);
       e.step(4);
       e.waitForStep(5, 5000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 5000);
   }

   public void testReplacementAutoConfig() {
       DependencyManager m = getDM();
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Component provider = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
       Component provider2 = component(m).impl(new ServiceProvider2(e)).provides(ServiceInterface.class.getName()).build();
       Component consumer = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, true).build();
       m.add(provider2);
       m.add(consumer);
       e.waitForStep(3, 5000);
       m.add(provider);
       m.remove(provider2);
       e.step(4);
       e.waitForStep(5, 5000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 5000);
   }

   public void testReplacementCallbacks() {
       DependencyManager m = getDM();
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Component provider = component(m).impl(new ServiceProvider(e)).provides(ServiceInterface.class.getName()).build();
       Component provider2 = component(m).impl(new ServiceProvider2(e)).provides(ServiceInterface.class.getName()).build();
       Component consumer = component(m).impl(new ServiceConsumer(e)).withSvc(ServiceInterface.class, srv->srv.add("add").remove("remove")).build();
       m.add(provider2);
       m.add(consumer);
       e.waitForStep(3, 15000);
       m.add(provider);
       m.remove(provider2);
       e.step(4);
       e.waitForStep(5, 15000);
       m.remove(provider);
       m.remove(consumer);
       e.waitForStep(6, 15000);
   }

   static interface ServiceInterface {
       public void invoke();
   }

   static class ServiceProvider implements ServiceInterface {
       private final Ensure m_ensure;
       public ServiceProvider(Ensure e) {
           m_ensure = e;
       }
       public void invoke() {
           m_ensure.step(5);
       }
   }

   static class ServiceProvider2 implements ServiceInterface {
       private final Ensure m_ensure;
       public ServiceProvider2(Ensure e) {
           m_ensure = e;
       }
       public void invoke() {
           m_ensure.step(2);
       }
   }

   static class ServiceConsumer implements Runnable {
       private volatile ServiceInterface m_service;
       private final Ensure m_ensure;

       @SuppressWarnings("unused")
       private void add(ServiceInterface service) { m_service = service; }
       
       @SuppressWarnings("unused")
       private void remove(ServiceInterface service) { if (m_service == service) { m_service = null; }}
       public ServiceConsumer(Ensure e) { m_ensure = e; }

       public void start() {
           Thread t = new Thread(this);
           t.start();
       }

       public void run() {
           m_ensure.step(1);
           m_service.invoke();
           m_ensure.step(3);
           m_ensure.waitForStep(4, 15000);
           m_service.invoke();
       }

       public void stop() {
           m_ensure.step(6);
       }
   }
}
