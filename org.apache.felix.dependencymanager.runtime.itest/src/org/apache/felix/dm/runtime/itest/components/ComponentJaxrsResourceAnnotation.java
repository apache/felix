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

import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.itest.util.Ensure;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentJaxrsResourceAnnotation {
    public final static String ENSURE = "ComponentJaxrsResourceAnnotation";
            
	@Component(provides=MyComponent.class)
	@JaxrsResource
    public static class MyComponent {
    }
	
    @Component
    public static class MyConsumer {
        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_ensure;

        @ServiceDependency
        void bind(MyComponent comp, Map<String, Object> props) {
        	m_ensure.step(1);
        	Object value = props.get(JaxRSWhiteboardConstants.JAX_RS_RESOURCE);        	
        	if (Boolean.TRUE.equals(value)) {
        		m_ensure.step(2);
        	}
        }
    }
    
}
