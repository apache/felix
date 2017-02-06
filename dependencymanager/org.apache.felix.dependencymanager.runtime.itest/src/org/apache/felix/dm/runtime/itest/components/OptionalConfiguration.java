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

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class OptionalConfiguration {
    public final static String ENSURE_CONF_CREATOR = "OptionalConfiguration.Conf.Creator";
    public final static String ENSURE_CONF_CONSUMER = "OptionalConfiguration.Conf.Consumer";
    final static String PID = "OptionalConfiguration.pid";

    public static interface MyConfig {
        public default String getTestkey() { return "default"; }
    }

    @Component
    public static class OptionalConfigurationConsumer {
	    @ServiceDependency(required = true, filter = "(name=" + ENSURE_CONF_CONSUMER + ")")
	    public volatile Ensure m_ensure;
	    
        protected volatile int m_updateCount;
        
        @ConfigurationDependency(pid=PID, required=false)
        public void updated(MyConfig cnf) { // optional configuration, called after start(), like any other optional dependency callbacks.
        	if (cnf != null) {
        		m_updateCount ++;
        		if (m_updateCount == 1) {
        			if (!"default".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
        		} else if (m_updateCount == 2) {
        			if (!"testvalue".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
            		m_ensure.step(2);
        		} else if (m_updateCount == 3) {
        			if (!"default".equals(cnf.getTestkey())) {
        				Assert.fail("Could not find the configured property.");
        			}
            		m_ensure.step(3);
        		}
        	} else {
        		// configuration destroyed: should never happen
        		m_ensure.throwable(new Exception("lost configuration"));
        	}
        }

        @Start
        public void start() {
        	m_ensure.step(1);
        }
        
        @Stop
        public void stop() {
        	m_ensure.step(4);
        }
    }

	@Component
	public static class ConfigurationCreator {
		@ServiceDependency
	    private volatile ConfigurationAdmin m_ca;
		
	    @ServiceDependency(required = true, filter = "(name=" + ENSURE_CONF_CREATOR + ")")
	    private Ensure m_ensure;
	    
	    Configuration m_conf;
	    
	    @Start
		public void start() {
	        try {
	        	Assert.assertNotNull(m_ca);
	            m_conf = m_ca.getConfiguration(PID, null);
	            Dictionary<String, Object> props = new Hashtable<>();
	            props.put("testkey", "testvalue");
	            m_conf.update(props);
	        }
	        catch (IOException e) {
	            Assert.fail("Could not create configuration: " + e.getMessage());
	        }
	    }
	    
	    @Stop
	    public void stop() throws IOException {
	    	m_conf.delete();  
	    }
	}
	
}
