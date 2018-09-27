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
package org.apache.felix.cm.impl;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class UpdateThreadTest
{
    private static final int COUNT = 10;

    @Test
    public void testUpdateThread() throws Exception {
        final UpdateThread updateThread = new UpdateThread(null, "name");
        updateThread.start();
        try {
            final CountDownLatch counter = new CountDownLatch(COUNT);
            for (int i = 0; i < COUNT; ++i) {
                updateThread.schedule(new Runnable() {
                    @Override
                    public void run() {
                        counter.countDown();
                    }
                });
            }
            assertTrue(counter.await(1L, TimeUnit.MINUTES));
        } finally {
            updateThread.terminate();
        }
    }
}
