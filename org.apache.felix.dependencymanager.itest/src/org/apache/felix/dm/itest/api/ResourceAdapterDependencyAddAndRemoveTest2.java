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

import java.net.URL;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceAdapterDependencyAddAndRemoveTest2 extends TestBase {
    public void testBasicResourceAdapter() throws Exception {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a resource provider
        ResourceProvider provider = new ResourceProvider(context, new URL("file://localhost/path/to/file1.txt"));
        // activate it
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("id", "1");
        m.add(m.createComponent()
            .setInterface(ServiceInterface.class.getName(), props)
            .setImplementation(new ServiceProvider(e))
        );
        
        props = new Hashtable<String, String>();
        props.put("id", "2");
        m.add(m.createComponent()
            .setInterface(ServiceInterface.class.getName(), props)
            .setImplementation(new ServiceProvider(e))
        );
        
        m.add(m.createComponent()
            .setImplementation(provider)
            .add(m.createServiceDependency()
                .setService(ResourceHandler.class)
                .setCallbacks("add", "remove")
            )
        );
        
        // create a resource adapter for our single resource
        // note that we can provide an actual implementation instance here because there will be only one
        // adapter, normally you'd want to specify a Class here
        Dependency d = m.createServiceDependency().setService(ServiceInterface.class, "(id=1)").setRequired(true);
        ResourceAdapter service = new ResourceAdapter(e, d);

        CallbackInstance callbackInstance = new CallbackInstance(e, d);
        Component component = m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, callbackInstance, "changed")
            .setImplementation(service)
            .setCallbacks(callbackInstance, "init", "start", "stop", "destroy");
        component.add(new ComponentStateListenerImpl(e));
        m.add(component);
        // wait until the single resource is available
        e.waitForStep(1, 5000);
        // trigger a 'change' in our resource
        provider.change();
        // wait until the changed callback is invoked
        e.waitForStep(2, 5000);
        
        System.out.println("Done!");
        m.clear();
     }
    
    static class ResourceAdapter {
        protected URL m_resource; // injected by reflection.
        final Dependency m_dependency;
        
        ResourceAdapter(Ensure e, Dependency d) {
            m_dependency = d;
        }
    }
        
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider implements ServiceInterface {
        public ServiceProvider(Ensure e) {
        }
        public void invoke() {
        }
    }    
    
    static class CallbackInstance {
        private final Ensure m_ensure;
        private final Dependency m_dependency;
        
        public CallbackInstance(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
        }
        
        void init(Component c) {
            c.add(m_dependency);
            System.out.println("init");
            m_ensure.step(1);
        }
        
        void start() {
            System.out.println("start");
        }
        
        void stop() {
            System.out.println("stop");
        }
        
        void destroy() {
            System.out.println("destroy");
        }
        
        void changed(Component component) {
            m_ensure.step(2);
            System.out.println("Changing the dependencies");
            Dependency oldDependency = m_dependency;
            
            // and add a new dependency
            component.add(component.getDependencyManager().createServiceDependency().setService(ServiceInterface.class, "(id=2)").setRequired(true));
            // remove the old dependency
            component.remove(oldDependency);
            System.out.println("Changed the dependencies");
        }
    }
    
    static class ComponentStateListenerImpl implements ComponentStateListener {
        public ComponentStateListenerImpl(Ensure e) {
        }

        @Override
        public void changed(Component c, ComponentState state) {
            switch (state) {
            case INACTIVE:
                System.out.println("INACTIVE");
                break;
            case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
                System.out.println("INSTANTIATED_AND_WAITING_FOR_REQUIRED");
                break;
            case WAITING_FOR_REQUIRED:
                System.out.println("WAITING_FOR_REQUIRED");
                break;
            case TRACKING_OPTIONAL:
                System.out.println("TRACKING_OPTIONAL");
                break;

            }
        }
    }
}
