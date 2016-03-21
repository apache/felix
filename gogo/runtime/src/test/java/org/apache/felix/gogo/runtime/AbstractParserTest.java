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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractParserTest {

    private ThreadIOImpl threadIO;
    private InputStream sin;
    private PrintStream sout;
    private PrintStream serr;

    @Before
    public void setUp() throws Exception {
        sin = new NoCloseInputStream(System.in);
        sout = new NoClosePrintStream(System.out);
        serr = new NoClosePrintStream(System.err);
        threadIO = new ThreadIOImpl();
        threadIO.start();
    }

    @After
    public void tearDown() throws Exception {
        threadIO.stop();
    }

    public class Context extends org.apache.felix.gogo.runtime.Context {
        public Context() {
            super(AbstractParserTest.this.threadIO, sin, sout, serr);
        }
    }

    private static class NoCloseInputStream extends FilterInputStream {
        public NoCloseInputStream(InputStream in) {
            super(in);
        }
        @Override
        public void close() throws IOException {
        }
    }

    private static class NoClosePrintStream extends PrintStream {
        public NoClosePrintStream(OutputStream out) {
            super(out);
        }
        @Override
        public void close() {
        }
    }

}
