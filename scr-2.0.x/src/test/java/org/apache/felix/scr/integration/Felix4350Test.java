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

import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;
import org.apache.felix.scr.integration.components.Felix4350Component;
import org.apache.felix.scr.integration.components.SimpleComponent;
import org.apache.felix.scr.integration.components.SimpleComponent2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

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
    public void test_unbind_while_activating_single_static() throws Exception
    {
        doTest("SingleStatic");
    }

    @Test
    public void test_unbind_while_activating_single_dynamic() throws Exception
    {
        doTest("SingleDynamic");
    }

    @Test
    public void test_unbind_while_activating_multiple_dynamic() throws Exception
    {
        doTest("MultipleDynamic");
    }

    @Test
    public void test_unbind_while_activating_multiple_static_greedy() throws Exception
    {
        doTest("MultipleStaticGreedy");
    }

    @Test
    public void test_unbind_while_activating_multiple_static_reluctant() throws Exception
    {
        doTest("MultipleStaticReluctant");
    }

    protected void doTest(String componentName) throws Exception
    {
        ServiceRegistration dep1Reg = register(new SimpleComponent(), 0);
        ServiceRegistration dep2Reg = register(new SimpleComponent2(), 1000);
        
        final ComponentDescriptionDTO main = findComponentDescriptorByName(componentName);
        TestCase.assertNotNull(main);

        asyncEnable(main); //needs to be async
        delay(300); //dep2 getService has not yet returned
        dep1Reg.unregister();
        delay(2000); //dep2 getService has returned

        Felix4350Component.check(0, 0, false);

        dep1Reg = register(new SimpleComponent(), 0);
        delay(300);

        Felix4350Component.check(1, 0, true);

        disableAndCheck(main);  //does not need to be asyncv??
        dep1Reg.unregister();
        dep2Reg.unregister();

        Felix4350Component.check(1, 1, false);
        dep1Reg = register(new SimpleComponent(), 0);
        dep2Reg = register(new SimpleComponent2(), 1000);
        Felix4350Component.check(1, 1, false);
        
        asyncEnable(main); //needs to be async
        delay(300);
        dep1Reg.unregister();
        delay(100);
        dep1Reg = register(new SimpleComponent(), 0);
        delay(2000);

        Felix4350Component.check(2, 1, true); //n.b. counts are cumulative
    }
    
    protected void asyncEnable( final ComponentDescriptionDTO cd ) throws Exception
    {
    	new Thread( new Runnable() {

			public void run() {
				try
                {
                    enableAndCheck( cd );
                }
                catch (InvocationTargetException e)
                {
                }
                catch (InterruptedException e)
                {
                }
			}}).start();
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
