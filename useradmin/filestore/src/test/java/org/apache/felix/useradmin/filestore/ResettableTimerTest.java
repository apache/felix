/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.felix.useradmin.filestore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * Test case for {@link ResettableTimer}.
 */
public class ResettableTimerTest extends TestCase {

    private ResettableTimer m_timer;
    private CountDownLatch m_latch;

    /**
     * Tests that a executor service is mandatory.
     */
    public void testExecutorSerivceIsMandatory() throws Exception {
        Runnable task = createStubTask();

        try {
            new ResettableTimer(null, task, 10, TimeUnit.HOURS);
            fail("IllegalArgumentException expected!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Tests that multiple calls to {@link ResettableTimer#schedule()} causes 
     * pending tasks to be cancelled.
     */
    public void testScheduleCancelsPendingTasksOk() throws Exception {
        final AtomicInteger m_counter = new AtomicInteger(0);

        Runnable task = new Runnable() {
            public void run() {
                m_counter.incrementAndGet();
            }
        };
        
        m_timer = new ResettableTimer(task, 100, TimeUnit.MILLISECONDS);
        m_timer.schedule();

        TimeUnit.MILLISECONDS.sleep(75);
        
        Future f = m_timer.schedule();
        f.get();

        assertEquals(1, m_counter.get());
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.ResettableTimer#schedule()}.
     */
    public void testScheduleMultipleTasksOk() throws Exception {
        final AtomicInteger m_counter = new AtomicInteger(0);

        Runnable task = new Runnable() {
            public void run() {
                m_counter.incrementAndGet();
            }
        };

        m_timer = new ResettableTimer(task, 100, TimeUnit.MILLISECONDS);
        
        Future f = m_timer.schedule();
        f.get();

        f = m_timer.schedule();
        f.get();

        assertEquals(2, m_counter.get());
    }

    /**
     * Tests that a task is invoked as single shot.
     */
    public void testScheduleSingleShotOk() throws Exception {
        m_latch = new CountDownLatch(1);

        Runnable task = new Runnable() {
            public void run() {
                m_latch.countDown();
            }
        };

        m_timer = new ResettableTimer(task, 100, TimeUnit.MILLISECONDS);
        m_timer.schedule();

        assertTrue(m_latch.await(200, TimeUnit.MILLISECONDS));
    }

    /**
     * Test method for {@link org.apache.felix.useradmin.impl.ResettableTimer#shutDown()}.
     */
    public void testShutDownOk() throws Exception {
        final AtomicInteger m_counter = new AtomicInteger(0);

        Runnable task = new Runnable() {
            public void run() {
                m_counter.incrementAndGet();
            }
        };
        
        m_timer = new ResettableTimer(task, 100, TimeUnit.MILLISECONDS);
        m_timer.schedule();

        TimeUnit.MILLISECONDS.sleep(75);
        
        m_timer.schedule();
        m_timer.shutDown();

        assertEquals(1, m_counter.get());
    }

    /**
     * Tests that a task is mandatory.
     */
    public void testTaskIsMandatory() throws Exception {
        try {
            new ResettableTimer(null, 10, TimeUnit.HOURS);
            fail("IllegalArgumentException expected!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Tests that a timeout cannot be zero.
     */
    public void testTimeoutCannotBeNegative() throws Exception {
        Runnable task = createStubTask();

        try {
            new ResettableTimer(task, -1, TimeUnit.MILLISECONDS);
            fail("IllegalArgumentException expected!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Tests that a timeout cannot be zero.
     */
    public void testTimeoutCannotBeZero() throws Exception {
        Runnable task = createStubTask();

        try {
            new ResettableTimer(task, 0, TimeUnit.MILLISECONDS);
            fail("IllegalArgumentException expected!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * Tests that a time unit is mandatory.
     */
    public void testTimeUnitIsMandatory() throws Exception {
        Runnable task = createStubTask();

        try {
            new ResettableTimer(task, 10, null);
            fail("IllegalArgumentException expected!");
        } catch (Exception e) {
            // Ok; expected
        }
    }

    /**
     * @return a {@link Runnable} that does nothing, never <code>null</code>.
     */
    private Runnable createStubTask() {
        return new Runnable() {
            public void run() {
                // No-op
            }
        };
    }
}
