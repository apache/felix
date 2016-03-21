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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.junit.Assert;
import org.junit.Test;

public class ReflectiveTest {

    @Test
    public void testArrayInvocation() throws Exception {
        assertEquals(new Object[] { 1, "ab" }, invoke("test1", Arrays.asList(1, "ab")));
        assertEquals(new String[] { "1", "ab" }, invoke("test2", Arrays.asList(1, "ab")));

        assertEquals(new Object[] { Arrays.asList(1, 2), "ab" }, invoke("test1", Arrays.asList(Arrays.asList(1, 2), "ab")));
        assertEquals(new Object[] { new Object[] { 1, 2 }, "ab" }, invoke("test1", Arrays.asList(new Object[] { 1, 2 }, "ab")));
    }

    static class Target {
        public Object test1(CommandSession session, Object[] argv) {
            return argv;
        }

        public Object test2(CommandSession session, String[] argv) {
            return argv;
        }

        public Object test3(CommandSession session, Collection<Object> argv) {
            return argv;
        }

        public Object test4(CommandSession session, List<String> argv) {
            return argv;
        }
    }

    static Object invoke(String method, List<Object> args) throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        OutputStream out = new ByteArrayOutputStream();
        CommandProcessorImpl processor = new CommandProcessorImpl(null);
        return Reflective.invoke(new CommandSessionImpl(processor, in, out, out), new Target(), method, args);
    }

    static void assertEquals(Object o1, Object o2) {
        assertEquals(null, o1, o2);
    }

    static void assertEquals(String msg, Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            Assert.assertEquals(msg, o1, o2);
        }
        else if (o1.getClass().isArray() && o2.getClass().isArray()) {
            Assert.assertArrayEquals(msg, (Object[]) o1, (Object[]) o2);
        }
        else {
            Assert.assertEquals(msg, o1, o2);
        }
    }

}
