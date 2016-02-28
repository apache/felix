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

import org.osgi.service.log.LogService;

/**
 * The implementation for our service provider.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProviderImpl implements ServiceProvider {
    /**
     * Our log service, injected from Activator.
     * If field is only accessed by callbacks, then you do not need to declare it as volatile, because the DM thread model
     * ensures "safe publications" of objects when lifecycle or dependency callbacks are invoked.
     * Here we declare the field using volatile because our hello method may be invoked from any thread, and declaring the field
     * as volatile will ensure safe publication if the log service is replaced at runtime.
     */
    volatile LogService log;
    
    void start() {
        // Default lifecycle start calback (all required dependencies have been injected when start is called, 
        // and all optional dependency callbacks will be invoked after the start method. This allows to easily 
        // implement the whiteboard pattern: you are first injected with required dependencies, then you can initialize
        // your component from the start callback, and then optional dependency callbacks are invoked.
        
        // Notice that all callbacks are serially executed and you don't need to synchronize your callbacks and
        // you can perform any manual service registrations (using BundleContext.registerService) whithout dealing
        // with synchronization, because no locks are held by DM when callbacks are invoked.
    }
    
    @Override
    public void hello() {
        log.log(LogService.LOG_WARNING, "ServiceProviderImpl.hello");
    }
}
