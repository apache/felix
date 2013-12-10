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
package org.apache.felix.scr.integration;

import javax.inject.Inject;

import junit.framework.TestCase;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleComponent2;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentInstance;

/**
 * This test validates the FELIX-4350 issue.
 */
@RunWith(JUnit4TestRunner.class)
public class Felix4350Test extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
//                paxRunnerVmOption = DEBUG_VM_OPTION;
        descriptorFile = "/integration_test_FELIX_4350.xml";
        //comment to get debug logging if the test fails.
//        DS_LOGLEVEL = "warn";
    }

    @Test
    public void test_unbind_while_activating_single_static()
    {
        doTest("SingleStatic");
    }

    @Test
    public void test_unbind_while_activating_single_dynamic()
    {
        doTest("SingleDynamic");
    }

    @Test
    public void test_unbind_while_activating_multiple_dynamic()
    {
        doTest("MultipleDynamic");
    }

    @Test
    public void test_unbind_while_activating_multiple_static_greedy()
    {
        doTest("MultipleStaticGreedy");
    }

    @Test
    public void test_unbind_while_activating_multiple_static_reluctant()
    {
        doTest("MultipleStaticReluctant");
    }

    protected void doTest(String componentName)
    {
        final Component main = findComponentByName(componentName);
        TestCase.assertNotNull(main);

        ServiceRegistration dep1Reg = register(new SimpleComponent(), 0);
        ServiceRegistration dep2Reg = register(new SimpleComponent2(), 1000);
        main.enable();
        delay(300);
        dep1Reg.unregister();
        delay(2000);

        ComponentInstance mainCompInst = main.getComponentInstance();
        TestCase.assertNull(mainCompInst);

        dep1Reg = register(new SimpleComponent(), 0);
        delay(300);

        mainCompInst = main.getComponentInstance();
        TestCase.assertNotNull(mainCompInst);

        main.disable();
        dep1Reg.unregister();
        dep2Reg.unregister();

        dep1Reg = register(new SimpleComponent(), 0);
        dep2Reg = register(new SimpleComponent2(), 1000);
        main.enable();
        delay(300);
        dep1Reg.unregister();
        delay(100);
        dep1Reg = register(new SimpleComponent(), 0);
        delay(2000);

        mainCompInst = main.getComponentInstance();
        TestCase.assertNotNull(mainCompInst);
    }

    protected ServiceRegistration register(final Object service, final int delay) {
        return bundleContext.registerService(service.getClass().getName(), new ServiceFactory() {
            public Object getService(Bundle bundle, ServiceRegistration registration)
            {
                delay(delay);
                return service;
            }
            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
            {
            }
        }, null);
    }

}
