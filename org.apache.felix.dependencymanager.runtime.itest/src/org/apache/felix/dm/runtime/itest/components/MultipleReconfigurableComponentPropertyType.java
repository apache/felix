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
public class MultipleReconfigurableComponentPropertyType {
    public final static String ENSURE = "MultipleReconfigurableComponentPropertyType";

    @PropertyType
    @Retention(RetentionPolicy.CLASS)
    @interface MyProperties {
    	String string_value() default "defstring";
    	double double_value() default 123;    	
    }

    @PropertyType
    @Retention(RetentionPolicy.CLASS)
    @interface MyProperties2 {
        String string_value2() default "defstring2";
        double double_value2() default 456;      
    }

    @Component(provides=MyComponent.class)
	@MyProperties(string_value="string")
	@MyProperties2(string_value2="string2")
    public static class MyComponent {                
		@ConfigurationDependency(propagate=true, pidClass=MyComponent.class)
		void updated(MyProperties props, MyProperties2 props2) {
		    if (props != null && props2 != null) {
		        System.out.println("MyComponent.updated: string_value=" + props.string_value() + ", double_value=" + props.double_value() +
		            "string_value2=" + props2.string_value2() + ", double_value2=" + props2.double_value2());
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
                m_conf = cm.getConfiguration(MyComponent.class.getName());
                Hashtable<String, Object> newprops = new Hashtable<>();
                newprops.put("string.value", "string from CM");
                m_conf.update(newprops);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @ServiceDependency(required=false, removed="unbind")
        void bind(MyComponent comp, Map<String, Object> props) {
        	try {
        		// first, we expect to be injected with MyComponent with the following properties:
        		// string.value=another_string and double_value=123     	
        		if ("string from CM".equals(props.get("string.value")) && new Double(123).equals(props.get("double.value")) &&
        		    "string2".equals(props.get("string.value2")) && new Double(456).equals(props.get("double.value2"))) {
					m_ensure.step(1);
					m_conf.delete();
				}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        void unbind(MyComponent comp) {
            m_ensure.step(2);
        }
    }  
    
}
