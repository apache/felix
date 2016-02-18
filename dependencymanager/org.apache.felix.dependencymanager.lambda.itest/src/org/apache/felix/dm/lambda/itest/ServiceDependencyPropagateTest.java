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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.ServiceReference;

/**
 * Validates ServiceDependency service properties propagation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class ServiceDependencyPropagateTest extends TestBase {
    /**
     * Checks that a ServiceDependency propagates the dependency service properties to the provided service properties.
     */
    public void testServiceDependencyPropagate() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        
        Component c1 = component(m)
                      .impl(new C1(e))
                      .withSvc(C2.class, s->s.add("bind")).build();

        Component c2 = component(m)
                      .provides(C2.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
                      .impl(new C2())
                      .withSvc(C3.class, s->s.propagate()).build();

        Component c3 = component(m)
                      .provides(C3.class.getName(), new Hashtable() {{ put("foo2", "bar2"); put("foo", "overriden");}})
                      .impl(new C3()).build();
        
        m.add(c1);
        m.add(c2);
        m.add(c3);

        e.waitForStep(3, 10000);
        
        m.remove(c3);
        m.remove(c2);
        m.remove(c1);
        m.clear();
    }
    
    /**
     * Checks that a ServiceDependency propagates the dependency service properties to the provided service properties,
     * using a callback method.
     */
    public void testServiceDependencyPropagateCallback() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component c1 = component(m)
                      .impl(new C1(e))
                      .withSvc(C2.class, s->s.add("bind")).build();

        C2 c2Impl = new C2();
        Component c2 = component(m)
                      .provides(C2.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
                      .impl(c2Impl)
                      .withSvc(C3.class, s->s.propagate(c2Impl, "getServiceProperties")).build();
        
        Component c3 = component(m)
                      .provides(C3.class.getName())
                      .impl(new C3()).build();
        
        m.add(c1);
        m.add(c2);
        m.add(c3);

        e.waitForStep(3, 10000);
        m.clear();
    }
    
    public void testServiceDependencyPropagateCallbackRef() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component c1 = component(m)
                      .impl(new C1(e))
                      .withSvc(C2.class, s->s.add(C1::bind)).build();

        C2 c2Impl = new C2();
        Component c2 = component(m)
                      .provides(C2.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
                      .impl(c2Impl)
                      .withSvc(C3.class, s->s.propagate(c2Impl::getServiceProperties)).build();
        
        Component c3 = component(m)
                      .provides(C3.class.getName())
                      .impl(new C3()).build();
        
        m.add(c1);
        m.add(c2);
        m.add(c3);

        e.waitForStep(3, 10000);
        m.clear();
    }

    public static class C1 {
        private Map m_props;
        private Ensure m_ensure;
        
        C1(Ensure ensure) {
            m_ensure = ensure;
        }

        void bind(C2 c2, Map props) {
            m_props = props;
        }
        
        void start() {
            m_ensure.step(1);
            if ("bar".equals(m_props.get("foo"))) { // "foo=overriden" from C2 should not override our own "foo" property
                m_ensure.step(2);
            }
            if ("bar2".equals(m_props.get("foo2"))) {
                m_ensure.step(3);
            }
        }
    }
    
    public static class C2 {
      C3 m_c3;
      
      public Dictionary getServiceProperties(ServiceReference ref) {
          return new Hashtable() {{ put("foo2", "bar2"); put("foo", "overriden"); }};
      }
    }
    
    public static class C3 {
    }
}
