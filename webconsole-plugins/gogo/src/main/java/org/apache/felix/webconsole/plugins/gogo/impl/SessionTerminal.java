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
package org.apache.felix.webconsole.plugins.gogo.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import org.apache.felix.service.command.CommandProcessor;

public class SessionTerminal implements Runnable {

    private Terminal terminal;
    private Console console;
    private PipedOutputStream in;
    private PipedInputStream out;
    private boolean closed;

    @SuppressWarnings("serial")
    public SessionTerminal(final CommandProcessor commandProcessor, final String user) throws IOException {
        try {
            this.terminal = new Terminal(GogoPlugin.TERM_WIDTH, GogoPlugin.TERM_HEIGHT);
            terminal.write("\u001b\u005B20\u0068"); // set newline mode on

            in = new PipedOutputStream();
            out = new PipedInputStream();
            PrintStream pipedOut = new PrintStream(new PipedOutputStream(out), true);

            console = new Console(commandProcessor, new PipedInputStream(in), pipedOut, pipedOut, new Runnable() {
                public void run() {
                    SessionTerminal.this.terminal.write("done...");
                    close();
                }
            }, new HashMap<String, String>() {
                {
                    put("USER", user);
                    put("COLUMNS", Integer.toString(GogoPlugin.TERM_WIDTH));
                    put("LINES", Integer.toString(GogoPlugin.TERM_HEIGHT));
                }
            });
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
        new Thread(console).start();
        new Thread(this).start();
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        if (!closed) {
            this.closed = true;

            this.console.close();

            try {
                this.in.close();
            } catch (IOException e) {
            }
            try {
                this.out.close();
            } catch (IOException e) {
            }
        }
    }

    public String handle(String str, boolean forceDump) throws IOException {
        try {
            if (str != null && str.length() > 0) {
                String d = terminal.pipe(str);
                // echo unless backspace
                if (d.charAt(0) != '\b') {
                    terminal.write(d);
                }
                in.write(d.getBytes());
                in.flush();
            }
        } catch (IOException e) {
            // ignore
        }
        try {
            return terminal.dump(10, forceDump);
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.toString());
        }
    }

    public void run() {
        try {
            for (;;) {
                byte[] buf = new byte[8192];
                int l = out.read(buf);
                InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(buf, 0, l));
                StringBuilder sb = new StringBuilder();
                for (;;) {
                    int c = r.read();
                    if (c == -1) {
                        break;
                    }
                    sb.append((char) c);
                }
                if (sb.length() > 0) {
                    terminal.write(sb.toString());
                }
                String s = terminal.read();
                if (s != null && s.length() > 0) {
                    for (byte b : s.getBytes()) {
                        in.write(b);
                    }
                }
            }
        } catch (IOException e) {
            closed = true;
            e.printStackTrace();
        }
    }

}