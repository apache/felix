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
package org.apache.felix.http.base.internal.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides service ids for services not provided through the service registry.
 * <p>
 * All provided ids are unique negative {@code long}s.
 */
public enum InternalIdFactory
{
    INSTANCE;

    /** -1 is reserved for the http service servlet context. */
    private final AtomicLong idCounter = new AtomicLong(-1);

    public long next()
    {
        return idCounter.decrementAndGet();
    }
}