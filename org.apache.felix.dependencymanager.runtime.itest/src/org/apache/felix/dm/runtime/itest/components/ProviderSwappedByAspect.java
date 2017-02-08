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
package org.apache.felix.dm.runtime.itest.components;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;

/**
 * Check if a client already bound to a service provider is called in swap method
 * when an aspect service replaces the original service.
 */
public class ProviderSwappedByAspect {
	
    public interface ProviderService {
        public void run();
    }

    /**
     * Tests an aspect service, and ensure that its lifecycle methods are properly invoked (init/start/stop/destroy)
     */
    @Component
    public static class Consumer {
        public final static String ENSURE = "ProviderSwappedByAspect.Consumer";
        final static Ensure.Steps m_steps = new Ensure.Steps(
        		1, // provider bound the first time in bind method
        		3, // swap called (aspect is replacing provider)
        		6, // aspect removed , so swap called again with original service
        		8  // provider removed
        		);

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        @ServiceDependency(removed="unbind", swap="swap")
        void bind(ProviderService service) {
        	m_sequencer.steps(m_steps); 
            service.run(); 
        }
        
        void swap(ProviderService old, ProviderService replace) {
        	System.out.println("swapped: old=" + old + ", replace=" + replace);
        	m_sequencer.steps(m_steps); 
        	replace.run(); 
        }
        
        void unbind(ProviderService service) {
        	m_sequencer.steps(m_steps);
        	service.run(); 
        }
    }

    @Component
    public static class Provider implements ProviderService {
        public final static String ENSURE = "ProviderSwappedByAspect.Provider";
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        public void run() {
            m_sequencer.step();
        }
    }

    @AspectService(ranking = 10, added="bind")
    public static class ProviderAspect implements ProviderService {
        public final static String ENSURE = "ProviderSwappedByAspect.ProviderAspect";
		private ProviderService m_next;
        
        void bind(ProviderService provider) {
        	m_next = provider;
        }
        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        protected volatile Ensure m_sequencer;

        public void run() {
            m_sequencer.step();
            m_next.run();
        }
    }
}
