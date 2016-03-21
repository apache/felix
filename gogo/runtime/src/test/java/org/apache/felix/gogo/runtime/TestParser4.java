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
package org.apache.felix.gogo.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/*
 * Test features of the new parser/tokenizer, many of which are not supported
 * by the original parser.
 */
public class TestParser4 extends AbstractParserTest
{
    public void testPipeErr() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("echoerr", this);
        c.addCommand("tac", this);

        assertEquals("hello\n", c.execute("echo hello| tac"));
        assertEquals("hello\n", c.execute("echoerr hello|& tac"));
    }

    @Test
    public void testRedir() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);

        Path path = Paths.get("target/tmp");
        Files.createDirectories(path);
        c.currentDir(path);

        Files.deleteIfExists(path.resolve("foo"));
        assertEquals("hello\n", c.execute("echo hello>foo | tac"));
        assertEquals("hello\n", new String(Files.readAllBytes(path.resolve("foo"))));

        Files.deleteIfExists(path.resolve("foo2"));
        assertEquals("hello\n", c.execute("echo hello>\\\nfoo2 | tac"));
    }

    @Test
    public void testRedirInput() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);
        c.addCommand("cat", this);

        Path path = Paths.get("target/tmp");
        Files.createDirectories(path);
        c.currentDir(path);

        Files.deleteIfExists(path.resolve("fooa"));
        Files.deleteIfExists(path.resolve("foob"));
        c.execute("echo a>fooa");
        c.execute("echo b>foob");
        assertEquals("a\nb\n", c.execute("cat <fooa <foob | tac"));
    }

    @Test
    public void testMultiInput() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);
        c.addCommand("cat", this);

        Path path = Paths.get("target/tmp");
        Files.createDirectories(path);
        c.currentDir(path);

        Files.deleteIfExists(path.resolve("fooa"));
        Files.deleteIfExists(path.resolve("foob"));
        c.execute("echo a>fooa");
        c.execute("echo b>foob");
        assertEquals("foo\na\nb\n", c.execute("echo foo | cat <fooa | cat<foob | tac"));
    }

    @Test
    public void testRedirectWithVar() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);
        c.addCommand("cat", this);

        Path path = Paths.get("target/tmp");
        Files.createDirectories(path);
        c.currentDir(path);

        c.execute("a=foo");
        c.execute("echo bar > $a");
        assertEquals("bar\n", c.execute("cat <$a | tac"));

        // Empty var
        try {
            c.execute("echo bar > $b");
            fail("Expected IOException");
        } catch (IOException e) {
        }

        try {
            c.execute("cat < $b");
            fail("Expected IOException");
        } catch (IOException e) {
        }

        // Array var
        c.execute("c = [ ar1 ar2 ]");
        c.execute("echo bar > $c");
        assertEquals("bar\nbar\n", c.execute("cat <$c | tac"));
    }

    @Test
    public void testHereString() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);
        c.addCommand("cat", this);

        c.execute("a=foo");
        assertEquals("foo\n", c.execute("cat <<< $a | tac"));

        c.execute("c = [ ar1 ar2 ]");
        assertEquals("ar1 ar2\n", c.execute("cat <<<$c | tac"));
    }

    @Test
    public void testHereDoc() throws Exception
    {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);
        c.addCommand("cat", this);

        assertEquals("bar\nbaz\n", c.execute("cat <<foo\nbar\nbaz\nfoo\n| tac"));
        assertEquals("bar\nbaz\n", c.execute("cat <<-foo\n\tbar\n\tbaz\n\tfoo\n| tac"));
    }

    public void echo(String msg)
    {
        System.out.println(msg);
    }

    public void echoerr(String msg)
    {
        System.err.println(msg);
    }

    public void cat() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
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
