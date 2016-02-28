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

import java.util.concurrent.CompletableFuture;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * CompletableFuture Dependency test.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CFDependencyTest extends TestBase {
    private final Ensure m_ensure = new Ensure();
    
    public void testComponentWithCFDependencyDefinedFromInit() throws Exception {
        final DependencyManager dm = getDM();

        // Create a consumer depending on a Provider and on the result of a CF.
        // (the CF dependency is added from the Consumer.init() method).
        CompletableFuture<String> cf = new CompletableFuture<>();
        Component consumer = component(dm).impl(new Consumer(cf)).withSvc(Provider.class, true).build();
                        
        // Create provider
        Component provider = component(dm).impl(new ProviderImpl()).provides(Provider.class).build();
        
        dm.add(consumer);
        dm.add(provider);
        
        // Sets the result in the CF: this will normally trigger the activation of the Consumer component.
        cf.complete("cfFesult");
            
        m_ensure.waitForStep(3, 5000);
        dm.clear();
    }
    
    public static interface Provider {
        void invoke();
    }
    
    public class ProviderImpl implements Provider {
        @Override
        public void invoke() {
            m_ensure.step(3);
        }
    }
    
    public class Consumer {
        private Provider m_provider; // injected
        private String m_cfResult; // injected
        private final CompletableFuture<String> m_cf;
        
        Consumer(CompletableFuture<String> cf) {
            m_cf = cf;
        }

        void init(Component c) {
            component(c, comp -> comp.withFuture(m_cf, result -> result.complete(this::add)));
        }
        
		void add(String cfResult) {
		    m_cfResult = cfResult;
            Assert.assertNotNull(m_cfResult);
            Assert.assertEquals("cfFesult", m_cfResult);
            m_ensure.step(1);
        }
        
        void start() {
            Assert.assertNotNull(m_provider);
            m_ensure.step(2);
            m_provider.invoke();
        }
    }
}
