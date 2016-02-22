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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests various bind method signatures
 */
public class MethodSignatures {
	// For Consumer service
    public final static String ENSURE_SERVICE_DEPENDENCY = "MethodSignatures1";
    
    // For FactoryPidComponent
    public final static String ENSURE_FACTORYPID = "MethodSignatures2";

    // This component configures the Consumer component.
	@Component
	public static class ConsumerConfigurator {
		@ServiceDependency(filter="(name=" + ENSURE_SERVICE_DEPENDENCY + ")")
		Ensure m_ensure;
		
		@ServiceDependency
		ConfigurationAdmin m_cm;

		private Configuration m_conf;
	      private Configuration m_conf2;

		@Start
		void start() throws IOException {
			m_conf = m_cm.getConfiguration(Consumer.class.getName());
			Hashtable<String, Object> props = new Hashtable<>();
			props.put("foo", "bar");
			m_conf.update(props);
			
			m_conf2 = m_cm.getConfiguration(ConsumerConfig.class.getName());
			props = new Hashtable<>();
			props.put("foo", "bar");
			m_conf2.update(props);
		}
		
		@Stop
		void stop() throws IOException {
			m_conf.delete();
			m_conf2.delete();
		}
	}

	// A simple provider service
	@Component(provides=Provider.class)
	public static class Provider {
		@ServiceDependency(filter="(name=" + ENSURE_SERVICE_DEPENDENCY + ")")
		Ensure m_ensure;
	}
	
	public interface ConsumerConfig {
	    String getFoo();
	}
	
	// This consumer depends on the configuration and on the provider, using multiple method signatures.
	@Component
	public static class Consumer {
		Dictionary<String, Object> m_properties;
		Dictionary<String, Object> m_properties2;
		ConsumerConfig m_config;
        ConsumerConfig m_config2;

		@ConfigurationDependency // pid=Consumer.class
		void updated(Dictionary<String, Object> properties) {
		    if (properties != null) {
		        m_properties = properties;
		    } else {
		         m_ensure.step();
		    }
		}
		
		@ConfigurationDependency // pid = Consumer.class
		void updated2(org.apache.felix.dm.Component component, Dictionary<String, Object> properties) {
		    if (properties != null) {
		        Assert.assertNotNull(component);
		        m_properties2 = properties;
		    } else {
		        m_ensure.step();
		    }
		}
		
		@ConfigurationDependency
		void updated3(ConsumerConfig cnf) {
		    if (cnf != null) {
		        Assert.assertNotNull(cnf);
		        m_config = cnf;
		    } else {
		        m_ensure.step();
		    }
		}

		@ConfigurationDependency
		void updated4(org.apache.felix.dm.Component comp, ConsumerConfig cnf) {
		    if (cnf != null) {
		        Assert.assertNotNull(comp);
		        Assert.assertNotNull(cnf);
		        m_config2 = cnf;
		    } else {
		        m_ensure.step();
		    }
		}

		@ServiceDependency(filter="(name=" + ENSURE_SERVICE_DEPENDENCY + ")")
		Ensure m_ensure;
		
		@ServiceDependency
		void bind(org.apache.felix.dm.Component component, ServiceReference ref, Provider provider) {
			Assert.assertNotNull(component);
			Assert.assertNotNull(ref);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}
		
		@ServiceDependency
		void bind(org.apache.felix.dm.Component component, Provider provider) {
			Assert.assertNotNull(component);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}
		
		@ServiceDependency
		void bind(org.apache.felix.dm.Component component, Map<?, ?> properties, Provider provider) {
			Assert.assertNotNull(component);
			Assert.assertNotNull(properties);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}

		@ServiceDependency
		void bind(ServiceReference ref, Provider provider) {
			Assert.assertNotNull(ref);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}

		@ServiceDependency
		void bind(Provider provider) {
			Assert.assertNotNull(provider);
			m_ensure.step();
		}

		@ServiceDependency
		void bind(Provider provider, Map<?,?> properties) {
			Assert.assertNotNull(provider);
			Assert.assertNotNull(properties);
			m_ensure.step();
		}
		
		@ServiceDependency
		void bind(Map<?,?> properties, Provider provider) {
			Assert.assertNotNull(properties);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}
		
		@ServiceDependency
		void bind(Provider provider, Dictionary<?,?> properties) {
			Assert.assertNotNull(properties);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}
		
		@ServiceDependency
		void bind(Dictionary<?,?> properties, Provider provider) {
			Assert.assertNotNull(properties);
			Assert.assertNotNull(provider);
			m_ensure.step();
		}
		
		@Start
		void start() {
			Assert.assertNotNull(m_properties);
			Assert.assertNotNull(m_properties2);
			Assert.assertEquals("bar", m_properties.get("foo"));
			Assert.assertEquals("bar", m_properties2.get("foo"));
			Assert.assertNotNull(m_config);
			Assert.assertEquals("bar", m_config.getFoo());
			Assert.assertEquals("bar", m_config2.getFoo());
			m_ensure.step(10);
		}
	}
	
	public interface Config {
	    String getFoo();
	}	   

    // This component configures the FactoryPidComponent / FactoryPidComponent2 components.
	@Component
	public static class FactoryPidConfigurator {
		@ServiceDependency(filter="(name=" + ENSURE_FACTORYPID + ")")
		Ensure m_ensure;
		
		@ServiceDependency
		ConfigurationAdmin m_cm;

		private Configuration m_conf1;
		private Configuration m_conf2;
        private Configuration m_conf3;

		@Start
		void start() throws IOException {
			m_conf1 = m_cm.createFactoryConfiguration(FactoryPidComponent.class.getName());
			Hashtable<String, Object> props = new Hashtable<>();
			props.put("foo", "bar");
			m_conf1.update(props);
			
			m_conf2 = m_cm.createFactoryConfiguration(FactoryPidComponent2.class.getName());
			props = new Hashtable<>();
			props.put("foo", "bar");
			m_conf2.update(props);
			
			m_conf3 = m_cm.createFactoryConfiguration(Config.class.getName());
			props = new Hashtable<>();
			props.put("foo", "bar");
			m_conf3.update(props);
		}
		
		@Stop
		void stop() throws IOException {
			m_conf1.delete();
			m_conf2.delete();
			m_conf3.delete();
		}
	}

	// This is a factory pid component with an updated callback having the "updated(Dictionary)" signature
	@FactoryConfigurationAdapterService
	public static class FactoryPidComponent {
		Dictionary<String, Object> m_properties;
		
		void updated(Dictionary<String, Object> properties) {
			m_properties = properties;
		}
		
		@ServiceDependency(filter="(name=" + ENSURE_FACTORYPID + ")")
		Ensure m_ensure;
		
		@Start
		void start() {
			Assert.assertNotNull(m_properties);
			Assert.assertEquals("bar", m_properties.get("foo"));
			m_ensure.step();
		}
        
        @Stop
        void stop() {
            m_ensure.step();
        }
	}
	
	// This is a factory pid component with an updated callback having the "updated(Component, Dictionary)" signature
	@FactoryConfigurationAdapterService
	public static class FactoryPidComponent2 {
		Dictionary<String, Object> m_properties;
		
		void updated(org.apache.felix.dm.Component component, Dictionary<String, Object> properties) {
			Assert.assertNotNull(component);
			m_properties = properties;
		}
		
		@ServiceDependency(filter="(name=" + ENSURE_FACTORYPID + ")")
		Ensure m_ensure;
		
		@Start
		void start() {
			Assert.assertNotNull(m_properties);
			Assert.assertEquals("bar", m_properties.get("foo"));
			m_ensure.step();
		}
		
		@Stop
		void stop() {
		    m_ensure.step();
		}
	}
	
	// This is a factory pid component with an updated callback having the "updated(Config)" signature
    @FactoryConfigurationAdapterService(configType=Config.class)
    public static class FactoryPidComponent3 {
        Config m_properties;
        
        void updated(Config properties) {
            m_properties = properties;
        }
        
        @ServiceDependency(filter="(name=" + ENSURE_FACTORYPID + ")")
        Ensure m_ensure;
        
        @Start
        void start() {
            Assert.assertNotNull(m_properties);
            Assert.assertEquals("bar", m_properties.getFoo());
            m_ensure.step();
        }
        
        
        @Stop
        void stop() {
            m_ensure.step();
        }
    }
}
