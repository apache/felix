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

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AbstractServiceDependencyTest extends TestBase {
   public void testAbstractClassDependency() {
       DependencyManager m = getDM();
       // helper class that ensures certain steps get executed in sequence
       Ensure e = new Ensure();
       // create a service provider and consumer
       Component sp = m.createComponent()
           .setInterface(ServiceAbstract.class.getName(), null)
           .setImplementation(new ServiceProvider(e))
           ;
       Component sc = m.createComponent()
           .setImplementation(new ServiceConsumer(e))
           .add(m.createServiceDependency()
               .setService(ServiceAbstract.class)
               .setRequired(true)
               .setCallbacks("bind", "unbind")
               );
       m.add(sp);
       m.add(sc);
       m.remove(sp);
       // ensure we executed all steps inside the component instance
       e.step(8);
       m.clear();
   }

   static abstract class ServiceAbstract {
       public abstract void invoke();
   }

   static class ServiceProvider extends ServiceAbstract {
       private final Ensure m_ensure;
       public ServiceProvider(Ensure e) {
           m_ensure = e;
       }

       public void start() {
           m_ensure.step(1);
       }

       public void invoke() {
           m_ensure.step(4);
       }

       public void stop() {
           m_ensure.step(7);
       }
   }

   static class ServiceConsumer {
       private volatile ServiceAbstract m_service;
       private final Ensure m_ensure;

       public ServiceConsumer(Ensure e) {
           m_ensure = e;
       }

       public void bind(ServiceAbstract service) {
           m_ensure.step(2);
           m_service = service;
       }

       public void start() {
           m_ensure.step(3);
           m_service.invoke();
       }

       public void stop() {
           m_ensure.step(5);
       }

       public void unbind(ServiceAbstract service) {
           System.out.println("UNBINDDDDDDDDDDDDDDDDDDDDDDDDDDD");
           Assert.assertEquals(m_service, service);
           m_ensure.step(6);
       }
   }
}