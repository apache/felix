/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
public class AsyncHealthCheckIT {

    @Inject
    private HealthCheckExecutor executor;

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        return U.config();
    }

    final AtomicInteger counter = new AtomicInteger(Integer.MIN_VALUE);

    final static int MAX_VALUE = 12345678;

    class TestHC implements HealthCheck {
        @Override
        public Result execute() {
            final int v = counter.incrementAndGet();
            return new Result(v > MAX_VALUE ? Result.Status.WARN : Result.Status.OK, "counter is now " + v);
        }
    }

    private ServiceRegistration registerAsyncHc(HealthCheck hc, String id, Object async, int stickyMinutes) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, "async_HC_" + id);
        props.put(HealthCheck.TAGS, id);
        if (async instanceof String) {
            props.put(HealthCheck.ASYNC_CRON_EXPRESSION, async);
        } else if (async instanceof Long) {
            props.put(HealthCheck.ASYNC_INTERVAL_IN_SEC, async);
        }

        if (stickyMinutes > 0) {
            props.put(HealthCheck.WARNINGS_STICK_FOR_MINUTES, stickyMinutes);
        }

        final ServiceRegistration result = bundleContext.registerService(HealthCheck.class.getName(), hc, props);

        // Wait for HC to be registered
        U.expectHealthChecks(1, executor, id);

        return result;
    }

    private void assertStatus(String id, Result.Status expected, long maxMsec, String msg) throws InterruptedException {
        final long timeout = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < timeout) {
            final Result.Status actual = executor.execute(HealthCheckSelector.tags(id)).get(0).getHealthCheckResult().getStatus();
            if (actual == expected) {
                return;
            }
            Thread.sleep(100L);
        }
        fail("Did not get status " + expected + " after " + maxMsec + " msec " + msg);
    }

    @Test
    public void testAsyncHealthCheckExecution() throws InterruptedException {

        final String id = UUID.randomUUID().toString();
        final HealthCheck hc = new TestHC();
        final ServiceRegistration reg = registerAsyncHc(hc, id, "*/1 * * * * ?", 0);
        final long maxMsec = 5000L;

        try {
            // Reset the counter and check that HC increments it without explicitly calling the executor
            {
                counter.set(0);
                final long timeout = System.currentTimeMillis() + maxMsec;
                while (System.currentTimeMillis() < timeout) {
                    int currentVal = counter.get();
                    if (currentVal > 0) {
                        break;
                    }
                    Thread.sleep(100L);
                }
                assertTrue("Expecting counter to be incremented", counter.get() > 0);
            }

            // Verify that we get the right log
            final String msg = executor.execute(HealthCheckSelector.tags(id)).get(0).getHealthCheckResult().iterator().next().getMessage();
            assertTrue("Expecting the right message: " + msg, msg.contains("counter is now"));

            // And verify that calling executor lots of times doesn't increment as much
            final int previous = counter.get();
            final int n = 100;
            for (int i = 0; i < n; i++) {
                executor.execute(HealthCheckSelector.tags(id));
            }
            assertTrue("Expecting counter to increment asynchronously", counter.get() < previous + n);

            // Verify that results are not sticky
            assertStatus(id, Result.Status.OK, maxMsec, "before WARN");
            counter.set(MAX_VALUE + 1);
            assertStatus(id, Result.Status.WARN, maxMsec, "right after WARN");
            counter.set(0);
            assertStatus(id, Result.Status.OK, maxMsec, "after resetting counter");

        } finally {
            reg.unregister();
        }

    }

    @Test
    public void testAsyncHealthCheckExecutionWithInterval() throws InterruptedException {

        final String id = UUID.randomUUID().toString();
        final HealthCheck hc = new TestHC();
        final ServiceRegistration reg = registerAsyncHc(hc, id, new Long(2), 0);
        final long maxMsec = 5000L;

        try {
            // Reset the counter and check that HC increments it without explicitly calling the executor
            {
                counter.set(0);
                final long timeout = System.currentTimeMillis() + maxMsec;
                while (System.currentTimeMillis() < timeout) {
                    int currentVal = counter.get();
                    if (currentVal > 0) {
                        break;
                    }
                    Thread.sleep(100L);
                }
                assertTrue("Expecting counter to be incremented", counter.get() > 0);
            }

            // Verify that we get the right log
            final String msg = executor.execute(HealthCheckSelector.tags(id)).get(0).getHealthCheckResult().iterator().next().getMessage();
            assertTrue("Expecting the right message: " + msg, msg.contains("counter is now"));

        } finally {
            reg.unregister();
        }

    }

    @Test
    public void testAsyncHealthCheckWithStickyResults() throws InterruptedException {
        final String id = UUID.randomUUID().toString();
        final HealthCheck hc = new TestHC();
        final long maxMsec = 5000L;
        final int stickyMinutes = 1;
        final ServiceRegistration reg = registerAsyncHc(hc, id, "*/1 * * * * ?", stickyMinutes);

        try {
            assertStatus(id, Result.Status.OK, maxMsec, "before WARN");
            counter.set(MAX_VALUE + 1);
            assertStatus(id, Result.Status.WARN, maxMsec, "right after WARN");
            counter.set(0);

            // Counter should be incremented after a while, and in range, but with sticky WARN result
            final long timeout = System.currentTimeMillis() + maxMsec;
            boolean ok = false;
            while (System.currentTimeMillis() < timeout) {
                if (counter.get() > 0 && counter.get() < MAX_VALUE) {
                    ok = true;
                    break;
                }
                Thread.sleep(100L);
            }

            assertTrue("expecting counter to be incremented", ok);
            assertStatus(id, Result.Status.WARN, maxMsec, "after resetting counter, expecting sticky result");

        } finally {
            reg.unregister();
        }
    }

}
