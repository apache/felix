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

package org.apache.felix.ipojo.manipulator.util;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StreamsTestCase extends TestCase {
    public void testSimpleClose() throws Exception {
        Closeable closeable = mock(Closeable.class);
        Streams.close(closeable);
        verify(closeable).close();
    }

    public void testCloseWithNullParameter() throws Exception {
        Closeable closeable = mock(Closeable.class);
        Streams.close(closeable, null);
        verify(closeable).close();
    }

    public void testTransfer() throws Exception {
        String toBeRead = "Tadam";
        ByteArrayInputStream bais = new ByteArrayInputStream(toBeRead.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Streams.transfer(bais, baos);
        Assert.assertEquals(toBeRead, new String(baos.toByteArray()));
    }

    public void testReadBytes() throws Exception {
        String toBeRead = "Tadam";
        ByteArrayInputStream bais = new ByteArrayInputStream(toBeRead.getBytes());
        byte[] bytes = Streams.readBytes(bais);
        Assert.assertEquals(toBeRead, new String(bytes));
    }
}
