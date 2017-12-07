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
package org.osgi.util.converter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    @Test
    public void testMangling() {
        assertMangle("", "");
        assertMangle("a", "a");
        assertMangle("ab", "ab");
        assertMangle("abc", "abc");
        assertMangle("a\u0008bc", "a\bbc");

        assertMangle("$_$", "-");
        assertMangle("$_", ".");
        assertMangle("_$", ".");
        assertMangle("x$_$", "x-");
        assertMangle("$_$x", "-x");
        assertMangle("abc$_$abc", "abc-abc");
        assertMangle("$$_$x", "$.x");
        assertMangle("$_$$", "-");
        assertMangle("$_$$$", "-$");
        assertMangle("$", "");
        assertMangle("$$", "$");
        assertMangle("_", ".");
        assertMangle("$_", ".");

        assertMangle("myProperty143", "myProperty143");
        assertMangle("$new", "new");
        assertMangle("n$ew", "new");
        assertMangle("new$", "new");
        assertMangle("my$$prop", "my$prop");
        assertMangle("dot_prop", "dot.prop");
        assertMangle("_secret", ".secret");
        assertMangle("another__prop", "another_prop");
        assertMangle("three___prop", "three_.prop");
        assertMangle("four_$__prop", "four._prop");
        assertMangle("five_$_prop", "five..prop");
    }

    private void assertMangle(String methodName, String key) {
        assertEquals(Util.unMangleName(methodName), key);
    }
}
