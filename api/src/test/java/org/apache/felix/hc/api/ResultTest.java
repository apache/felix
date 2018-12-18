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
package org.apache.felix.hc.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ResultTest {

    private final AtomicInteger counter = new AtomicInteger();

    private void assertSingleResult(Result.Status toSet, Result.Status expected, boolean expectOk) {
        final String msg = "test " + counter.incrementAndGet();
        final Result r = new Result(toSet, msg);
        assertEquals(expected, r.getStatus());
        assertEquals(expectOk, r.isOk());
        assertTrue(r.iterator().hasNext());
        assertEquals(toSet.toString() + " " + msg, r.iterator().next().toString());
    }

    @Test
    public void testSingleResult() {
        assertSingleResult(Result.Status.OK, Result.Status.OK, true);
        assertSingleResult(Result.Status.WARN, Result.Status.WARN, false);
        assertSingleResult(Result.Status.CRITICAL, Result.Status.CRITICAL, false);
        assertSingleResult(Result.Status.HEALTH_CHECK_ERROR, Result.Status.HEALTH_CHECK_ERROR, false);
    }

    @Test
    public void testLog() {
        final ResultLog log = new ResultLog();
        log.add(new ResultLog.Entry(Result.Status.OK, "some msg"));
        log.add(new ResultLog.Entry(Result.Status.WARN, "problematic condition"));

        final Result result = new Result(log);
        assertEquals(Result.Status.WARN, result.getStatus());

        final StringBuilder sb = new StringBuilder();
        for (ResultLog.Entry e : result) {
            sb.append(e.toString()).append("#");
        }
        assertEquals("OK some msg#WARN problematic condition#", sb.toString());
    }
}
