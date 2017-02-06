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
import org.apache.felix.dm.runtime.itest.components.OptionalConfiguration;
import org.osgi.framework.ServiceRegistration;

@SuppressWarnings("rawtypes")
public class OptionalConfigurationTest extends TestBase {
	
    public void testOptionalConfig() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration ensureConsumer = register(e, OptionalConfiguration.ENSURE_CONF_CONSUMER);
        e.waitForStep(1, 5000); // consumer called in updated with testKey="default" config, and then called in start
        
        // will register a "testKey=testvalue" configuration, using config admin.
        ServiceRegistration ensureConfCreator = register(e, OptionalConfiguration.ENSURE_CONF_CREATOR);
        
        e.waitForStep(2, 5000);  // consumer called in updated with testKey="testvalue"
        ensureConfCreator.unregister(); // remove configuration.
        e.waitForStep(3, 5000);  // consumer called in updated with default "testkey=default" property (using the default method from the config interface.        
        ensureConsumer.unregister(); // stop the OptionalConfigurationConsumer component
        e.waitForStep(4, 5000);  // consumer stopped

        e.ensure();
    }

}
