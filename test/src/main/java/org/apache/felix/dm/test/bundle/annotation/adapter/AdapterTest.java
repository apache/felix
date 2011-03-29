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
package org.apache.felix.dm.test.bundle.annotation.adapter;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.ServiceRegistration;

public class AdapterTest
{
    public interface S1
    {
        public void run();
    }

    public interface S2
    {
        public void run2();
    }

    public interface S3
    {
        public void run3();
    }

    @Component
    public static class S3Consumer
    {
        @ServiceDependency
        Sequencer m_sequencer;
        private Map<String, String> m_serviceProperties;
        private volatile S3 m_s3;
        
        @ServiceDependency
        void bind(Map<String, String> serviceProperties, S3 s3)
        {
            m_serviceProperties = serviceProperties;
            m_s3 = s3;
        }
        
        @Start
        void start() {
            // The adapter service must inherit from adaptee service properties ...
            if ("value1".equals(m_serviceProperties.get("param1")) // adaptee properties
                && "true".equals(m_serviceProperties.get("adapter"))) // adapter properties
            {
                m_s3.run3();
            }        
        }
    }

    @AdapterService(adapteeService = S1.class, 
                    properties={@Property(name="adapter", value="true")})
    public static class S1ToS3AdapterAutoConfig implements S3
    {
        // This is the adapted service
        protected S1 m_s1;
        
        @ServiceDependency(filter="(name=AdapterAutoConfig)")
        protected Sequencer m_sequencer;

        public void run3()
        {
            m_s1.run();
            m_sequencer.step(3);
        }
    }

    @AdapterService(adapteeService = S1.class, 
                    properties={@Property(name="adapter", value="true")},
                    field="m_s1")
    public static class S1ToS3AdapterAutoConfigField implements S3
    {
        // This is the adapted service
        protected S1 m_s1;
        
        @ServiceDependency(filter="(name=AdapterAutoConfigField)")
        protected Sequencer m_sequencer;

        public void run3()
        {
            m_s1.run();
            m_sequencer.step(3);
        }
    }
    
    @AdapterService(adapteeService = S1.class, 
                    properties={@Property(name="adapter", value="true")},
                    added="bind", 
                    changed="changed",
                    removed="removed")
    public static class S1ToS3AdapterCallback implements S3
    {
        // This is the adapted service
        protected Object m_s1;
        
        @ServiceDependency(filter="(name=AdapterCallback)")
        protected Sequencer m_sequencer;

        void bind(S1 s1)
        {
            m_s1 = s1;
        }
        
        public void run3()
        {
            ((S1) m_s1).run(); // s1 will change its properties here
        }
        
        void changed(S1 s1)
        {
            m_sequencer.step(3);            
        }
        
        @Stop
        void stop() 
        {
            m_sequencer.step(4);            
        }
        
        void removed(S1 s1)
        {
            m_sequencer.step(5);            
        }
    }

    @Component(properties = { @Property(name = "param1", value = "value1") })
    public static class S1Impl implements S1
    {
        @ServiceDependency
        protected Sequencer m_sequencer;

        @ServiceDependency
        protected S2 m_s2;
        
        // Injected when started
        ServiceRegistration m_registration;

        public void run()
        {
            m_sequencer.step(1);
            m_s2.run2();
            Thread update = new Thread() {
                public void run() {
                    m_registration.setProperties(new Hashtable<String, String>() {{ 
                        put("param1", "value1");
                        put("param2", "value2");
                    }});
                }
            };
            update.start();
        }
    }
    
    @Component
    public static class S2Impl implements S2
    {
        @ServiceDependency
        protected Sequencer m_sequencer;
        
        public void run2()
        {
            m_sequencer.step(2);
        }
    }
}
