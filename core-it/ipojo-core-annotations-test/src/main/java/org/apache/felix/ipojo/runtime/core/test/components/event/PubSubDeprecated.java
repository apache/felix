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

package org.apache.felix.ipojo.runtime.core.test.components.event;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.event.Subscriber;
import org.osgi.service.event.Event;


@Component
public class PubSubDeprecated {
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p1", synchronous=true)
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher1;
    
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p2", synchronous=false, topics="foo,bar", data_key="data")
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher2;
    
    @org.apache.felix.ipojo.handlers.event.Publisher(name="p3", synchronous=true, topics="bar")
    org.apache.felix.ipojo.handlers.event.publisher.Publisher publisher3;
    
    @Subscriber(name="s1", data_key="data")
    public void receive1(Object foo) {
        // Nothing
    }
    
    @Subscriber(name="s2", topics="foo,bar", filter="(foo=true)")
    public void receive2(Event foo) {
        // Nothing
    }
    
    
    @Subscriber(name="s3", topics= "foo", data_key="data", data_type="java.lang.String")
    public void receive3(String foo) {
        // Nothing
    }
    
    
    
}
