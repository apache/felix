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

import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5406_FluentPropertyWithDotTest extends TestBase {
    private final Ensure m_ensure = new Ensure();

	public void testFluentServiceProperty() {
        final DependencyManager dm = getDM();
        
        component(dm, comp -> comp.factory(ProviderImpl::new).provides(Provider.class, foo -> "bar"));
        component(dm, comp -> comp.impl(new Consumer("foo", "bar"))
        		.withSvc(Provider.class, svc -> svc.filter("(foo=bar)").required().add(Consumer::bind)));
        
        m_ensure.waitForStep(1,  5000);
	}
	
	public void testFluentServicePropertyWithDot() {
        final DependencyManager dm = getDM();
        
        component(dm, comp -> comp.factory(ProviderImpl::new).provides(Provider.class, foo_bar -> "zoo"));
        component(dm, comp -> comp.impl(new Consumer("foo.bar", "zoo"))
        		.withSvc(Provider.class, svc -> svc.filter("(foo.bar=zoo)").required().add(Consumer::bind)));
        
        m_ensure.waitForStep(1,  5000);
	}
	
	public void testFluentServicePropertyWithUnderscore() {
        final DependencyManager dm = getDM();
        
        component(dm, comp -> comp.factory(ProviderImpl::new).provides(Provider.class, foo__bar -> "zoo"));
        component(dm, comp -> comp.impl(new Consumer("foo_bar", "zoo"))
        		.withSvc(Provider.class, svc -> svc.filter("(foo_bar=zoo)").required().add(Consumer::bind)));
        
        m_ensure.waitForStep(1,  5000);
	}

	interface Provider {		
	}
	
	public class ProviderImpl implements Provider {				
	}

	public class Consumer {
		private final String m_key;
		private final String m_value;

		Consumer(String key, String value) {
			m_key = key;
			m_value = value;
		}
		
		void bind(Provider provider, Map<String, Object> properties) {
            Assert.assertEquals(m_value, properties.get(m_key));
            m_ensure.step(1);
		}
	}
	
}
