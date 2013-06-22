/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.components;

import org.apache.felix.ipojo.configuration.Configuration;
import org.apache.felix.ipojo.configuration.Instance;

import java.util.Properties;

import static org.apache.felix.ipojo.configuration.Instance.instance;

/**
 * Simple configuration
 */
@Configuration
public class ConfigureTwoInstances {

    // Declare an instance of MyComponent named myInstance
    Instance myInstance1 = instance().of(MyComponent.class)
            .with("floating").setto("1.0")
            .with("message").setto("foo")
            .with("bool").setto(true)
            .with("number").setto(1l)
            .with("integer").setto(1)
            .with("props").setto(new Properties());

    // Declare an instance of MyComponent named hello
    Instance myInstance2 = instance().of(MyComponent.class)
            .named("hello")
            .with("floating").setto("1.0")
            .with("message").setto("foo")
            .with("bool").setto(true)
            .with("number").setto(1l)
            .with("integer").setto(1)
            .with("props").setto(new Properties());
}
