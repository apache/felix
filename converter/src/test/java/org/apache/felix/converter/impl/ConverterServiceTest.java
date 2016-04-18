/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.converter.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConverterServiceTest {
    private ConverterService cs;

    @Before
    public void setUp() {
        cs = new ConverterService();
    }

    @After
    public void tearDown() {
        cs = null;
    }

    @Test
    public void testUUIDConversion() {
        ConverterService cs = new ConverterService();
        UUID uuid = UUID.randomUUID();
        String s = cs.convert(uuid).to(String.class);
        assertTrue("UUID should be something", s.length() > 0);
        UUID uuid2 = cs.convert(s).to(UUID.class);
        assertEquals(uuid, uuid2);
    }

    @Test
    public void testPatternConversion() {
        String p = "\\S*";
        Pattern pattern = cs.convert(p).to(Pattern.class);
        Matcher matcher = pattern.matcher("hi");
        assertTrue(matcher.matches());
        String p2 = cs.convert(pattern).to(String.class);
        assertEquals(p, p2);
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        String s = cs.convert(ldt).to(String.class);
        assertTrue(s.length() > 0);
        LocalDateTime ldt2 = cs.convert(s).to(LocalDateTime.class);
        assertEquals(ldt, ldt2);
    }

    @Test
    public void testLocalDate() {
        LocalDate ld = LocalDate.now();
        String s = cs.convert(ld).to(String.class);
        assertTrue(s.length() > 0);
        LocalDate ld2 = cs.convert(s).to(LocalDate.class);
        assertEquals(ld, ld2);
    }

    @Test
    public void testLocalTime() {
        LocalTime lt = LocalTime.now();
        String s = cs.convert(lt).to(String.class);
        assertTrue(s.length() > 0);
        LocalTime lt2 = cs.convert(s).to(LocalTime.class);
        assertEquals(lt, lt2);
    }

    @Test
    public void testOffsetDateTime() {
        OffsetDateTime ot = OffsetDateTime.now();
        String s = cs.convert(ot).to(String.class);
        assertTrue(s.length() > 0);
        OffsetDateTime ot2 = cs.convert(s).to(OffsetDateTime.class);
        assertEquals(ot, ot2);
    }

    @Test
    public void testOffsetTime() {
        OffsetTime ot = OffsetTime.now();
        String s = cs.convert(ot).to(String.class);
        assertTrue(s.length() > 0);
        OffsetTime ot2 = cs.convert(s).to(OffsetTime.class);
        assertEquals(ot, ot2);
    }

    @Test
    public void testZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        String s = cs.convert(zdt).to(String.class);
        assertTrue(s.length() > 0);
        ZonedDateTime zdt2 = cs.convert(s).to(ZonedDateTime.class);
        assertEquals(zdt, zdt2);
    }
}
