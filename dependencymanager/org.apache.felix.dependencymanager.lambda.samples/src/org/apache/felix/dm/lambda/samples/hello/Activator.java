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
package org.apache.felix.dm.lambda.samples.hello;

import static java.lang.System.out;

import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    @Override
    public void activate() throws Exception {
    	out.println("type \"log info\" to see the logs emitted by this test.");
    	
    	// Creates a Service Provider
        component(comp -> comp
            .impl(ServiceProviderImpl.class)
            .provides(ServiceProvider.class, property1 -> "value1", property2 -> 123) // property names are deduced from lambda parameter names
            .withSrv(LogService.class));

        // Creates a Service Consumer. we depend on LogService, EventAdmin and on our ServiceProvider.
        // (LogService and EventAdmin are declared in one single method call).
        
        component(comp -> comp
            .impl(ServiceConsumer.class)
            .withSrv(LogService.class, EventAdmin.class)
            .withSrv(ServiceProvider.class, srv -> srv.filter("(property1=value1)")) 
            .withCnf(ServiceConsumer.class));  
        
        // Creates a component that populates some properties in the Configuration Admin.
        component(comp -> comp.impl(Configurator.class).withSrv(ConfigurationAdmin.class));
    }
}
