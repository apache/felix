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

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	out.println("type \"log warn\" to see the logs emitted by this test.");
    	
    	// Creates a Service Provider (property names are deduced from lambda parameter names).
    	// (service dependencies are required by default)
        component(comp -> comp.impl(ServiceProviderImpl.class)
            .provides(ServiceProvider.class, p1 -> "v1", p2 -> 123)
            .withSvc(LogService.class));
            
        // Creates a Service Consumer. we depend on LogService, EventAdmin and on our ServiceProvider.
        // (LogService and EventAdmin are declared in one single method call).
        // We also depend on a configuration. Our ServiceConsumer.updated method takes as argument a "Configuration" interface
        // which is used to wrap the actual properties behind a dynamic proxy for our "Configuration" interface that is implemented by Dependency Manager.
        // (the pid is assumed to be by default the fqdn of our Configuration interface).
        component(comp -> comp.impl(ServiceConsumer.class)
            .withSvc(LogService.class, EventAdmin.class)
            .withSvc(ServiceProvider.class, srv -> srv.filter("(p1=v1)")) 
            .withCnf(cnf -> cnf.update(Configuration.class, ServiceConsumer::updated))); 
        
        // Creates a component that populates some properties in the Configuration Admin.
        // Here, we inject the CM (Configuration Admin) service dependency using a method reference:
        component(comp -> comp.factory(() -> new Configurator(Configuration.class.getName()))
            .withSvc(ConfigurationAdmin.class, srv -> srv.add(Configurator::bind)));
    }
}
