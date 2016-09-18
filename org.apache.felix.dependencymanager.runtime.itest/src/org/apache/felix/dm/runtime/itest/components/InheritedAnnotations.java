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
package org.apache.felix.dm.runtime.itest.components;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;
import org.junit.Assert;

public class InheritedAnnotations
{
    public final static String ENSURE = "InheritedAnnotations";
    
    @Component(provides=Service1.class)
    public static class Service1 {       
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        Ensure m_ensure;

    }
    
    @Component(provides=Service2.class)
    public static class Service2 {
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        Ensure m_ensure;
    }
    
    public static class Base {
        @ServiceDependency(filter="(name=" + ENSURE + ")")
        protected Ensure m_ensure;
        
        protected Service1 m_s1;
        
        @ServiceDependency
        void bind(Service1 s1) {
            m_s1 = s1;
        }

        @Init
        void init() {
            Assert.assertNotNull(m_ensure);
            Assert.assertNotNull(m_s1);
            m_ensure.step(1);
        }
    }
    
    @Component
    public static class Child extends Base {
        private Service2 m_s2;

        @ServiceDependency
        void bind(Service2 s2) { 
            m_s2 = s2;
        }

        @Start
        void start() {    
            Assert.assertNotNull(m_ensure);
            Assert.assertNotNull(m_s1);
            Assert.assertNotNull(m_s2);
            m_ensure.step(2);
        }        
    }

}
