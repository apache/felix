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
public class ResourceAdapterDependencyAddAndRemoveTest extends TestBase {
    public void testBasicResourceAdapter() throws Exception {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();

        // create and add a service provider
        m.add(m.createComponent()
                .setInterface(ServiceInterface.class.getName(), null)
                .setImplementation(new ServiceProvider(e)));
        
        // create and add a resource provider
        ResourceProvider provider = new ResourceProvider(context, new URL("file://localhost/path/to/file1.txt"));
        m.add(m.createComponent()
                .setImplementation(provider)
                .add(m.createServiceDependency()
                    .setService(ResourceHandler.class)
                    .setCallbacks("add", "remove"))
                );
        
        // create a resource adapter for our single resource
        // note that we can provide an actual implementation instance here because there will be only one
        // adapter, normally you'd want to specify a Class here
        // also, create a callback instance which will be used for both callbacks on resource changes and
        // life cycle callbacks on the adapters themselves
        
        Dependency d = m.createServiceDependency()
            .setService(ServiceInterface.class)
            .setRequired(true);
        CallbackInstance callbackInstance = new CallbackInstance(e, d);
        Component component = m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, callbackInstance, "changed")
            .setImplementation(new ResourceAdapter(e))
            .setCallbacks(callbackInstance, "init", "start", "stop", "destroy");
        
        // add the resource adapter
        m.add(component);
        
        // wait until the single resource is available (the adapter has been started)
        e.waitForStep(1, 5000);
        // trigger a 'change' in our resource
        provider.change();
        // wait until the changed callback is invoked
        e.waitForStep(2, 5000);
        // and has completed (ensuring no "extra" steps are invoked in the mean time)
        e.waitForStep(3, 5000);
                
        // remove the resource adapter again
        // add a component state listener, in order to track resource adapter destruction
        component.add(new ComponentStateListenerImpl(e));
        m.remove(component);
        
        // wait for the stopped callback in the state listener
        e.waitForStep(4, 5000);
        m.clear();
     }
    
    static class ResourceAdapter {
        protected URL m_resource; // injected by reflection.
        
        ResourceAdapter(Ensure e) {
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
    
    class CallbackInstance {
        private final Dependency m_dependency;
        private final Ensure m_ensure;
        
        
        public CallbackInstance(Ensure e, Dependency d) {
            m_ensure = e;
            m_dependency = d;
        }
        
        void init(Component c) {
            debug("CallbackInstance.init");
            c.add(m_dependency);
        }
        
        void start() {
            debug("CallbackInstance.start");
            m_ensure.step(1);
        }
        
        void stop() {
            debug("CallbackInstance.stop");
        }
        
        void destroy() {
            debug("CallbackInstance.destroy");
        }
        
        void changed(Component component) {
            m_ensure.step(2);
            Dependency oldDependency = m_dependency;
            // and add a new dependency
            component.add(component.getDependencyManager().createServiceDependency().setService(ServiceInterface.class).setRequired(true));
            // remove the old dependency
            component.remove(oldDependency);
            debug("CallbackInstance.changed the dependencies");
            m_ensure.step(3);
        }
    }
    
    class ComponentStateListenerImpl implements ComponentStateListener {
        
        private final Ensure m_ensure;
        
        public ComponentStateListenerImpl(Ensure e) {
            this.m_ensure = e;
        }
        
        public void changed(Component c, ComponentState state) {
            debug("ComponentStateListenerImpl.changed: state=%s", state);
            switch (state) {
            case INACTIVE:
                System.out.println("stopped");
                m_ensure.step(4);
            default:
            }
        }
    }
}
