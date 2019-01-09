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
package org.apache.felix.hc.generalchecks.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class SimpleConstraintCheckerTest {

    private final SimpleConstraintChecker checker = new SimpleConstraintChecker();

    @Before
    public void setup() {
    }

    @Test
    public void testStringEquals() {
        final String s = "test_" + System.currentTimeMillis();
        assertTrue(checker.check(s, s));
    }

    @Test
    public void testStringNotEquals() {
        final String s = "test_" + System.currentTimeMillis();
        assertFalse(checker.check(s, "something else"));
    }

    @Test
    public void testFiveEquals() {
        final String s = "5";
        assertTrue(checker.check(s, s));
    }

    @Test
    public void testIntTwelveEquals() {
        assertTrue(checker.check(12, "12"));
    }

    @Test
    public void testIntTwelveGreaterThan() {
        assertTrue(checker.check(12, "> 11"));
    }

    @Test
    public void testFiveNotEquals() {
        assertFalse(checker.check("5", "foo"));
    }

    @Test
    public void testNullNotEquals() {
        assertFalse(checker.check(null, "foo"));
    }


    @Test
    public void testNumberEquals() {
        assertTrue(checker.check("7", "= 7"));
    }

    @Test
    public void testNullNotGreater() {
        assertFalse(checker.check(null, "> 2"));
    }

    @Test
    public void testGreaterThanTrue() {
        assertTrue(checker.check("5", "> 2"));
    }

    @Test
    public void testGreaterThanFalse() {
        assertFalse(checker.check("5", "> 12"));
    }

    @Test
    public void testLessThanTrue() {
        assertTrue(checker.check("5", "< 12"));
    }

    @Test
    public void testLessThanFalse() {
        assertFalse(checker.check("5", "< 2"));
    }

    @Test
    public void testNot() {
        assertTrue(checker.check("5", "not < 2"));
        assertFalse(checker.check("5", "not < 6"));
    }
    
    @Test
    public void testBetweenA() {
        assertTrue(checker.check("5", "between 2 and 6"));
    }

    @Test
    public void testBetweenB() {
        assertFalse(checker.check("5", "between 12 and 16"));
    }

    @Test
    public void testBetweenC() {
        assertFalse(checker.check(5L, "between 12 and 16"));
    }

    @Test
    public void testBetweenD() {
        assertTrue(checker.check(5L, "between 4 and 16"));
    }

    @Test
    public void testBetweenE() {
        assertTrue(checker.check(5L, "betWEEN 4 aND 16"));
    }

    @Test(expected=NumberFormatException.class)
    public void testNotAnInteger() {
        assertFalse(checker.check("foo", "between 12 and 16"));
    }

    @Test
    public void testContainsA() {
        assertTrue(checker.check("This is a NICE STRING ok?", "contains NICE STRING"));
    }

    @Test
    public void testContainsB() {
        assertFalse(checker.check("This is a NICE TOUCH ok?", "contains NICE STRING"));
    }

    @Test
    public void testContainsC() {
        assertTrue(checker.check("This is a NICE TOUCH ok?", "contains NICE"));
    }
    
    @Test
    public void testStartsWithA() {
        assertTrue(checker.check("This is a NICE TOUCH ok?", "STARTS_WITH This is"));
    }
    
    @Test
    public void testStartsWithB() {
        assertFalse(checker.check("This is a NICE TOUCH ok?", "STARTS_WITH is"));
    }    

    @Test
    public void testEndsWithA() {
        assertTrue(checker.check("This is a NICE TOUCH ok?", "ENDS_WITH TOUCH ok?"));
    }
    
    @Test
    public void testEndsWithB() {
        assertFalse(checker.check("This is a NICE TOUCH ok?", "ENDS_WITH is"));
    }

    @Test
    public void testMatches() {
        assertTrue(checker.check("testABCtest", "matches .*ABC.*"));
        assertFalse(checker.check("testABCtest", "matches .*XYZ.*"));
        assertTrue(checker.check("testABCtest", "not matches .*XYZ.*"));
        assertTrue(checker.check("2.1.0", "matches ^2\\.[1-9]\\.[0-9]+"));
    }

    @Test
    public void testOlderThan() {
        long timestampNow = new Date().getTime();
        assertFalse(checker.check(new Long(timestampNow), "OLDER_THAN 5 sec"));
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -55);

        assertTrue(checker.check(cal.getTime().getTime()+"", "OLDER_THAN 53 min"));
        assertFalse(checker.check(cal.getTime().getTime()+"", "OLDER_THAN 57 min"));
        assertTrue(checker.check(cal.getTime().getTime()+"", "NOT OLDER_THAN 57 min"));
        assertFalse(checker.check(cal.getTime().getTime()+"", "OLDER_THAN 1 hour"));
        assertFalse(checker.check(cal.getTime().getTime()+"", "OLDER_THAN 1 days"));
        assertTrue(checker.check(cal.getTime().getTime()+"", "OLDER_THAN 100 ms"));
    }
    
}
