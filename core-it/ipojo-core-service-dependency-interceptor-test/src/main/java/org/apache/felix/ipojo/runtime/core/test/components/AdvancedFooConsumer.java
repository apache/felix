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

package org.apache.felix.ipojo.runtime.core.test.components;

import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.Enhanced;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.apache.felix.ipojo.runtime.core.test.services.Setter;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

/**
 * A component consuming FooService with a filter requiring interception, and impacting the ranking.
 */
@Component(immediate = true)
@Provides
public class AdvancedFooConsumer implements CheckService, Setter {

    @Requires(id= "foo", policy = BindingPolicy.DYNAMIC_PRIORITY, proxy = false, filter= "(intercepted=true)",
            optional = true, nullable = false)
    private FooService foo;

    private Map<String, Object> props;

    // Will be read by the interceptor to inject only matching provider.
    @Property(value = "0")
    private int grade = 0;

    @Override
    public boolean check() {
        return foo.foo();
    }

    @Override
    public Dictionary getProps() {
        Properties properties =  new Properties();
        properties.put("props", props);
        if (foo != null) {
            properties.put("grade", foo.getGrade());
            if (foo instanceof Enhanced) {
                properties.put("enhanced", ((Enhanced) foo).enhance());
            }
        }
        return properties;
    }

    @Bind(id="foo")
    public void bind(FooService foo, Map<String, Object> properties) {
        props = properties;
    }

    @Modified(id = "foo")
    public void modified(FooService foo, Map<String, Object> properties) {
        props = properties;
    }

    @Override
    public void set(String newValue) {
        System.out.println("Setting grade to " + newValue);
        grade = Integer.parseInt(newValue);
    }
}
