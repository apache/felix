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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX4614_FactoryWithComponentInCreateParam extends TestBase {
    private final static Ensure m_ensure = new Ensure();

    public void testSimpleFactory() {
        DependencyManager manager = getDM();
        
        Component service = manager.createComponent()
            .setFactory(Factory.class, "create")
            .setInterface(Service.class.getName(), null);
        
        Component client = manager.createComponent()            
            .setImplementation(Client.class)
            .add(manager.createServiceDependency().setService(Service.class).setRequired(true));
        
        manager.add(client);
        manager.add(service);
        m_ensure.waitForStep(3, 5000);        
        manager.clear();        
    }
    
    public static class Factory {        
        public Object create(Component c) {
            m_ensure.step(1);
            Assert.assertNotNull(c);
            m_ensure.step(2);
            return new ServiceImpl();
        }
    }
    
    public static interface Service {        
    }
    
    public static class ServiceImpl implements Service {        
    }
    
    public static class Client {
        volatile Service m_service;
        
        void start() {
            Assert.assertNotNull(m_service);   
            m_ensure.step(3);
        }
    }
}

