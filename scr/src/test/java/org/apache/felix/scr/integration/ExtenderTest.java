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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.component.ComponentConstants;

@RunWith(JUnit4TestRunner.class)
public class ExtenderTest extends ComponentTestBase
{
    static
    {
        // uncomment to enable debugging of this test class
//          paxRunnerVmOption = DEBUG_VM_OPTION;
    }
    
    @Test
    public void testWired() throws BundleException 
    {
        if (isAtLeastR5())
        {
            BundleWiring scrWiring = bundle.adapt(BundleWiring.class);
            List<BundleWire> extenderWires = scrWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);
            boolean wired = false;
            for (BundleWire wire: extenderWires) 
            {
                if (ComponentConstants.COMPONENT_CAPABILITY_NAME.equals(wire.getCapability().getAttributes().get(ExtenderNamespace.EXTENDER_NAMESPACE)))
                {
                    Assert.assertEquals("Not wired to us", "org.apache.felix.scr", wire.getProviderWiring().getBundle().getSymbolicName());
                    wired = true;
                    break;
                }
            }
            Assert.assertTrue("should be wired to us", wired);
        }
    }

}
