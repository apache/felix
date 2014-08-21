/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.eventadmin.ittests;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class Listener implements EventHandler {

    private final ServiceRegistration reg;

    private final Object payload;

    private final AbstractTest test;

    public Listener(final BundleContext ctx, final AbstractTest test, final String topic, final Object payload) {
        this(ctx, test, topic != null ? new String[] {topic} : null, payload);
    }

    public Listener(final BundleContext ctx, final AbstractTest test, final String[] topics, final Object payload) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        if ( topics != null ) {
            props.put("event.topics", topics);
        } else {
            props.put("event.topics", "*");
        }
        this.test = test;
        this.reg = ctx.registerService(EventHandler.class.getName(), this, props);
        this.payload = payload;
    }

    public void dispose() {
        this.reg.unregister();
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        this.test.handleEvent(event, this.payload);
    }
}
