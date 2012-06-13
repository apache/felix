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
package org.apache.felix.scrplugin.description;

import java.lang.reflect.Method;

/**
 * A method description describes a reference to a method,
 * this can either just be the name or a real method object.
 */
public class MethodDescription {

    private final String name;

    private final Method method;

    public MethodDescription(final String name) {
        this.name = name;
        this.method = null;
    }

    public MethodDescription(final Method method) {
        this.name = method.getName();
        this.method = method;
    }

    public String getName() {
        return this.name;
    }

    public Method getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "MethodDescription [name=" + name + ", method=" + method + "]";
    }
}
