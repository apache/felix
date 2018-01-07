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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.itest.lifecycle.SimpleComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Checks if a component is stopped when its bundle is stopped.
 * see FELIX-5768
 */
public class StopCallbackTest extends TestBase {
    private final Ensure m_ensure = new Ensure();
    
    public void testStopCalledWhenBundleIsStopped() throws Exception {
        final DependencyManager dm = getDM();

        // Register the Ensure object in the osgi registry (the SimpleComponent depends on it).
        component(dm, comp -> comp.impl(m_ensure).provides(Ensure.class).properties("name", SimpleComponent.class.getName()));
                        		
        // Make sure the SimpleComponent has been started
        m_ensure.waitForStep(1, 5000);
        
        // Now, stop the bundle which contains the SimpleComponent
        BundleContext bc = FrameworkUtil.getBundle(SimpleComponent.class).getBundleContext();
        bc.getBundle().stop();
        
        // Make sure the SimpleComponent has been stopped
        m_ensure.waitForStep(2, 5000);
               
        // Remove our Ensure component
        dm.clear();        
    }    
}
