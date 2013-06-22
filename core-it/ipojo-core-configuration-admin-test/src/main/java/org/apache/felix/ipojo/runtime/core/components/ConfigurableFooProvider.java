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

import org.apache.felix.ipojo.runtime.core.services.FooService;

import java.util.Properties;

public class ConfigurableFooProvider implements FooService {

    private String message; // Configurable property
    private int invokeCount = 0;

    public void setMessage(String message) {
        System.err.println("=== Set message to " + message);
        this.message = message;
        invokeCount++;
    }

    public boolean foo() {
        return true;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        if (message == null) {
            props.put("message", "NULL");
        } else {
            props.put("message", message);
        }
        props.put("count", new Integer(invokeCount));
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return invokeCount;
    }

    public int getInt() {
        return invokeCount;
    }

    public long getLong() {
        return invokeCount;
    }

    public Boolean getObject() {
        return null;
    }

}
