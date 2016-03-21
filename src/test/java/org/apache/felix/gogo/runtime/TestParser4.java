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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map.Entry;

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

        assertEquals("hello", c.execute("echo | tac"));
        assertEquals("hello", c.execute("echoerr |& tac"));
    }

    public void testRedir() throws Exception {
        Context c = new Context();
        c.addCommand("echo", this);
        c.addCommand("tac", this);

        Path path = Paths.get("target/tmp");
        Files.createDirectories(path);
        c.currentDir(path);

        Files.deleteIfExists(path.resolve("foo"));
        assertEquals("hello", c.execute("echo >foo | tac"));
        assertEquals("hello\n", new String(Files.readAllBytes(path.resolve("foo"))));
    }

    public void echo()
    {
        System.out.println("hello");
    }

    public void echoerr()
    {
        System.err.println("hello");
    }

    public String tac() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        boolean first = true;
        String s;
        while ((s = rdr.readLine()) != null) {
            if (!first) {
                sw.write(' ');
            }
            first = false;
            sw.write(s);
        }
        return sw.toString();
    }

}
