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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

public class Console implements Runnable {

    public static final String IGNORE_INTERRUPTS = "karaf.ignoreInterrupts";

    protected CommandSession session;

    private BlockingQueue<Integer> queue;

    private boolean interrupt;

    private Thread pipe;

    volatile private boolean running;

    volatile private boolean eof;

    private Runnable closeCallback;

    private InputStream consoleInput;

    private InputStream in;

    private PrintStream out;

    private PrintStream err;

    private Thread thread;

    public Console(CommandProcessor processor, InputStream in, PrintStream out, PrintStream err,
            Runnable closeCallback, Map<String, String> sessionProps) throws Exception {
        this.in = in;
        this.out = out;
        this.err = err;
        this.queue = new ArrayBlockingQueue<Integer>(1024);
        this.consoleInput = new ConsoleInputStream();
        this.session = processor.createSession(this.consoleInput, this.out, this.err);
        this.closeCallback = closeCallback;

        if (sessionProps != null) {
            for (Entry<String, String> entry: sessionProps.entrySet()) {
                this.session.put(entry.getKey(), entry.getValue());
            }
        }
        pipe = new Thread(new Pipe());
        pipe.setName("gogo shell pipe thread");
        pipe.setDaemon(true);
    }

    public CommandSession getSession() {
        return session;
    }

    public void close() {
        running = false;
        pipe.interrupt();
    }

    public void run() {
        thread = Thread.currentThread();
        running = true;
        pipe.start();
        try
        {
            session.execute("gosh --login --noshutdown");
        }
        catch (Exception e)
        {
            e.printStackTrace(this.err);
        }
        finally
        {
            session.close();

            this.out.println("Good Bye!");
        }
        close();

        if (closeCallback != null) {
            closeCallback.run();
        }
    }

    protected boolean getBoolean(String name) {
        Object s = session.get(name);
        if (s == null) {
            s = System.getProperty(name);
        }
        if (s == null) {
            return false;
        }
        if (s instanceof Boolean) {
            return (Boolean) s;
        }
        return Boolean.parseBoolean(s.toString());
    }

    private void checkInterrupt() throws IOException {
        if (Thread.interrupted() || interrupt) {
            interrupt = false;
            throw new InterruptedIOException("Keyboard interruption");
        }
    }

    private void interrupt() {
        interrupt = true;
        thread.interrupt();
    }

    private class ConsoleInputStream extends InputStream {
        private int read(boolean wait) throws IOException {
            if (!running) {
                return -1;
            }
            checkInterrupt();
            if (eof && queue.isEmpty()) {
                return -1;
            }
            Integer i;
            if (wait) {
                try {
                    i = queue.take();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
                checkInterrupt();
            } else {
                i = queue.poll();
            }
            if (i == null) {
                return -1;
            }
            return i;
        }

        @Override
        public int read() throws IOException {
            return read(true);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int nb = 1;
            int i = read(true);
            if (i < 0) {
                return -1;
            }
            b[off++] = (byte) i;
            while (nb < len) {
                i = read(false);
                if (i < 0) {
                    return nb;
                }
                b[off++] = (byte) i;
                nb++;
            }
            return nb;
        }

        @Override
        public int available() throws IOException {
            return queue.size();
        }
    }

    private class Pipe implements Runnable {
        public void run() {
            try {
                while (running) {
                    try {
                        int c = in.read();
                        if (c == -1) {
                            return;
                        } else if (c == 4 && !getBoolean(IGNORE_INTERRUPTS)) {
                            err.println("^D");
                        } else if (c == 3 && !getBoolean(IGNORE_INTERRUPTS)) {
                            err.println("^C");
                            interrupt();
                        }
                        queue.put(c);
                    } catch (Throwable t) {
                        return;
                    }
                }
            } finally {
                eof = true;
                try {
                    queue.put(-1);
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
