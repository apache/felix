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

package org.apache.felix.ipojo.runtime.core.test.components.components;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

import java.util.Properties;

/**
 * A component publishing a service with a static service property.
 */
@Component
@Provides(properties = {
    @StaticServiceProperty(name="property", value="value", type = "java.lang.String")
})
@Instantiate(name="instanceWithProperties")
// We must avoid having such static properties:
// Fixed in https://issues.apache.org/jira/browse/FELIX-4053
//@StaticServiceProperty(name="property", value="value", type = "java.lang.String")
public class ComponentWithProperties implements FooService{

    // The implementation is meaningless.

    @Override
    public boolean foo() {
        return false;
    }

    @Override
    public Properties fooProps() {
        return null;
    }

    @Override
    public Boolean getObject() {
        return null;
    }

    @Override
    public boolean getBoolean() {
        return false;
    }

    @Override
    public int getInt() {
        return 0;
    }

    @Override
    public long getLong() {
        return 0;
    }

    @Override
    public double getDouble() {
        return 0;
    }
}
