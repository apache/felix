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
package org.apache.felix.dm.runtime.itest.tests;

import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.ProviderSwappedByAspect;
import org.osgi.framework.ServiceRegistration;

/**
 * Check if a client already bound to a service provider is called in swap method
 * when an aspect service replaces the original service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class ProviderSwappedByAspectTest extends TestBase {
    
	public void testAnnotatedAspect() {
        Ensure e = new Ensure();
        ServiceRegistration consumer = register(e, ProviderSwappedByAspect.Consumer.ENSURE);
        ServiceRegistration provider = register(e, ProviderSwappedByAspect.Provider.ENSURE);
        
        // client bound to provider (1), and client calls provider (2)
        e.waitForStep(2, 5000);
        
        ServiceRegistration aspect = register(e, ProviderSwappedByAspect.ProviderAspect.ENSURE);

        // provider swapped with aspect (3), aspect called (4), original provider called (5)     
        e.waitForStep(5, 5000);
        
        aspect.unregister();
        
        // aspect swapped by provider (6), provider called (7)
        e.waitForStep(7, 5000);

        provider.unregister();
        
        //provider unbound (8), provider called (9)
        e.waitForStep(9, 5000);
        
        consumer.unregister();
    }
}
