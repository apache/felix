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

package org.apache.felix.ipojo.runtime.core.api;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.api.PrimitiveComponentType;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.runtime.core.api.components.HostImpl;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


public class ExternalHandlerTest extends Common {


    private BundleContext context;

    @Before
    public void setUp() {
        context = getContext();
    }

    @Test
    public void createAHost() throws Exception {
        PrimitiveComponentType type = createAWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat(ci.getState(), is(ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat(hd, is(notNullValue()));
    }

    @Test
    public void createDoubleHost() throws Exception {
        PrimitiveComponentType type = createASecondWhiteboardHost();
        ComponentInstance ci = type.createInstance();
        assertThat(ci.getState(), is(ComponentInstance.VALID));
        HandlerDescription hd = ci.getInstanceDescription().getHandlerDescription(Whiteboard.NAMESPACE + ":" + Whiteboard.NAME);
        assertThat(hd, is(notNullValue()));
    }

    private PrimitiveComponentType createAWhiteboardHost() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(HostImpl.class.getName())
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=foo)")
                );
    }

    private PrimitiveComponentType createASecondWhiteboardHost() {
        return new PrimitiveComponentType()
                .setBundleContext(context)
                .setClassName(HostImpl.class.getName())
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=foo)")
                )
                .addHandler(new Whiteboard()
                        .onArrival("arrival")
                        .onDeparture("departure")
                        .setFilter("(foo=bar)")
                        .onModification("modification")
                );
    }

}
