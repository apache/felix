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

package org.apache.felix.ipojo.runtime.core.test.components.exceptions;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;

import java.util.Properties;

/**
 * A component supporting exception when no services are available.
 */
@Component
@Provides
public class ExceptionAwareCheckServiceProvider implements CheckService {

    @Requires(id="foo", optional = true, exception = NoServiceException.class)
    private FooService fs;
    private int bind;
    private int unbind;

    @Bind(id = "foo")
    public void bindFoo(FooService fs) {
        if (fs != null) {
            bind++;
        }
    }

    public void unbindFoo(FooService fs) {
        if (fs != null) {
            unbind++;
        }
    }

    @Override
    public boolean check() {
        try {
            fs.foo();
            return true; // always return true, to detect the exception case.
        } catch (NoServiceException e) {
            return false;
        }
    }

    @Override
    public Properties getProps() {
        Properties properties = new Properties();
        properties.put("bind", bind);
        properties.put("unbind", unbind);
        return properties;
    }
}
