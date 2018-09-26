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
import org.apache.felix.dm.runtime.itest.components.CollectionFieldDependencyAnnotation;
import org.osgi.framework.ServiceRegistration;

/**
 * Check if a client already bound to a service provider is called in swap method
 * when an aspect service replaces the original service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings("rawtypes")
public class CollectionFieldDependencyAnnotationTest extends TestBase {
    
	public void testAnnotatedAspect() {
        Ensure e = new Ensure();
        // register the two providers
        ServiceRegistration provider1 = register(e, CollectionFieldDependencyAnnotation.ProviderImpl1.ENSURE);
        ServiceRegistration provider2 = register(e, CollectionFieldDependencyAnnotation.ProviderImpl2.ENSURE);        
        e.waitForStep(2, 5000);
        
        // register the consumer
        ServiceRegistration consumer = register(e, CollectionFieldDependencyAnnotation.Consumer.ENSURE);
        e.waitForStep(3, 5000);
        
        // check if the first collection (m_list1) was properly injected in consumer
        e.waitForStep(4, 5000);
        
        // check if the second collection (m_list2) was properly injected in consumer
        e.waitForStep(6, 5000);
        
        provider1.unregister();
        provider2.unregister();
        consumer.unregister();
    }
}
