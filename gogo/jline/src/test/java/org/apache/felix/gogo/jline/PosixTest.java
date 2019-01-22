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
package org.apache.felix.gogo.jline;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.junit.Test;

public class PosixTest extends AbstractParserTest {

    @Test
    public void testGrepWithColoredInput() throws Exception {
        Context context = new Context();
        context.addCommand("echo", new Posix(context));
        context.addCommand("grep", new Posix(context));
        context.addCommand("tac", this);

        Object res = context.execute("echo \"  \\u001b[1mbold\\u001b[0m  la\" | grep la | tac");
        assertEquals("  \u001b[1mbold\u001b[0m  la", res);
    }

    @Test
    public void testLsDotDot() throws Exception {
        Context context = new Context();
        context.addCommand("ls", new Posix(context));
        context.addCommand("tac", this);

        String current = (String) context.execute("ls -1 --color=never . | tac");
        assertTrue(current.indexOf("..") >= 0);
        assertTrue(current.indexOf(".") >= 0);

        String parent = (String) context.execute("ls -1 --color=never .. | tac");
        assertTrue(parent.indexOf("..") >= 0);
        assertTrue(parent.indexOf(".") >= 0);

        assertNotEquals(current, parent);
    }

    @Test
    public void testWcLines() throws Exception {
        Context context = new Context();
        context.addCommand("echo", new Posix(context));
        context.addCommand("wc", new Posix(context));
        context.addCommand("tac", this);

        Object res = context.execute("echo \"test\" | wc -l | tac");
        assertEquals("1", res);
    }

    @Test
    public void testWcBytes() throws Exception {
        Context context = new Context();
        context.addCommand("echo", new Posix(context));
        context.addCommand("wc", new Posix(context));
        context.addCommand("tac", this);

        Object res = context.execute("echo \"test\" | wc -c | tac");
        assertEquals("5", res);
    }

    @Test
    public void testWcLinesBytes() throws Exception {
        Context context = new Context();
        context.addCommand("echo", new Posix(context));
        context.addCommand("wc", new Posix(context));
        context.addCommand("tac", this);

        Object res = context.execute("echo \"test\" | wc -l -c | tac");
        assertEquals("       1       5", res);
    }

    @Test
    public void testWcLinesBytesChar() throws Exception {
        Context context = new Context();
        context.addCommand("echo", new Posix(context));
        context.addCommand("wc", new Posix(context));
        context.addCommand("tac", this);

        Object res = context.execute("echo \"test\" | wc -l -c -m | tac");
        assertEquals("       1       5       5", res);
    }

    public String tac() throws IOException {
        StringWriter sw = new StringWriter();
        Reader rdr = new InputStreamReader(System.in);
        char[] buf = new char[1024];
        int len;
        while ((len = rdr.read(buf)) >= 0) {
            sw.write(buf, 0, len);
        }
        return sw.toString();
    }
}
