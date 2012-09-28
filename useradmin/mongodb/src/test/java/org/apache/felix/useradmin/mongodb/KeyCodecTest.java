/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.mongodb;

import junit.framework.TestCase;

/**
 * Test cases for {@link KeyCodec}.
 */
public class KeyCodecTest extends TestCase {

    /**
     * Tests that we can decode a string with an encoded dollar.
     */
    public void testDecodeDollarOk() {
        String input = "%24hello %24world";
        assertEquals("$hello $world", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with encoded dot and dollars.
     */
    public void testDecodeDotAndDollarsOk() {
        String input = "%24hello %24world%2E";
        assertEquals("$hello $world.", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an encoded dot.
     */
    public void testDecodeDotOk() {
        String input = "hello world%2E";
        assertEquals("hello world.", KeyCodec.decode(input));
    }

    /**
     * Tests that encoding a string with a dot works.
     */
    public void testDecodeEncodeOk() {
        String key = "%25hello world.";
        assertEquals(key, KeyCodec.decode(KeyCodec.encode(key)));
    }

    /**
     * Tests that we can decode a string with an incorrect entity at the end.
     */
    public void testDecodeIncorrectEntityAtEndOk() {
        String input = "hello world%1";
        assertEquals("hello world%1", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an incorrect entity at the end.
     */
    public void testDecodeIncorrectEntityOk() {
        String input = "hello%1world";
        assertEquals("hello%1world", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an encoded null.
     */
    public void testDecodeNullEntityOk() {
        String input = "%00hello%00world";
        assertEquals("\0hello\0world", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a null value.
     */
    public void testDecodeNullOk() {
        assertNull(KeyCodec.decode(null));
    }

    /**
     * Tests that we can decode a string with a percent sign at the end.
     */
    public void testDecodePercentAtEndOk() {
        String input = "hello world%";
        assertEquals("hello world%", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an escaped percent.
     */
    public void testDecodePercentOk() {
        String input = "%%1%%%2E";
        assertEquals("%1%.", KeyCodec.decode(input));
        
        input = "%%Hello %%World%2E";
        assertEquals("%Hello %World.", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an encoded underscore.
     */
    public void testDecodeUnderscoreOk() {
        String input = "%5Fhello%5Fworld";
        assertEquals("_hello_world", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an unknown entity at the end.
     */
    public void testDecodeUnknownEntityAtEndOk() {
        String input = "hello world%10";
        assertEquals("hello world%10", KeyCodec.decode(input));
    }

    /**
     * Tests that we can decode a string with an unknown entity.
     */
    public void testDecodeUnknownEntityOk() {
        String input = "%25hello %25world%2F";
        assertEquals("%25hello %25world%2F", KeyCodec.decode(input));
    }

    /**
     * Tests that encoding a string with dollars works.
     */
    public void testEncodeDollarOk() {
        String key = "$hello $world";
        assertEquals("%24hello %24world", KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string with dollars and a dot works.
     */
    public void testEncodeDotAndDollarOk() {
        String key = "$hello $world.";
        assertEquals("%24hello %24world%2E", KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string with a dot works.
     */
    public void testEncodeDotOk() {
        String key = "hello world.";
        assertEquals("hello world%2E", KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string with null-characters works.
     */
    public void testEncodeNullEntityOk() {
        String key = "\0hello\0world";
        assertEquals("%00hello%00world", KeyCodec.encode(key));
    }

    /**
     * Tests that we can encode a null value.
     */
    public void testEncodeNullInputOk() {
        assertNull(KeyCodec.encode(null));
    }

    /**
     * Tests that encoding a string with percents works.
     */
    public void testEncodePercentOk() {
        String key = "%hello %world.";
        assertEquals("%%hello %%world%2E", KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string without dots or dollars works.
     */
    public void testEncodePlainStringOk() {
        String key = "hello world!";
        assertEquals(key, KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string with underscores works.
     */
    public void testEncodeUnderscoreOk() {
        String key = "_hello_world";
        assertEquals("%5Fhello%5Fworld", KeyCodec.encode(key));
    }

    /**
     * Tests that encoding a string with a dot works.
     */
    public void testEncodeUnknownEntitiesOk() {
        String key = "%25hello world.";
        assertEquals("%%25hello world%2E", KeyCodec.encode(key));
    }
}
