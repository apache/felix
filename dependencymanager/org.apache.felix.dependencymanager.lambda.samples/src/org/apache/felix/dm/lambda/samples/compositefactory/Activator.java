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
package org.apache.felix.dm.lambda.samples.compositefactory;

import static java.lang.System.out;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * Creates a "Provider" service. The implementation for this service (ProviderImpl) is
 * created using a factory class (ProviderFactory) that also creates some other helper classes 
 * (ProviderComposite1 and ProviderComposite2) that are internally used by ProviderImpl.
 * 
 * The ProviderFactory is also injected with a Configuration that can be used by the Factory
 * when creating the ProviderImpl, ProviderComposite1, and ProviderComposite2 classes.
 * 
 * The LogService in only injected to the ProviderImpl and the ProviderComposite1 classes.
 * Both composites are called in their "start" callbacks, when all required dependencies are available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	out.println("type \"log info\" to see the logs emitted by this test.");

    	// Create the Factory used to instantiate ProvuderImpl, ProviderComposite1 and ProviderComposite2
        ProviderFactory factory = new ProviderFactory();
                
        // Define the component which implementation is instantiated by the ProviderFactory.
        // a LogService is injected in the ProviderImpl, as well as to the ProviderComposite1 class.
        // And a configuration is injected directly to the ProviderFactory so it can use some configurations
        // before creating the composition of classes.
        component(comp -> comp
            .factory(factory::create, factory::getComposition)
            .start(ProviderImpl::start) // only call start on ProviderImpl          
            .withSrv(LogService.class, srv -> srv.cb(ProviderImpl::bind).cb(ProviderComposite1::bind))
            .withCnf(conf -> conf.pid(ProviderFactory.class).cbi(factory::updated)));
                
        // Creates a configuration with pid name = "org.apache.felix.dependencymanager.lambda.samples.compositefactory.ProviderFactory"
        component(comp -> comp
            .impl(Configurator.class)
            .withSrv(ConfigurationAdmin.class));
    }
}
