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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ClosureTest {

    @Test
    public void testParentSessionClosure() throws Exception {
        CommandProcessorImpl processor = new CommandProcessorImpl(null);
        ByteArrayInputStream bais = new ByteArrayInputStream("".getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandSessionImpl parent = processor.createSession(bais, baos, baos);
        parent.execute("var = a");
        parent.execute("cmd = { $var }");
        CommandSessionImpl child = processor.createSession(parent);
        child.execute("var = b");
        assertEquals("a", ((Closure) parent.get("cmd")).execute(parent, Collections.emptyList()).toString());
        assertEquals("b", ((Closure) parent.get("cmd")).execute(child, Collections.emptyList()).toString());
    }
}
