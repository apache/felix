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

package org.apache.felix.ipojo.extender.internal.queue.callable;

import java.util.concurrent.Callable;

import org.apache.felix.ipojo.extender.queue.Job;
import org.osgi.framework.Bundle;

/**
* A dummy job.
*/
public class StringCallable extends EmptyJob<String> {

    private final String m_hello;

    public StringCallable() {
        this((Bundle) null, "hello");
    }

    public StringCallable(String value) {
        this((Bundle) null, value);
    }

    public StringCallable(String type, String value) {
        this(null, type, value);
    }

    public StringCallable(Bundle bundle) {
        this(bundle, "hello");
    }

    public StringCallable(Bundle bundle, String hello) {
        super(bundle);
        m_hello = hello;
    }

    public StringCallable(Bundle bundle, String type, String hello) {
        super(bundle, type);
        m_hello = hello;
    }

    public String call() throws Exception {
        return m_hello;
    }
}
