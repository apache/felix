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
package org.apache.felix.dm.test.bundle.annotation.felix4050;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Component
public class S {
    @ServiceDependency
    volatile Sequencer m_sequencer;

    @Inject
    volatile org.apache.felix.dm.Component _component;

    volatile A m_a;
    volatile B m_b;

    void bind(A a) {
        System.out.println("bind(A): " + a);
        m_a = a;
    }

    @ServiceDependency(name = "B")
    void bind(B b) {
        System.out.println("bind(B): " + b);
        m_b = b;
    }

    @Init
    Map init() {
        m_sequencer.step(1);

        List l = new ArrayList();
        l.add(_component.getDependencyManager().createServiceDependency().setService(A.class)
                .setRequired(true).setCallbacks("bind", null).setInstanceBound(true));
        _component.add(l);

        return new HashMap() {
            {
                put("B.required", "true");
                put("B.filter", "(type=b2)");
            }
        };
    }

    @Start
    void start() {
        if (m_a == null) {
            throw new RuntimeException("A not injected");
        }
        if (m_b == null) {
            throw new RuntimeException("B not injected");
        }
        m_sequencer.step(2);
        m_b.run(); // step(3)
    }

    @Stop
    void stop() {
        m_sequencer.step(4);
    }

    @Destroy
    void destroy() {
        m_sequencer.step(5);
    }
}
