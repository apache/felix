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
import org.apache.felix.dm.runtime.itest.components.Felix4050;
import org.osgi.framework.ServiceRegistration;

/**
 * Test for FELIX-4050 issue: It validates that component state calculation does not mess up
 * when an @Init method adds an available dependency using the API, and also returns a Map for
 * configuring a named dependency.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Felix4050Test extends TestBase {
    public void testFelix4050() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, Felix4050.ENSURE);
        // wait for S to be started
        e.waitForStep(3, 10000);
        // remove our sequencer: this will stop S
        sr.unregister();
        // ensure that S is stopped and destroyed
        e.waitForStep(5, 10000);
    }
}
