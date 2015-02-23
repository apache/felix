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
package org.apache.felix.dm.itest.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX2348_ResourceAdapterTest extends TestBase {
    public void testBasicResourceAdapter() throws Exception {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        m.add(m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, null, "changed")
              .setImplementation(new ResourceAdapter(e)));
        URL resourceURL = new URL("file://localhost/path/to/file1.txt");
        m.add(m.createComponent().setImplementation(new ResourceProvider(context, resourceURL))
        	   .add(m.createServiceDependency().setService(ResourceHandler.class).setCallbacks("add", "remove")));
        e.waitForStep(3, 5000);
        m.clear();
     }
    
    static class ResourceAdapter {
        protected URL m_resource; // injected by reflection.
        private Ensure m_ensure;
        
        ResourceAdapter(Ensure e) {
            m_ensure = e;
        }
        
        public void start() {
            m_ensure.step(1);
            Assert.assertNotNull("resource not injected", m_resource);
            m_ensure.step(2);
            try {
                m_resource.openStream();
            } 
            catch (FileNotFoundException e) {
                m_ensure.step(3);
            }
            catch (IOException e) {
                Assert.fail("We should not have gotten this exception.");
            }
        }
    }    
}
