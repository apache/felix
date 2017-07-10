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
package org.apache.felix.http.base.internal.runtime;

import static java.lang.Integer.signum;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AbstractInfoOrderingTest
{
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Expected value must be non-negative
                // negative values are tested by symmetry

                // same service id (note: rank must be identical)
                { 0, 0, 0, 1, 1 },
                { 0, 1, 1, 0, 0 },
                { 0, -1, -1, -1, -1 },
                // rank has priority
                { 1, 0, 1, 1, 0 },
                { 1, 0, 1, -1, 0 },
                { 1, -1, 0, 1, 0 },
                { 1, -1, 0, -1, 0 },
                // same rank
                { 1, 1, 1, 2, 1 },
                { 1, -1, -1, -1, 1 },
                { 1, 0, 0, 2, 1 },
                { 1, 0, 0, 1, 0 },
                { 1, 0, 0, -1, 1 },
                { 1, 0, 0, -1, 1 },
                { 1, 0, 0, -2, 1 },
                { 1, 0, 0, -1, 2 },
                { 1, 0, 0, -1, 1 },
                { 1, 0, 0, -2, -1 }
           });
    }

    private final int expected;
    private final TestInfo testInfo;
    private final TestInfo other;

    public AbstractInfoOrderingTest(int expected,
            int testRank, int otherRank, long testId, long otherId)
    {
        if (expected < 0)
        {
            throw new IllegalArgumentException("Expected values must be non-negative.");
        }
        this.expected = expected;
        testInfo = new TestInfo(testRank, testId);
        other = new TestInfo(otherRank, otherId);
    }

    @Test
    public void ordering()
    {
        assertEquals(expected, signum(testInfo.compareTo(other)));
        if ( expected != 0 )
        {
            final List<AbstractInfo> list = new ArrayList<AbstractInfo>();
            list.add(testInfo);
            list.add(other);
            Collections.sort(list);
            if ( expected == -1 )
            {
                assertEquals(testInfo, list.get(0));
                assertEquals(other, list.get(1));
            }
            else
            {
                assertEquals(testInfo, list.get(1));
                assertEquals(other, list.get(0));
            }
        }
    }

    @Test
    public void orderingSymetry()
    {
        assertTrue(signum(testInfo.compareTo(other)) == -signum(other.compareTo(testInfo)));
    }

    @Test
    public void orderingTransitivity()
    {
        assertTrue(testInfo.compareTo(other) >= 0);

        TestInfo three = new TestInfo(0, 0);

        // three falls in between the two other points
        if (testInfo.compareTo(three) >= 0 && three.compareTo(other) >= 0)
        {
            assertTrue(testInfo.compareTo(other) >= 0);
        }
        // three falls below the two other points
        else if (testInfo.compareTo(other) >= 0 && other.compareTo(three) >= 0)
        {
            assertTrue(testInfo.compareTo(three) >= 0);
        }
        // three falls above the two other points
        else if (three.compareTo(testInfo) >= 0 && testInfo.compareTo(other) >= 0)
        {
            assertTrue(three.compareTo(other) >= 0);
        }
        else
        {
            fail("Since testInfo >= other, one of the above cases must match");
        }
    }

    private static class TestInfo extends AbstractInfo<TestInfo>
    {
        public TestInfo(int ranking, long serviceId)
        {
            super(ranking, serviceId);
        }
    }
}
