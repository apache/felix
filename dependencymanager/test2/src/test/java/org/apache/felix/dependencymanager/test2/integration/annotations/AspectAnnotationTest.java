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
package org.apache.felix.dependencymanager.test2.integration.annotations;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Verify Aspect Annotations usage.
 */
@RunWith(PaxExam.class)
public class AspectAnnotationTest extends TestBase {
    @Test
    public void testAspectChain() throws Throwable {
        Ensure e = new Ensure();
        // Activate service consumer
        ServiceRegistration scSequencer = register(e, "AspectChainTest.ServiceConsumer");
        // Activate service provider
        ServiceRegistration spSequencer = register(e, "AspectChainTest.ServiceProvider");
        // Activate service aspect 2
        ServiceRegistration sa2Sequencer = register(e, "AspectChainTest.ServiceAspect2");
        // Activate service aspect 3
        ServiceRegistration sa3Sequencer = register(e, "AspectChainTest.ServiceAspect3");
        // Activate service aspect 1
        ServiceRegistration sa1Sequencer = register(e, "AspectChainTest.ServiceAspect1");

        e.step();
        e.waitForStep(6, 10000);

        // Deactivate service provider
        spSequencer.unregister();
        // Make sure that service aspect 1 has been called in ts removed and stop callbacks 
        e.waitForStep(8, 10000);
        e.ensure();
        
        scSequencer.unregister();
        sa1Sequencer.unregister();
        sa2Sequencer.unregister();
        sa3Sequencer.unregister();
    }
}
