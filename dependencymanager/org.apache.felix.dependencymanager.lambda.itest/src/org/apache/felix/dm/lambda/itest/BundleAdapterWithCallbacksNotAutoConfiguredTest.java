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

import static org.apache.felix.dm.lambda.DependencyManagerActivator.bundleAdapter;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleAdapterWithCallbacksNotAutoConfiguredTest extends TestBase {  
    final Ensure m_e = new Ensure();
    
    public void testBundleAdapterWithCallbacksNotAutoConfigured() {
        DependencyManager m = getDM();
        // create a bundle adapter service (one is created for each bundle)
        BundleAdapterWithCallback baWithCb = new BundleAdapterWithCallback();
        String bsn = "org.apache.felix.dependencymanager";
        String filter = "(Bundle-SymbolicName=" + bsn + ")";

        Component adapter = bundleAdapter(m).mask(Bundle.ACTIVE).filter(filter).add("add").impl(baWithCb).build();
                    												 
        // add the bundle adapter
        m.add(adapter);
        
        // Check if adapter has not been auto configured (because it has callbacks defined).
        m_e.waitForStep(1, 3000);
        Assert.assertNull("bundle adapter must not be auto configured", baWithCb.getBundle());
        
        // remove the bundle adapters
        m.remove(adapter);
    }
                
    class BundleAdapterWithCallback {
        volatile Bundle m_bundle; // must not be auto configured because we are using callbacks.
        
        Bundle getBundle() {
            return m_bundle;
        }
        
        void add(Bundle b) {
            Assert.assertNotNull(b);
            Assert.assertEquals("org.apache.felix.dependencymanager", b.getSymbolicName());
            m_e.step(1);
        }
    }
}
