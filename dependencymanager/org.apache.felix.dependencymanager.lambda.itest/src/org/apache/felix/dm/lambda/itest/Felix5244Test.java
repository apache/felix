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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Felix5244Test extends TestBase {
    private final Ensure m_ensure = new Ensure();

    public void testAbstractBindMethod() throws Exception {
        final DependencyManager dm = getDM();
        Component myService = component(dm).impl(new MyService()).withSvc(MyDependency.class, svc -> svc.required().add(AbstractService::bind)).build();
        Component myDependency = component(dm).impl(new MyDependency() {}).provides(MyDependency.class).build();
        dm.add(myService);
        dm.add(myDependency);
        m_ensure.waitForStep(1,  5000);        
        dm.clear();
    }

    interface MyDependency {}
    
    public abstract class AbstractService {
		void bind(MyDependency myDependency) {
			Assert.assertNotNull(myDependency);
    		m_ensure.step(1);
    	}
    }
    
    public class MyService extends AbstractService {
    }    
}
