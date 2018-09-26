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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.PropertyType;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentPropertyTypeWithDictionaryPassedInUpdateCallback {
    public final static String ENSURE = "ComponentPropertyWithDictionaryPassedInUpdateCallback";

    @PropertyType
    @Retention(RetentionPolicy.CLASS)
    @interface MyProperty {
    	String string_value();
    }

	@Component(provides=MyComponent.class)
	@MyProperty(string_value="defstring")
    public static class MyComponent {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;
        
        private Dictionary<String, String> m_cnf;
        private MyProperty m_prop;

		@ConfigurationDependency(propagate=true)
		void updated(Dictionary<String, String> cnf, MyProperty prop) {
		    m_cnf = cnf;
		    m_prop = prop;
		}		
		
		@Start
		void start() {
          System.out.println("MyComponent.start: cnf=" + m_cnf + ", string_value=" + m_prop.string_value());
          if ("configured".equals(m_cnf.get("string.value")) && "configured".equals(m_prop.string_value())) {
              m_ensure.step(1);
          }
		}
    }

    @Component
    public static class MyConsumer {        
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;
        Configuration m_conf;

        @ServiceDependency
        void bind(ConfigurationAdmin cm) {
            try {
                m_conf = cm.getConfiguration(MyProperty.class.getName());
                Hashtable<String, Object> newprops = new Hashtable<>();
                newprops.put("string.value", "configured");
                m_conf.update(newprops);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        
        @ServiceDependency(removed="remove", required=false)
        void bind(MyComponent comp, Map<String, Object> props) {
        	try {
        		// first, we expect to be injected with MyComponent with the following properties:
        		// string.value=another_string and double_value=123     	
        		if ("configured".equals(props.get("string.value"))) {
					m_ensure.step(2);
					m_conf.delete();
				}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        void remove(MyComponent comp) {
            m_ensure.step(3);
        }
    }
    
}
