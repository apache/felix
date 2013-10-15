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
import org.apache.felix.dependencymanager.test2.components.SimpleAnnotations.Consumer;
import org.apache.felix.dependencymanager.test2.components.SimpleAnnotations.Producer;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Ensure that a Provider can be injected into a Consumer, using simple DM annotations.
 */
@RunWith(PaxExam.class)
public class SimpleAnnotationsTest extends TestBase {
    public SimpleAnnotationsTest() {
        super(true /* start test components bundle */);
    }

    @Test
    public void testSimpleAnnotations() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration er = register(e, Producer.ENSURE);
        e.waitForStep(3, 10000); // Producer registered
        ServiceRegistration er2 = register(e, Consumer.ENSURE);

        er2.unregister(); // stop consumer
        er.unregister(); // stop provider

        // And check if components have been deactivated orderly.
        e.waitForStep(10, 10000);
        e.ensure();
        stopTestComponentsBundle();
    }
}
