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
import org.apache.felix.dm.annotation.api.PropertyType;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A Factory Pid component which provides its service properties using property type annotations.
 * The service properties can also be overriden from factory configuration.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryPidWithPropertyTypeAnnotation {
    public final static String ENSURE = "FactoryPidWithPropertyTypeAnnotation";

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

    /**
     * Service properties are defined using the two annotations above, 
     * and we override some service properties using the factory configuration
     */ 
	@Component(provides=MyComponent.class, propagate=true, factoryPid="org.apache.felix.dm.runtime.itest.components.FactoryPidWithPropertyTypeAnnotation$MyProperties")
	@MyProperties(string_value="string")
    @MyProperties2(string_value2="string2")
	public static class MyComponent {
		void updated(MyProperties props, MyProperties2 props2) {
		    if (props != null) {
		        System.out.println("MyComponent.updated: string_value=" + props.string_value() + ", double_value=" + props.double_value() +
		            "string_value2=" + props2.string_value2() + ", double_value2=" + props2.double_value2());
		    }
		}
    }

    @Component
    public static class MyConsumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        Ensure m_ensure;
        
        Configuration m_conf;

        @ServiceDependency
        void bind(ConfigurationAdmin cm) {
            try {
                m_conf = cm.createFactoryConfiguration(MyProperties.class.getName());
                Hashtable<String, Object> newprops = new Hashtable<>();
                newprops.put("string.value", "CM");
                m_conf.update(newprops);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
                
        @ServiceDependency(required=false, changed="change", removed="remove")
        void add(MyComponent comp, Map<String, Object> props) {
        	try {
        		// first, we expect to be injected with MyComponent with the following properties:
        		// string.value=CM and double_value=123  
        		if ("CM".equals(props.get("string.value")) && new Double(123).equals(props.get("double.value")) &&
        		    "string2".equals(props.get("string.value2")) && new Double(456).equals(props.get("double.value2"))) {
					m_ensure.step(1);

					// at this point, let's reconfigure the factory component
					Hashtable<String, Object> newprops = new Hashtable<>();
					newprops.put("string.value", "CM modified");
					m_conf.update(newprops);
				}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        void change(MyComponent comp, Map<String, Object> props) {
        	try {
        		System.out.println("MyConsumer.change: " + props);
        		if ("CM modified".equals(props.get("string.value")) && new Double(123).equals(props.get("double.value")) &&
        		    "string2".equals(props.get("string.value2")) && new Double(456).equals(props.get("double.value2"))) {			
        		    m_ensure.step(2);
        		    m_conf.delete();
				} 
        	}  catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        void remove(MyComponent comp, Map<String, Object> props) {
            m_ensure.step(3);
        }
    }  
    
}
