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
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.PropertyType;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentDMPropertyTypeArrayAnnotation {
    public final static String ENSURE = "ComponentDMPropertyTypeArrayAnnotation";
        
    @PropertyType
    @Retention(RetentionPolicy.CLASS)
    @interface MyProperties {
    	String[] pattern();  
    	String[] pattern2();  
    	String prefix();
    }
    
	@Component(provides=MyComponent.class)
	@MyProperties(pattern="/web", pattern2 = { "/web1", "/web2" }, prefix="/*")
    public static class MyComponent {
    }
	
    @Component
    public static class MyConsumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;

        @ServiceDependency
        void bind(MyComponent comp, Map<String, Object> props) {
        	m_ensure.step(1);
        	Object pattern = props.get("pattern");
        	if (pattern instanceof String) {
        		if ("/web".equals(pattern.toString()))
        			m_ensure.step(2);        			
        	} 
        	
        	Object pattern2 = props.get("pattern2");
        	if (pattern2 != null) {
        		if (pattern2 instanceof String[]) {        	
        			String[] array = (String[]) pattern2;
        			if (array.length == 2 && array[0].equals("/web1") && array[1].equals("/web2")) {
        				m_ensure.step(3);
        			}
        		}
        	} 
        	
        	if ("/*".equals(props.get("prefix"))) {
        		m_ensure.step(4);
        	}
        }
    }
    
}
