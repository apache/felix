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
package org.apache.felix.dm.benchmark.scenario;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Helper class containing misc functions, and constants.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Helper {
    /**
     * Activate this flag for debugging.
     */
    private final static boolean DEBUG = false;

    /** 
     * Generator used to create unique identifiers.
     */
    private final static AtomicLong m_idGenerator = new AtomicLong();

    /**
     * Threadpool which can be optionally used by parallel scenarios.
     */
    private final static int CORES = Runtime.getRuntime().availableProcessors();
    private final static ForkJoinPool TPOOL = new ForkJoinPool(CORES);
    
    /**
     * Get the threadpool, possibly needed by some scenario supporting parallel mode
     */
    public static ForkJoinPool getThreadPool() {
        return TPOOL;
    }
    
    /**
     * Display some debug messages.
     */
    public static void debug(Supplier<String> message) {
        if (DEBUG) {
            System.out.println(message.get());
        }
    }

    /**
     * Generates a unique id.
     */
    public static long generateId() {
        return m_idGenerator.incrementAndGet();
    }
}
