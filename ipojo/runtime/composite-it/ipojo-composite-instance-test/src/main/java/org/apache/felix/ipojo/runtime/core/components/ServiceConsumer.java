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

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.Service;

import java.util.Properties;

public class ServiceConsumer implements CheckService {

    private Service service;
    private Properties props = new Properties();

    public ServiceConsumer() {
        props.put("1", new Integer(service.count()));
        props.put("2", new Integer(service.count()));
        props.put("3", new Integer(service.count()));
    }

    public boolean check() {
        return service.count() > 0;
    }

    public Properties getProps() {
        return props;
    }

}
