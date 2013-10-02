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

import org.apache.felix.ipojo.extender.queue.Job;
import org.osgi.framework.Bundle;

/**
 * User: guillaume
 * Date: 01/10/13
 * Time: 17:51
 */
public class EmptyJob<T> implements Job<T> {

    private final Bundle m_bundle;
    private final String m_type;

    public EmptyJob() {
        this(null);
    }

    public EmptyJob(final Bundle bundle) {
        this(bundle, "test");
    }

    public EmptyJob(Bundle bundle, String type) {
        m_bundle = bundle;
        m_type = type;
    }

    public String getJobType() {
        return m_type;
    }

    public Bundle getBundle() {
        return m_bundle;
    }

    public T call() throws Exception {
        return null;
    }
}
