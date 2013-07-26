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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.runtime.core.handlers.Foo;
import org.apache.felix.ipojo.runtime.core.handlers.IgnoredFoo;
import org.apache.felix.ipojo.runtime.core.services.HandlerBindingTestService;

/**
 * User: guillaume
 * Date: 24/07/13
 * Time: 12:31
 */
@Component
@Provides
@Instantiate
public class HandlerBindingTestComponent implements HandlerBindingTestService {

    @Foo("Bonjour")
    private String greeting;

    @Foo("Welcome")
    private String welcome;

    @IgnoredFoo("Ignored")
    private String ignored;

    @Override
    public String get(final String name) {
        if ("greeting".equals(name)) {
            return greeting;
        }
        if ("welcome".equals(name)) {
            return welcome;
        }
        if ("ignored".equals(name)) {
            return ignored;
        }
        throw new IllegalArgumentException(name + " is not valid");
    }
}
