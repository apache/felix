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
package org.apache.felix.dm.test.bundle.annotation.factoryconfadapter;

import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

@Component
public class ServiceClient
{
    @ServiceDependency(changed="changeServiceProvider")
    void addServiceProvider(Map props, ServiceInterface si) {
        // props should contain foo=bar, foo2=bar2
        if (! "bar".equals(props.get("foo")))
        {
            throw new IllegalArgumentException("configuration does not contain foo=bar: " + props);
        }
        if (! "bar2".equals(props.get("foo2")))
        {
            throw new IllegalArgumentException("configuration does not contain foo2=bar2: " + props);
        }
        si.doService();
    }
    
    void changeServiceProvider(Map props, ServiceInterface si) 
    {
        // props should contain foo=bar, foo2=bar2_modified
        if (! "bar".equals(props.get("foo")))
        {
            throw new IllegalArgumentException("configuration does not contain foo=bar: " + props);
        }
        if (! "bar2_modified".equals(props.get("foo2")))
        {
            throw new IllegalArgumentException("configuration does not contain foo2=bar2: " + props);
        }
        si.doService();
    }
}
