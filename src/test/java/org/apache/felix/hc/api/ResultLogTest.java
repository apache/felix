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
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class ResultLogTest {

    private ResultLog log;

    @Before
    public void setup() {
        log = new ResultLog();
    }

    @Test
    public void testEmptyLogIsNotOk() {
        assertEquals(Result.Status.WARN, log.getAggregateStatus());
        assertFalse(log.iterator().hasNext());
    }

    @Test
    public void testSetStatusGoingUp() {
        log.add(new ResultLog.Entry(Result.Status.OK, "argh"));
        assertEquals(Result.Status.OK, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.WARN, "argh"));
        assertEquals(Result.Status.WARN, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.CRITICAL, "argh"));
        assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.HEALTH_CHECK_ERROR, "argh"));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, log.getAggregateStatus());
    }

    @Test
    public void testSetStatusGoingDownHCE() {
        log.add(new ResultLog.Entry(Result.Status.HEALTH_CHECK_ERROR, "argh"));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.CRITICAL, "argh"));
        assertEquals(Result.Status.HEALTH_CHECK_ERROR, log.getAggregateStatus());
    }

    @Test
    public void testSetStatusGoingDownCRIT() {
        log.add(new ResultLog.Entry(Result.Status.CRITICAL, "argh"));
        assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.WARN, "argh"));
        assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
    }

    @Test
    public void testSetStatusGoingDownWARN() {
        log.add(new ResultLog.Entry(Result.Status.WARN, "argh"));
        assertEquals(Result.Status.WARN, log.getAggregateStatus());
        log.add(new ResultLog.Entry(Result.Status.OK, "argh"));
        assertEquals(Result.Status.WARN, log.getAggregateStatus());
    }

    @Test
    public void testLogEntries() {
        log.add(new ResultLog.Entry(Result.Status.OK, "ok 1"));
        log.add(new ResultLog.Entry(Result.Status.WARN, "warn 3"));
        log.add(new ResultLog.Entry(Result.Status.CRITICAL, "critical 4"));

        final Iterator<ResultLog.Entry> it = log.iterator();
        assertEquals("INFO ok 1", it.next().toString());
        assertEquals("WARN warn 3", it.next().toString());
        assertEquals("CRITICAL critical 4", it.next().toString());
        assertFalse(it.hasNext());
    }
}
