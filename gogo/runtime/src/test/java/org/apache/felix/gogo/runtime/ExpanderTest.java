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

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExpanderTest {

    @Test
    public void testOctalAndHex() throws Exception {
        Evaluate evaluate = new Evaluate() {
            @Override
            public Object eval(Token t) throws Exception {
                return null;
            }
            @Override
            public Object get(String key) {
                return null;
            }
            @Override
            public Object put(String key, Object value) {
                return null;
            }
            @Override
            public Object expr(Token t) {
                return null;
            }
            @Override
            public Path currentDir() {
                return null;
            }
        };
        assertEquals("\033\033", Expander.expand("$'\\033\\u001B'", evaluate));
    }

    @Test
    public void testSortingFlags() throws Exception {
        Evaluate evaluate = new Evaluate() {
            @Override
            public Object eval(Token t) throws Exception {
                return null;
            }
            @Override
            public Object get(String key) {
                switch (key) {
                    case "a":
                        return Arrays.asList(5, 3, 4, 2, 1, 3);
                    case "b":
                        return Arrays.asList(5, 3, 4, 2, 1, 31);
                    case "c":
                        return Arrays.asList("foo1", "foo02", "foo2", "foo3", "foo20", "foo23");
                    case "d":
                        return Arrays.asList("foo1", "foo02", "Foo2", "Foo3", "foo20", "foo23");
                }
                return null;
            }
            @Override
            public Object put(String key, Object value) {
                return null;
            }
            @Override
            public Object expr(Token t) {
                return null;
            }
            @Override
            public Path currentDir() {
                return null;
            }
        };

        assertEquals("5 3 4 2 1 3", Expander.expand("${(j: :)a}", evaluate));
        assertEquals("5 3 4 2 1", Expander.expand("${(j: :)${(u)a}}", evaluate));
        assertEquals("3 1 2 4 3 5", Expander.expand("${(j: :)${(Oa)a}}", evaluate));
        assertEquals("1 2 3 31 4 5", Expander.expand("${(j: :)${(i)b}}", evaluate));
        assertEquals("1 2 3 4 5 31", Expander.expand("${(j: :)${(n)b}}", evaluate));
        assertEquals("foo1 foo02 foo2 foo3 foo20 foo23", Expander.expand("${(j: :)${(n)c}}", evaluate));
        assertEquals("foo23 foo20 foo3 foo2 foo02 foo1", Expander.expand("${(j: :)${(On)c}}", evaluate));
        assertEquals("Foo2 Foo3 foo1 foo02 foo20 foo23", Expander.expand("${(j: :)${(n)d}}", evaluate));
        assertEquals("foo1 foo02 Foo2 Foo3 foo20 foo23", Expander.expand("${(j: :)${(ni)d}}", evaluate));
    }

    @Test
    public void testGenerateFiles() throws IOException {
        final Path testdir = Paths.get(".").toAbsolutePath().resolve("target/testdir").normalize();
        Evaluate evaluate = new Evaluate() {
            @Override
            public Object eval(Token t) throws Exception {
                return null;
            }
            @Override
            public Object get(String key) {
                if ("HOME".equals(key)) {
                    return testdir.resolve("Users/gogo");
                }
                return null;
            }
            @Override
            public Object put(String key, Object value) {
                return null;
            }
            @Override
            public Object expr(Token t) {
                return null;
            }
            @Override
            public Path currentDir() {
                return testdir.resolve("Users/gogo/karaf/home");
            }
        };
        deleteRecursive(testdir);
        Files.createDirectories(testdir);
        Files.createDirectories(evaluate.currentDir());
        Path home = Paths.get(evaluate.get("HOME").toString());
        Files.createDirectories(home);
        Files.createFile(home.resolve("test1.txt"));
        Files.createDirectories(home.resolve("child"));
        Files.createFile(home.resolve("child/test2.txt"));

        Expander expander = new Expander("", evaluate, false, false, false, false, false);
        List<? extends CharSequence> files = expander.generateFileNames("~/*.[tx][v-z][!a]");
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("test1.txt", home.relativize(Paths.get(files.get(0).toString())).toString());
    }

    private static void deleteRecursive(Path file) throws IOException {
        if (file != null) {
            if (Files.isDirectory(file)) {
                for (Path child : Files.newDirectoryStream(file)) {
                    deleteRecursive(child);
                }
            }
            Files.deleteIfExists(file);
        }
    }
}
