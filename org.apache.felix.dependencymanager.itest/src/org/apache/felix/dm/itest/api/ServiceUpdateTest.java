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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceUpdateTest extends TestBase {
    public void testServiceUpdate() throws Exception {
        final DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a resource provider
        ResourceProvider provider = new ResourceProvider(context, new URL("file://localhost/path/to/file1.txt"));
        // activate it
        m.add(m.createComponent()
            .setImplementation(new ServiceProvider(e))
            .add(m.createServiceDependency()
                .setService(ServiceInterface.class)
                .setRequired(true)
                .setCallbacks("add", "change", "remove")
            )
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
        CallbackInstance callbackInstance = new CallbackInstance(e);
        Hashtable<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put("number", "1");
        Component component = m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, callbackInstance, "changed")
            .setImplementation(new ResourceAdapter(e))
            .setInterface(ServiceInterface.class.getName(), serviceProps)
            .setCallbacks(callbackInstance, "init", "start", "stop", "destroy");
        m.add(component);
        // wait until the single resource is available
        e.waitForStep(1, 5000);
        // wait until the component gets the dependency injected
        e.waitForStep(2, 5000);
        // trigger a 'change' in our resource
        provider.change();
        // wait until the changed callback is invoked
        e.waitForStep(3, 5000);        
        // wait until the changed event arrived at the component
        e.waitForStep(4, 5000);
        System.out.println("Done!");
        m.clear();
     }
    
    static class ResourceAdapter implements ServiceInterface {
        protected URL m_resource; // injected by reflection.
        
        ResourceAdapter(Ensure e) {
        }

        public void invoke() {
            // TODO Auto-generated method stub
            
        }
    }
        
    static interface ServiceInterface {
        public void invoke();
    }

    static class ServiceProvider {
        private final Ensure m_ensure;
        public ServiceProvider(Ensure e) {
            m_ensure = e;
        }
        void add(ServiceInterface i) {
            m_ensure.step(2);
        }
        void change(ServiceInterface i) {
            System.out.println("Change...");
            m_ensure.step(4);
        }
        void remove(ServiceInterface i) {
            System.out.println("Remove...");
        }
    }    
    
    static class CallbackInstance {
        private final Ensure m_ensure;
        public CallbackInstance(Ensure e) {
            m_ensure = e;
        }
        
        void init() {
            System.out.println("init");
        }
        void start() {
            System.out.println("start");
            m_ensure.step(1);
        }
        void stop() {
            System.out.println("stop");
        }
        void destroy() {
            System.out.println("destroy");
        }
        void changed(Component component) {
            System.out.println("resource changed");
            m_ensure.step(3);
            
            Properties newProps = new Properties();
            // update the component's service properties
            Dictionary<String, String> dict = component.getServiceProperties();
            Enumeration<String> e = dict.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                String value = dict.get(key);
                newProps.setProperty(key, value);
            }
            newProps.setProperty("new-property", "2");
            component.getServiceRegistration().setProperties(newProps);
        }
    }
}
