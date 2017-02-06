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
import org.apache.felix.dm.runtime.itest.components.FELIX5337;
import org.apache.felix.dm.runtime.itest.components.FELIX5337_MatchAllServicesWithFilter;
import org.osgi.framework.ServiceRegistration;

/**
 * Test test validates that we can lookup ALL existing services using annotation, and "(objectClass=*)" filter.
 */
@SuppressWarnings("rawtypes")
public class FELIX5337Test extends TestBase {
    public void testCatchAllServicesUsingAnnotation() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, FELIX5337.ENSURE);
        // wait for S to be started
        e.waitForStep(2, 5000);
        // remove our sequencer: this will stop S
        sr.unregister();
        // ensure that S is stopped and destroyed
        e.waitForStep(3, 5000);
    }
    
    public void testCatchAllServicesWithFiltersUsingAnnotation() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, FELIX5337_MatchAllServicesWithFilter.ENSURE);
        // wait for S to be started
        e.waitForStep(1, 5000);
        sr.unregister();
    }
}
