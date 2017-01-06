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
package org.apache.felix.configurator.impl.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

public class JSMinTest {

    @Test public void simpleTest() throws IOException {
        final String input = "// Some comment\n" +
                             "{\n"
                             + "  \"a\" : 1,\n"
                             + "  // another comment\n"
                             + "  /** And more\n"
                             + "   * comments\n"
                             + "   */\n"
                             + "  \"b\" : 2\n"
                             + "}\n";
        final StringWriter w = new StringWriter();
        final JSMin min = new JSMin(new StringReader(input), w);
        min.jsmin();
        assertEquals("\n{\"a\":1,\"b\":2}", w.toString());
    }
}
