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

import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.itest.util.Ensure;

/**
 * Verifies ServiceDependencyservice properties propagation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"rawtypes"})
public class PropagateAnnotation {
    public final static String ENSURE = "PropagateAnnotation";
    
    @Component
    public static class Consumer {
        private volatile Map m_producerProps;

        @ServiceDependency
        void bind(Map props, Producer producer) {
            m_producerProps = props;
        }

        @ServiceDependency(filter = "(name=" + ENSURE + ")")
        volatile Ensure m_sequencer;

        @Start
        void start() {
            m_sequencer.step(1);
            if ("bar".equals(m_producerProps.get("foo"))) {
                m_sequencer.step(2);
            }
            if ("bar2".equals(m_producerProps.get("foo2"))) {
                m_sequencer.step(3);
            }
        }
    }

    @Component(provides = {Producer.class}, properties = {@Property(name = "foo", value = "bar")})
    public static class Producer {
        @ServiceDependency(propagate = true)
        volatile Producer2 m_producer;
    }

    @Component(provides = {Producer2.class}, properties = {@Property(name = "foo2", value = "bar2")})
    public static class Producer2 {
    }
}
