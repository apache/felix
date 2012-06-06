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
package org.apache.felix.gogo.shell;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.felix.gogo.shell.History;
import org.junit.Before;
import org.junit.Test;

public class HistoryTest {

    private static final String[] COMMANDS = {
        "cd home", "ls", "more config", "more xconfig"
    };

    private History history;

    @Before
    public void setup() {
        this.history = new History();
        for (String cmd : COMMANDS) {
            this.history.append(cmd);
        }
    }

    @Test
    public void test_add_get_history() {
        final History his = new History();

        his.append("cmd1");
        Iterator<String> hi = his.getHistory();
        TestCase.assertEquals("cmd1", hi.next());
        TestCase.assertFalse(hi.hasNext());
    }

    @Test
    public void test_fill_history() {
        final History his = new History();

        // NOTE: Assumes a fixed history size of 100
        for (int i = 0; i < 100; i++) {
            his.append("cmd" + i);
        }

        Iterator<String> hi = his.getHistory();
        for (int i = 0; i < 100; i++) {
            TestCase.assertEquals("cmd" + i, hi.next());
        }
        TestCase.assertFalse(hi.hasNext());
    }

    @Test
    public void test_overflow_history() {
        final History his = new History();

        // NOTE: Assumes a fixed history size of 100
        for (int i = -20; i < 100; i++) {
            his.append("cmd" + i);
        }

        Iterator<String> hi = his.getHistory();
        for (int i = 0; i < 100; i++) {
            TestCase.assertEquals("cmd" + i, hi.next());
        }
        TestCase.assertFalse(hi.hasNext());
    }

    @Test
    public void test_get_recent() {
        TestCase.assertEquals(COMMANDS[3], history.evaluate("!!"));

        TestCase.assertEquals(COMMANDS[3], history.evaluate("!-1"));
        TestCase.assertEquals(COMMANDS[2], history.evaluate("!-2"));
        TestCase.assertEquals(COMMANDS[1], history.evaluate("!-3"));
        TestCase.assertEquals(COMMANDS[0], history.evaluate("!-4"));

        TestCase.assertEquals(COMMANDS[0], history.evaluate("!1"));
        TestCase.assertEquals(COMMANDS[1], history.evaluate("!2"));
        TestCase.assertEquals(COMMANDS[2], history.evaluate("!3"));
        TestCase.assertEquals(COMMANDS[3], history.evaluate("!4"));
    }

    @Test
    public void test_get_recent_wrong_index() {
        try {
            history.evaluate("!0");
            TestCase.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            history.evaluate("!5");
            TestCase.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            history.evaluate("!-5");
            TestCase.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_caret() {
        TestCase.assertEquals("ls xconfig", history.evaluate("^more^ls^"));
        TestCase.assertEquals("ls xconfig", history.evaluate("^more^ls"));
        TestCase.assertEquals("ls -l xconfig", history.evaluate("^more^ls -l"));
    }

    @Test
    public void test_caret_fail() {
        try {
            history.evaluate("^ls^cat^");
            TestCase.fail("Expected failure for impossible replacement");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_s_last() {
        TestCase.assertEquals("ls xconfig", history.evaluate("!!:s?more?ls?"));
        TestCase.assertEquals("ls xconfig", history.evaluate("!!:s/more/ls"));
        TestCase.assertEquals("ls -l xconfig", history.evaluate("!!:s^more^ls -l"));

        TestCase.assertEquals("mare xconfig", history.evaluate("!!:s?o?a?"));
    }

    @Test
    public void test_s_last_fail() {
        try {
            history.evaluate("!!:s/ls/cat/");
            TestCase.fail("Expected failure for impossible replacement");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_s_g_a_last() {
        TestCase.assertEquals("mire xcinfig", history.evaluate("!!:gs?o?i?"));
        TestCase.assertEquals("mire xcinfig", history.evaluate("!!:gs/o/i"));
        TestCase.assertEquals("mi-xre xci-xnfig", history.evaluate("!!:gs^o^i-x"));

        TestCase.assertEquals("mire xcinfig", history.evaluate("!!:as?o?i?"));
        TestCase.assertEquals("mire xcinfig", history.evaluate("!!:as/o/i"));
        TestCase.assertEquals("mi-xre xci-xnfig", history.evaluate("!!:as^o^i-x"));
    }

    @Test
    public void test_s_neg_2() {
        TestCase.assertEquals("ls config", history.evaluate("!-2:s?more?ls?"));
        TestCase.assertEquals("ls config", history.evaluate("!-2:s/more/ls"));
        TestCase.assertEquals("ls -l config", history.evaluate("!-2:s^more^ls -l"));

        TestCase.assertEquals("mare config", history.evaluate("!-2:s?o?a?"));
    }

    @Test
    public void test_s_neg_2_fail() {
        try {
            history.evaluate("!-2:s/ls/cat/");
            TestCase.fail("Expected failure for impossible replacement");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_s_3() {
        TestCase.assertEquals("ls config", history.evaluate("!-2:s?more?ls?"));
        TestCase.assertEquals("ls config", history.evaluate("!-2:s/more/ls"));
        TestCase.assertEquals("ls -l config", history.evaluate("!-2:s^more^ls -l"));

        TestCase.assertEquals("mare config", history.evaluate("!-2:s?o?a?"));
    }

    @Test
    public void test_s_3_fail() {
        try {
            history.evaluate("!-2:s/ls/cat/");
            TestCase.fail("Expected failure for impossible replacement");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void test_contains() {
        TestCase.assertEquals("ls", history.evaluate("!?s"));
        TestCase.assertEquals("ls", history.evaluate("!?s?"));

        try {
            history.evaluate("!?s:s/l/x/");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        TestCase.assertEquals("xs", history.evaluate("!?s?:s/l/x/"));
    }

    @Test
    public void test_startsWith() {
        TestCase.assertEquals("cd home", history.evaluate("!cd"));
        TestCase.assertEquals("cd home", history.evaluate("!cd "));

        try {
            history.evaluate("!cd:s/x/y/");
            TestCase.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        TestCase.assertEquals("ls home", history.evaluate("!cd:s/cd/ls/"));
    }
}
