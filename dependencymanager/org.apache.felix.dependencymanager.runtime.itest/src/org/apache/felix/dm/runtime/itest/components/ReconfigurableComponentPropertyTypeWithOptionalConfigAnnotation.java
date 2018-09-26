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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.PropertyType;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ReconfigurableComponentPropertyTypeWithOptionalConfigAnnotation {
    public final static String ENSURE = "ReconfigurableComponentPropertyTypeWithOptionalConfigAnnotation";
        
    @PropertyType
    @Retention(RetentionPolicy.CLASS)
    @interface MyReconfigurableProperties {
    	String string_value() default "defstring";
    	double double_value() default 123;    	
    }

	@Component(provides=MyComponent.class)
	@MyReconfigurableProperties(string_value="another_string")
    public static class MyComponent {
		@ConfigurationDependency(propagate=true, required=false)
		void updated(MyReconfigurableProperties props) {
			System.out.println("MyComponent.updated: string_value=" + props.string_value() + ", double_value=" + props.double_value());
		}		
    }

    @Component
    public static class MyConsumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;

        @ServiceDependency
        volatile ConfigurationAdmin m_cm;
        
        Configuration m_conf;
        
        @ServiceDependency(changed="change")
        void bind(MyComponent comp, Map<String, Object> props) {
        	try {
        		// first, we expect to be injected with MyComponent with the following properties:
        		// string.value=another_string and double_value=123     	
        		if ("another_string".equals(props.get("string.value")) && new Double(123).equals(props.get("double.value"))) {
					m_ensure.step(1);

					// at this point, let's reconfigure ourself
					m_conf = m_cm.getConfiguration(MyReconfigurableProperties.class.getName());
					Hashtable<String, Object> newprops = new Hashtable<>();
					newprops.put("string.value", "a_string_configured_from_CM");
					m_conf.update(newprops);
				}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        void change(MyComponent comp, Map<String, Object> props) {
        	try {
        		System.out.println("MyConsumer.change: " + props);
				if (m_conf != null) {
					// CM is reconfiguring us: we expect to be injected with MyComponent with the following properties:
					// string.value=a_string_configured_from_CM and double_value=123
					if ("a_string_configured_from_CM".equals(props.get("string.value"))
							&& new Double(123).equals(props.get("double.value"))) {
						m_ensure.step(2);
						Configuration conf = m_conf;
						m_conf = null;
						conf.delete();
					}
				} else {
					// configuration has been deleted, we expect to be injected with default service properties:
					// string.value=another_string" and double_value=123
					if ("another_string".equals(props.get("string.value")) && new Double(123).equals(props.get("double.value"))) {							
						m_ensure.step(3);
					}
				}
        	}  catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }  
    
}
