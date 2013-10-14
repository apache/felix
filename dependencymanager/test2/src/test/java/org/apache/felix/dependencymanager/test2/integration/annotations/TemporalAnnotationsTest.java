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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dependencymanager.test2.components.Ensure;
import org.apache.felix.dependencymanager.test2.components.TemporalAnnotations;
import org.apache.felix.dependencymanager.test2.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Verify Temporal Service dependency Annotations usage.
 */
@RunWith(PaxExam.class)
public class TemporalAnnotationsTest extends TestBase
{
    @Test
    public void testTemporalServiceDependency()
    {
        Ensure ensure = new Ensure();
        ServiceRegistration ensureReg = register(ensure, TemporalAnnotations.ENSURE);                       
        Dictionary props = new Hashtable() {{ put("test", "temporal"); }};
        Runnable r = Ensure.createRunnableStep(ensure, 1);             
        ServiceRegistration sr = context.registerService(Runnable.class.getName(), r, props);
        ensure.waitForStep(1, 15000);
        System.out.println("unregistering R");
        sr.unregister();
        ensure.step(2);
        sleep(500);
        r = Ensure.createRunnableStep(ensure, 3);
        sr = context.registerService(Runnable.class.getName(), r, props);
        ensure.waitForStep(3, 15000);
        sr.unregister();
        ensure.step(4);
        sleep(1500);
        ensure.waitForStep(5, 15000);
        ensureReg.unregister();
    }
}
