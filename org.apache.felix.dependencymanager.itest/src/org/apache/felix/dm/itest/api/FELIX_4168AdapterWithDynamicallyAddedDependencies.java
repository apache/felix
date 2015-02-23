/**
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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX_4168AdapterWithDynamicallyAddedDependencies extends TestBase {
    public void testAdapterWithExtraDependenciesAndCallbacks() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        // create a service S2, which will be added to A1 (needs to be available)
        Component s2 = m.createComponent().setInterface(S2.class.getName(), null).setImplementation(new S2Impl(e));
        m.add(s2);

        // create a service adapter that adapts to services S1 and has an optional dependency on services S2
        Component sa = m.createAdapterService(S1.class, null).setImplementation(SA.class);
        m.add(sa);

        // create a service S1, which triggers the creation of the first adapter instance (A1)
        Component s1 = m.createComponent().setInterface(S1.class.getName(), null).setImplementation(new S1Impl());
        m.add(s1);

        // create a second service S1, which triggers the creation of the second adapter instance (A2)
        Component s1b = m.createComponent().setInterface(S1.class.getName(), null).setImplementation(new S1Impl());
        m.add(s1b);

        // observe that S2 is also added to A2
        e.waitForStep(2, 5000);

        // remove S2 again
        m.remove(s2);

        // make sure both adapters have their "remove" callbacks invoked
        e.waitForStep(4, 5000);
        m.clear();
    }

    static interface S1 {
    }

    static interface S2 {
        public void invoke();
    }

    static class S1Impl implements S1 {
    }

    static class S2Impl implements S2 {

        private final Ensure m_e;

        public S2Impl(Ensure e) {
            m_e = e;
        }

        public void invoke() {
            m_e.step();
        }
    }

    public static class SA {
        volatile S2 s2;
        volatile Component component;
        volatile DependencyManager manager;

        public SA() {
            System.out.println("Adapter created");
        }

        public void init() {
            System.out.println("Adapter init " + s2);
            component.add(manager.createServiceDependency()
                .setService(S2.class).setCallbacks("add", "remove").setRequired(true));
        }

        public void add(S2 s) {
            System.out.println("adding " + s);
            s.invoke();
        }

        public void remove(S2 s) {
            System.out.println("removing " + s);
            s.invoke();
        }
    }
}
