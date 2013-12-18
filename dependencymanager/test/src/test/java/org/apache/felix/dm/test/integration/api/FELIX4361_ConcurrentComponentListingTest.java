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
package org.apache.felix.dm.test.integration.api;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.integration.common.TestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * Test for FELIX-4361 The DependencyManager.getComponents method failed in a concurrent situation on iterating the
 * result of the method.
 */
@RunWith(PaxExam.class)
public class FELIX4361_ConcurrentComponentListingTest extends TestBase {

    @Test
    public void testConcurrentGetComponentsManipulation() {
        DependencyManager dm = new DependencyManager(context);
        dm.add(dm.createComponent().setImplementation(Object.class));
        Iterator iterator = dm.getComponents().iterator();
        dm.add(dm.createComponent().setImplementation(Object.class));
        iterator.next();
    }

    @Test
    public void testConcurrentGetComponentsMultipleThreads() {
        final DependencyManager m = new DependencyManager(context);
        final AtomicInteger errors = new AtomicInteger(0);
        final AtomicInteger componentsAdded = new AtomicInteger(0);
        final int max = 10000;
        final AtomicBoolean isRunning = new AtomicBoolean(true);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        Runnable readTask = new Runnable() {
            public void run() {
                while (isRunning.get()) {
                    try {
                        List components = m.getComponents();
                        for (Object component : components) {
                            // Just iterating the components should check for concurrent modifications
                        }
                    }
                    catch (Exception ex) {
                        errors.addAndGet(1);
                        ex.printStackTrace();
                    }
                }
            }
        };

        Callable<Boolean> modifyTask = new Callable<Boolean>() {
            public Boolean call() throws Exception {
                try {
                    m.add(m.createComponent().setImplementation(Object.class));
                    componentsAdded.addAndGet(1);
                    return true;
                }
                catch (Exception ex) {
                    return false;
                }
            }
        };

        executorService.submit(readTask);
        for (int i = 0; i < max; i++) {
            executorService.submit(modifyTask);
        }
        isRunning.set(false);
        executorService.shutdown();

        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
        }
        Assert.assertEquals(0, errors.get());
        Assert.assertEquals(max, componentsAdded.get());
    }
}
