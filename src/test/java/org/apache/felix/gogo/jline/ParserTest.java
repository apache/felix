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

import org.jline.reader.CompletingParsedLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParserTest {

    @Test
    public void testEscapedWord() {
        Parser parser = new Parser();
        CompletingParsedLine line = (CompletingParsedLine) parser.parse("foo second\\ param \"quoted param\"", 15);
        assertNotNull(line);
        assertNotNull(line.words());
        assertEquals("foo second\\ param \"quoted param\"", line.line());
        assertEquals(15, line.cursor());
        assertEquals(3, line.words().size());
        assertEquals("second param", line.word());
        assertEquals(10, line.wordCursor());
        assertEquals(11, line.rawWordCursor());
        assertEquals(13, line.rawWordLength());
    }

    @Test
    public void testQuotedWord() {
        Parser parser = new Parser();
        CompletingParsedLine line = (CompletingParsedLine) parser.parse("foo second\\ param \"quoted param\"", 20);
        assertNotNull(line);
        assertNotNull(line.words());
        assertEquals("foo second\\ param \"quoted param\"", line.line());
        assertEquals(20, line.cursor());
        assertEquals(3, line.words().size());
        assertEquals("quoted param", line.word());
        assertEquals(1, line.wordCursor());
        assertEquals(2, line.rawWordCursor());
        assertEquals(14, line.rawWordLength());
    }

}
